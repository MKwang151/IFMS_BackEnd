package com.mkwang.backend.modules.audit.repository;

import com.mkwang.backend.modules.audit.entity.AuditAction;
import com.mkwang.backend.modules.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /** All audit logs created by a specific actor, newest first */
    List<AuditLog> findByActor_IdOrderByCreatedAtDesc(Long actorId);

    /** All audit logs for a specific entity (e.g. all changes to a department) */
    List<AuditLog> findByEntityNameAndEntityIdOrderByCreatedAtDesc(String entityName, String entityId);

    /** All audit logs for a specific action type, paginated */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action, Pageable pageable);

    /** All audit logs within a time range, paginated */
    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    /** All audit logs for a given entity type, paginated */
    Page<AuditLog> findByEntityNameOrderByCreatedAtDesc(String entityName, Pageable pageable);
}
