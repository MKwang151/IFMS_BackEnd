package com.mkwang.backend.modules.audit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO for audit log entries.
 * All fields use camelCase (no @JsonProperty overrides).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

    private Long id;
    private Long actorId;
    private String actorEmail;
    private String action;
    private String entityName;
    private String entityId;
    private String oldValues;
    private String newValues;
    private LocalDateTime createdAt;
}

