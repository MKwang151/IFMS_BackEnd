package com.mkwang.backend.modules.audit.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.modules.audit.entity.AuditLog;
import com.mkwang.backend.modules.audit.event.AuditEvent;
import com.mkwang.backend.modules.audit.repository.AuditLogRepository;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async listener that persists AuditLog records after the calling transaction commits.
 * <p>
 * Architecture:
 * <pre>
 *   [Any Service] → AuditEventPublisher.publish(event)
 *       → Spring Event Bus
 *           → AuditEventListener.onAuditEvent(event)   ← AFTER_COMMIT + @Async
 *               → AuditLogRepository.save(auditLog)    ← new transaction (REQUIRES_NEW)
 * </pre>
 * <p>
 * Key design decisions:
 * <ul>
 *   <li><b>@TransactionalEventListener(AFTER_COMMIT):</b> Only fires after the business
 *       transaction commits successfully. If business tx rolls back → no audit log
 *       for an action that never happened.</li>
 *   <li><b>fallbackExecution = true:</b> For events published outside a transaction
 *       (e.g. login failures, scheduler actions), the listener still executes.</li>
 *   <li><b>@Async("auditExecutor"):</b> Runs on a dedicated thread pool so the caller
 *       (business Service) is never blocked by audit I/O.</li>
 *   <li><b>Propagation.REQUIRES_NEW:</b> Audit log is saved in its own transaction,
 *       completely independent of the business transaction.</li>
 *   <li><b>Try-catch everything:</b> Audit failures are logged but NEVER propagated
 *       back to the caller — they must not break business logic.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Handles audit events after the originating transaction commits.
     * Runs asynchronously on the dedicated "auditExecutor" thread pool.
     *
     * @param event the audit event published by any module
     */
    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(AuditEvent event) {
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

            log.debug("Audit log saved: action={}, entity={}#{}, actor={}",
                    event.getAction(), event.getEntityName(), event.getEntityId(),
                    event.getActorId() != null ? event.getActorId() : "SYSTEM");

        } catch (Exception ex) {
            // CRITICAL: Audit failure must NEVER propagate to business logic.
            // Log the error with full context for ops team to investigate.
            log.error("Failed to save audit log: action={}, entity={}#{}, actor={} — Error: {}",
                    event.getAction(),
                    event.getEntityName(),
                    event.getEntityId(),
                    event.getActorId(),
                    ex.getMessage(),
                    ex);
        }
    }

    // ======================== Helpers ========================

    /**
     * Resolve actor User entity from actorId.
     * Returns null for system-triggered events (actorId == null).
     */
    private User resolveActor(Long actorId) {
        if (actorId == null) {
            return null;
        }
        return userRepository.findById(actorId).orElse(null);
    }

    /**
     * Serialize Map to JSON string for JSONB column.
     * Returns null if input is null or empty.
     */
    private String serializeToJson(Object values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit values to JSON: {}", ex.getMessage());
            return null;
        }
    }
}

