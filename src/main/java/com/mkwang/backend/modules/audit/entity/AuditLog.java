package com.mkwang.backend.modules.audit.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * AuditLog entity - Module 10: System Audit Trail.
 *
 * Records every state-changing operation on critical data (config, permissions,
 * budgets, system status). This table is strictly Append-Only —
 * no UPDATE or DELETE is ever performed on it.
 *
 * Mapped to table: audit_logs
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who performed the action.
     * Nullable — system-triggered actions may have no actor.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    /**
     * Classification of the action performed.
     * e.g. USER_LOCKED, ROLE_ASSIGNED, QUOTA_TOPUP
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private AuditAction action;

    /**
     * Name of the affected table / entity.
     * e.g. "departments", "users", "system_configs"
     */
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    /**
     * ID of the affected row (stored as String to support composite keys).
     */
    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    /**
     * Snapshot of the entity state BEFORE the change (JSON).
     * Nullable for CREATE actions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    /**
     * Snapshot of the entity state AFTER the change (JSON).
     * Nullable for DELETE actions.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    // created_at is inherited from BaseEntity
}
