package com.mkwang.backend.modules.audit.mapper;

import com.mkwang.backend.modules.audit.dto.response.AuditLogResponse;
import com.mkwang.backend.modules.audit.entity.AuditLog;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for AuditLog entity ↔ DTO.
 * No business logic — only data copying.
 */
@Component
public class AuditLogMapper {

    /**
     * Entity → Response DTO.
     */
    public AuditLogResponse toResponse(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .actorId(entity.getActor() != null ? entity.getActor().getId() : null)
                .actorEmail(entity.getActor() != null ? entity.getActor().getEmail() : null)
                .action(entity.getAction().name())
                .entityName(entity.getEntityName())
                .entityId(entity.getEntityId())
                .oldValues(entity.getOldValues())
                .newValues(entity.getNewValues())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}

