package com.mkwang.backend.modules.audit.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.modules.audit.entity.AuditLog;
import com.mkwang.backend.modules.audit.event.AuditEvent;
import com.mkwang.backend.modules.audit.repository.AuditLogRepository;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AuditConsumer — nhận AuditEvent từ RabbitMQ và persist vào DB.
 * <p>
 * Flow:
 * <ol>
 *   <li>AuditEventListener (Spring @TransactionalEventListener AFTER_COMMIT)
 *       gửi AuditEvent sau khi business transaction commit</li>
 *   <li>AuditConsumer nhận message, save vào audit_logs trong transaction riêng (REQUIRES_NEW)</li>
 *   <li>Nếu save thất bại → default retry policy (3 lần, 3s→9s) → NACK → auditDLQ</li>
 * </ol>
 * Audit failure KHÔNG được propagate lên business logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditConsumer {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ── Main listener ────────────────────────────────────────────

    @RabbitListener(queues = "${spring.rabbitmq.audit.queue}")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void consumeAuditEvent(AuditEvent event) {
        try {
            User actor = resolveActor(event.getActorId());

            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(event.getAction())
                    .entityName(event.getEntityName())
                    .entityId(event.getEntityId())
                    .oldValues(serializeToJson(event.getOldValues()))
                    .newValues(serializeToJson(event.getNewValues()))
                    .build();

            auditLogRepository.save(auditLog);

            log.debug("[AuditConsumer] Saved: action={}, entity={}#{}, actor={}",
                    event.getAction(), event.getEntityName(), event.getEntityId(),
                    event.getActorId() != null ? event.getActorId() : "SYSTEM");

        } catch (Exception ex) {
            // Re-throw để trigger Spring AMQP retry → sau 3 lần sẽ NACK → DLQ
            log.error("[AuditConsumer] Failed to save audit log: action={}, entity={}#{} — {}",
                    event.getAction(), event.getEntityName(), event.getEntityId(), ex.getMessage(), ex);
            throw ex;
        }
    }

    // ── DLQ listener ─────────────────────────────────────────────

    @RabbitListener(queues = "${spring.rabbitmq.audit.dlq}")
    public void consumeAuditDLQ(Message rawMessage) {
        log.warn("[AuditConsumer][DLQ] Audit event FAILED after all retries. " +
                        "messageId={}, body={}",
                rawMessage.getMessageProperties().getMessageId(),
                new String(rawMessage.getBody()));
        // TODO: alert ops team hoặc persist vào failsafe table
    }

    // ── Helpers ──────────────────────────────────────────────────

    private User resolveActor(Long actorId) {
        if (actorId == null) return null;
        return userRepository.findById(actorId).orElse(null);
    }

    private String serializeToJson(Object values) {
        if (values == null) return null;
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            log.warn("[AuditConsumer] Failed to serialize audit values to JSON: {}", ex.getMessage());
            return null;
        }
    }
}
