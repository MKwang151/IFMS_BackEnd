package com.mkwang.backend.modules.audit.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.audit.dto.response.AuditLogResponse;
import com.mkwang.backend.modules.audit.entity.AuditAction;
import com.mkwang.backend.modules.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for querying audit logs.
 * All endpoints require AUDIT_LOG_VIEW permission (Admin only).
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('AUDIT_LOG_VIEW')")
public class AuditController {

    private final AuditService auditService;

    /**
     * GET /api/v1/audit/logs — Get all audit logs, paginated.
     * Default: page=0, size=20, sort by createdAt DESC.
     */
    @GetMapping("/logs")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAllLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getAllLogs(pageable)));
    }

    /**
     * GET /api/v1/audit/logs/action/{action} — Filter by action type.
     */
    @GetMapping("/logs/action/{action}")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogsByAction(
            @PathVariable AuditAction action,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByAction(action, pageable)));
    }

    /**
     * GET /api/v1/audit/logs/entity?name=users — Filter by entity type.
     */
    @GetMapping("/logs/entity")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogsByEntityName(
            @RequestParam String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByEntityName(name, pageable)));
    }

    /**
     * GET /api/v1/audit/logs/entity/{name}/{id} — Get change history of a specific entity.
     */
    @GetMapping("/logs/entity/{name}/{id}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByEntity(
            @PathVariable String name,
            @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByEntity(name, id)));
    }

    /**
     * GET /api/v1/audit/logs/actor/{actorId} — Get all actions by a specific user.
     */
    @GetMapping("/logs/actor/{actorId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getLogsByActor(
            @PathVariable Long actorId) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByActor(actorId)));
    }

    /**
     * GET /api/v1/audit/logs/range?from=...&to=... — Filter by time range.
     * Date format: ISO-8601 (yyyy-MM-dd'T'HH:mm:ss)
     */
    @GetMapping("/logs/range")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getLogsByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditService.getLogsByTimeRange(from, to, pageable)));
    }
}

