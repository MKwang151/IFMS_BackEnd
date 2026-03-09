package com.mkwang.backend.modules.audit.service;

import com.mkwang.backend.modules.audit.dto.response.AuditLogResponse;
import com.mkwang.backend.modules.audit.entity.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only service for querying audit logs.
 * Only accessible by Admin (AUDIT_LOG_VIEW permission).
 */
public interface AuditService {

    /**
     * Get all audit logs, paginated, newest first.
     */
    Page<AuditLogResponse> getAllLogs(Pageable pageable);

    /**
     * Get audit logs by action type, paginated.
     */
    Page<AuditLogResponse> getLogsByAction(AuditAction action, Pageable pageable);

    /**
     * Get audit logs by entity type (table name), paginated.
     */
    Page<AuditLogResponse> getLogsByEntityName(String entityName, Pageable pageable);

    /**
     * Get audit logs for a specific entity instance (e.g. all changes to user #5).
     */
    List<AuditLogResponse> getLogsByEntity(String entityName, String entityId);

    /**
     * Get audit logs by actor (who performed the actions).
     */
    List<AuditLogResponse> getLogsByActor(Long actorId);

    /**
     * Get audit logs within a time range, paginated.
     */
    Page<AuditLogResponse> getLogsByTimeRange(LocalDateTime from, LocalDateTime to, Pageable pageable);
}

