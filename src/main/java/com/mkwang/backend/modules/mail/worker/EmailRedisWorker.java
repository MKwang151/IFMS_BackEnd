package com.mkwang.backend.modules.mail.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.modules.mail.dto.EmailPayload;
import com.mkwang.backend.modules.mail.producer.EmailRedisProducer;
import com.mkwang.backend.modules.mail.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EmailRedisWorker — Consumer that polls the Redis email queue and dispatches
 * emails asynchronously via MailService.
 * <p>
 * Architecture:
 * <pre>
 *   ┌─────────────────┐
 *   │  Redis List      │   ← LPUSH by EmailRedisProducer
 *   │  (email queue)   │
 *   └────────┬────────┘
 *            │ @Scheduled(fixedDelay=2s) — RPOP × batchSize
 *            ▼
 *   ┌─────────────────┐
 *   │  Worker Thread   │   (ifms-scheduler-*)
 *   │  pollAndSend()   │   — pops messages, dispatches to MailService
 *   └────────┬────────┘
 *            │ mailService.sendMail(payload) — @Async("mailExecutor")
 *            ▼
 *   ┌─────────────────┐
 *   │  Mail Pool       │   (ifms-mail-1..5) — parallel SMTP sends
 *   └─────────────────┘
 * </pre>
 * <p>
 * The Worker thread does NOT wait for SMTP to complete — it dispatches all
 * batch items to the async mail pool, then collects CompletableFutures to
 * handle retry/DLQ for failed ones.
 * <p>
 * Error handling: message is NEVER lost.
 * <ul>
 *   <li>Send success → message consumed (already popped).</li>
 *   <li>Send failure + retryCount &lt; maxRetry → re-queue with incremented retryCount.</li>
 *   <li>Send failure + retryCount &gt;= maxRetry → move to Dead Letter Queue.</li>
 *   <li>Deserialization failure → move to DLQ immediately.</li>
 * </ul>
 *
 * @see com.mkwang.backend.modules.mail.producer.EmailRedisProducer
 * @see com.mkwang.backend.modules.mail.service.MailServiceImpl
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRedisWorker {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MailService mailService;
    private final EmailRedisProducer emailRedisProducer;

    @Value("${application.mail.queue-name}")
    private String queueName;

    @Value("${application.mail.dead-letter-queue}")
    private String deadLetterQueue;

    @Value("${application.mail.max-retry}")
    private int maxRetry;

    @Value("${application.mail.batch-size}")
    private int batchSize;

    /**
     * Scheduled poll — pops up to {@code batchSize} messages and dispatches them
     * to the async mail thread pool in parallel.
     * <p>
     * Uses fixedDelayString (not fixedRate) — next cycle starts AFTER the current
     * one finishes, preventing overlap.
     * <p>
     * Flow per cycle:
     * <ol>
     *   <li>Pop up to {@code batchSize} messages from Redis (RPOP, non-blocking).</li>
     *   <li>Deserialize each JSON → EmailPayload.</li>
     *   <li>Dispatch each to {@code mailService.sendMail()} → returns CompletableFuture.</li>
     *   <li>Wait for all futures to complete (allOf).</li>
     *   <li>For each failed send → retry or DLQ.</li>
     * </ol>
     */
    @Scheduled(fixedDelayString = "${application.mail.worker-poll-ms}")
    public void pollAndSend() {
        // ── Step 1: Pop batch from Redis ──
        List<PendingEmail> batch = popBatch();
        if (batch.isEmpty()) {
            return;
        }

        // ── Step 2: Dispatch all to async mail pool ──
        List<CompletableFuture<Void>> futures = new ArrayList<>(batch.size());

        for (PendingEmail item : batch) {
            CompletableFuture<Boolean> sendFuture = mailService.sendMail(item.payload);

            // Chain error handling — runs on the mail thread when the future completes
            CompletableFuture<Void> handled = sendFuture.thenAccept(success -> {
                if (!success) {
                    handleSendFailure(item.payload, item.originalJson,
                            new RuntimeException("MailService returned false"));
                }
            }).exceptionally(ex -> {
                handleSendFailure(item.payload, item.originalJson, (Exception) ex.getCause());
                return null;
            });

            futures.add(handled);
        }

        // ── Step 3: Wait for entire batch to finish before next poll cycle ──
        // This ensures we don't over-dispatch when the mail pool is busy.
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.debug("Email worker processed {} message(s) this cycle", batch.size());
    }

    /**
     * Pop up to {@code batchSize} messages from the Redis queue.
     * Deserialize each. Move corrupted messages to DLQ.
     *
     * @return list of valid PendingEmail items ready for dispatch
     */
    private List<PendingEmail> popBatch() {
        List<PendingEmail> batch = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            String json = redisTemplate.opsForList().rightPop(queueName);
            if (json == null) {
                break; // Queue empty
            }

            try {
                EmailPayload payload = objectMapper.readValue(json, EmailPayload.class);
                batch.add(new PendingEmail(payload, json));
            } catch (JsonProcessingException e) {
                log.error("Corrupted email message in queue, moving to DLQ: {}", e.getMessage());
                emailRedisProducer.sendToDeadLetterQueue(json, deadLetterQueue);
            }
        }

        return batch;
    }

    /**
     * Handle email send failure — retry or move to Dead Letter Queue.
     * <p>
     * Thread-safe: this method may be called from different mail pool threads concurrently,
     * but each operates on its own payload/json — no shared mutable state.
     */
    private void handleSendFailure(EmailPayload payload, String originalJson, Exception error) {
        int currentRetry = payload.getRetryCount() + 1;
        payload.setRetryCount(currentRetry);

        if (currentRetry < maxRetry) {
            log.warn("Email send failed (attempt {}/{}), re-queuing: to={}, subject={}, error={}",
                    currentRetry, maxRetry,
                    payload.getTo(), payload.getSubject(), error.getMessage());
            try {
                String updatedJson = objectMapper.writeValueAsString(payload);
                emailRedisProducer.requeue(updatedJson);
            } catch (JsonProcessingException serializeError) {
                log.error("Failed to re-serialize payload for retry, using original JSON", serializeError);
                emailRedisProducer.requeue(originalJson);
            }
        } else {
            log.error("Email failed after {} retries → DLQ: to={}, subject={}, error={}",
                    maxRetry, payload.getTo(), payload.getSubject(), error.getMessage());
            try {
                String dlqJson = objectMapper.writeValueAsString(payload);
                emailRedisProducer.sendToDeadLetterQueue(dlqJson, deadLetterQueue);
            } catch (JsonProcessingException serializeError) {
                emailRedisProducer.sendToDeadLetterQueue(originalJson, deadLetterQueue);
            }
        }
    }

    /**
     * Internal record to hold a deserialized payload together with its original JSON.
     * The original JSON is kept for fallback re-queue if re-serialization fails.
     */
    private record PendingEmail(EmailPayload payload, String originalJson) {
    }
}

