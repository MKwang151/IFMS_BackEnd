package com.mkwang.backend.modules.mail.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.modules.mail.dto.EmailPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * EmailRedisProducer — Pushes email jobs into a Redis List queue.
 * <p>
 * Producer-Consumer Pattern:
 * <pre>
 *   [Business Service] → Producer.sendToQueue(payload)
 *                              ↓ LPUSH (JSON)
 *                        ┌─────────────┐
 *                        │ Redis List   │  ← FIFO Queue
 *                        │ (LPUSH/RPOP) │
 *                        └─────────────┘
 *                              ↓ RPOP (JSON)
 *                        Worker.poll() → MailService.sendHtmlMail()
 * </pre>
 * <p>
 * WHY LPUSH + RPOP (not RPUSH + LPOP)?
 * Both achieve FIFO. Convention: LPUSH (left/head) + RPOP (right/tail) = queue.
 * The Worker uses RPOP (non-blocking) in a @Scheduled loop.
 * <p>
 * Usage example from other modules:
 * <pre>{@code
 * @Autowired EmailRedisProducer emailProducer;
 *
 * emailProducer.sendToQueue(EmailPayload.builder()
 *     .to("employee@company.vn")
 *     .subject("Đơn tạm ứng đã được duyệt")
 *     .templateName("request-approved")
 *     .variables(Map.of("requestCode", "REQ-IT-0326-001", "amount", "5,000,000 VND"))
 *     .build());
 * }</pre>
 *
 * @see com.mkwang.backend.modules.mail.worker.EmailRedisWorker
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRedisProducer {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${application.mail.queue-name}")
    private String queueName;

    /**
     * Serialize the EmailPayload to JSON and push to the Redis queue (LPUSH).
     * <p>
     * The message will be picked up by {@code EmailRedisWorker} on its next poll cycle.
     * <p>
     * Thread-safe: Redis LPUSH is atomic. Multiple threads/services can call this concurrently.
     *
     * @param payload the email job to enqueue
     * @throws IllegalArgumentException if payload is null or missing required fields
     */
    public void sendToQueue(EmailPayload payload) {
        validatePayload(payload);

        try {
            String json = objectMapper.writeValueAsString(payload);
            redisTemplate.opsForList().leftPush(queueName, json);
            log.debug("Email job enqueued: to={}, subject={}, template={}",
                    payload.getTo(), payload.getSubject(), payload.getTemplateName());
        } catch (JsonProcessingException e) {
            // JSON serialization should never fail for a well-formed DTO.
            // If it does, it's a programming error — log and throw immediately.
            log.error("Failed to serialize EmailPayload to JSON: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot serialize EmailPayload", e);
        }
    }

    /**
     * Convenience method — push a raw JSON string back to the queue.
     * Used by the Worker for retry (re-enqueue failed messages).
     *
     * @param json the raw JSON string of an EmailPayload
     */
    public void requeue(String json) {
        redisTemplate.opsForList().leftPush(queueName, json);
        log.debug("Email job re-queued for retry");
    }

    /**
     * Push a failed message to the Dead Letter Queue.
     * Messages here have exceeded the max retry limit and need manual intervention.
     *
     * @param json the raw JSON string of the failed EmailPayload
     * @param deadLetterQueue the DLQ Redis key name
     */
    public void sendToDeadLetterQueue(String json, String deadLetterQueue) {
        redisTemplate.opsForList().leftPush(deadLetterQueue, json);
        log.warn("Email job moved to Dead Letter Queue: {}", deadLetterQueue);
    }

    /**
     * Basic validation — fail-fast before touching Redis.
     */
    private void validatePayload(EmailPayload payload) {
        if (payload == null) {
            throw new IllegalArgumentException("EmailPayload must not be null");
        }
        if (payload.getTo() == null || payload.getTo().isBlank()) {
            throw new IllegalArgumentException("EmailPayload.to (recipient) must not be blank");
        }
        if (payload.getSubject() == null || payload.getSubject().isBlank()) {
            throw new IllegalArgumentException("EmailPayload.subject must not be blank");
        }
    }
}

