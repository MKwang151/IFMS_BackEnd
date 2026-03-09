package com.mkwang.backend.modules.audit.event;

import com.mkwang.backend.modules.audit.entity.AuditAction;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Domain event fired whenever an auditable action occurs in any module.
 * <p>
 * This is a plain POJO (not extending ApplicationEvent) — Spring 4.2+
 * supports publishing any object as an event.
 * <p>
 * Usage in any Service:
 * <pre>
 *   auditEventPublisher.publish(AuditEvent.builder()
 *       .actorId(currentUserId)
 *       .action(AuditAction.USER_LOCKED)
 *       .entityName("users")
 *       .entityId(String.valueOf(targetUserId))
 *       .oldValues(Map.of("status", "ACTIVE"))
 *       .newValues(Map.of("status", "LOCKED"))
 *       .build());
 * </pre>
 */
@Getter
@Builder
@ToString(exclude = {"oldValues", "newValues"})
public class AuditEvent {

    /**
     * ID of the user who performed the action.
     * Null for system-triggered actions (scheduler, startup).
     */
    private final Long actorId;

    /**
     * Classification of the action — maps to AuditAction enum.
     */
    private final AuditAction action;

    /**
     * Name of the affected table / entity (e.g. "users", "departments").
     */
    private final String entityName;

    /**
     * ID of the affected row. String type to support composite keys.
     */
    private final String entityId;

    /**
     * State BEFORE the change. Null for CREATE actions.
     * Keys = field names, Values = field values (serialized to JSON by listener).
     */
    private final Map<String, Object> oldValues;

    /**
     * State AFTER the change. Null for DELETE actions.
     * Keys = field names, Values = field values (serialized to JSON by listener).
     */
    private final Map<String, Object> newValues;

    /**
     * Timestamp when the event was created (captured at publish time).
     * Defaults to now if not explicitly set.
     */
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
}

