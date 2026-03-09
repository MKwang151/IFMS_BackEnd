package com.mkwang.backend.modules.audit.event;

import com.mkwang.backend.modules.audit.entity.AuditAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Public API for publishing audit events from any module.
 * <p>
 * This is the ONLY class other modules need to import from the audit module.
 * It wraps Spring's ApplicationEventPublisher to provide a clean, type-safe API.
 * <p>
 * The actual persistence is handled asynchronously by
 * {@link com.mkwang.backend.modules.audit.listener.AuditEventListener AuditEventListener}
 * AFTER the calling transaction commits — so audit failures never break business logic.
 * <p>
 * Example usage in UserService:
 * <pre>
 *   auditEventPublisher.publish(
 *       AuditAction.USER_LOCKED,
 *       "users",
 *       String.valueOf(user.getId()),
 *       currentUserId,
 *       Map.of("status", "ACTIVE"),
 *       Map.of("status", "LOCKED")
 *   );
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Publish a full AuditEvent object.
     *
     * @param event pre-built AuditEvent
     */
    public void publish(AuditEvent event) {
        log.debug("Publishing audit event: action={}, entity={}, entityId={}",
                event.getAction(), event.getEntityName(), event.getEntityId());
        eventPublisher.publishEvent(event);
    }

    /**
     * Convenience method — publish with all parameters (with old/new values).
     *
     * @param action     the audit action classification
     * @param entityName table/entity name affected
     * @param entityId   ID of the affected row
     * @param actorId    user who performed the action (null for system)
     * @param oldValues  state before change (null for CREATE)
     * @param newValues  state after change (null for DELETE)
     */
    public void publish(AuditAction action, String entityName, String entityId,
                        Long actorId, Map<String, Object> oldValues, Map<String, Object> newValues) {
        publish(AuditEvent.builder()
                .actorId(actorId)
                .action(action)
                .entityName(entityName)
                .entityId(entityId)
                .oldValues(oldValues)
                .newValues(newValues)
                .build());
    }

    /**
     * Convenience method — publish without old/new values (e.g. login events).
     *
     * @param action     the audit action classification
     * @param entityName table/entity name affected
     * @param entityId   ID of the affected row
     * @param actorId    user who performed the action (null for system)
     */
    public void publish(AuditAction action, String entityName, String entityId, Long actorId) {
        publish(action, entityName, entityId, actorId, null, null);
    }
}


