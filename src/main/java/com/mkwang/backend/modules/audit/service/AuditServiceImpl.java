package com.mkwang.backend.modules.audit.service;

import com.mkwang.backend.modules.audit.dto.response.AuditLogResponse;
import com.mkwang.backend.modules.audit.entity.AuditAction;
import com.mkwang.backend.modules.audit.mapper.AuditLogMapper;
import com.mkwang.backend.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only implementation for querying audit logs.
 * All methods are read-only transactions — no writes allowed through this service.
 * Writing is handled exclusively by {@link com.mkwang.backend.modules.audit.listener.AuditEventListener}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    public Page<AuditLogResponse> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable)
                .map(auditLogMapper::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getLogsByAction(AuditAction action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable)
                .map(auditLogMapper::toResponse);
    }

    @Override
    public Page<AuditLogResponse> getLogsByEntityName(String entityName, Pageable pageable) {
        return auditLogRepository.findByEntityNameOrderByCreatedAtDesc(entityName, pageable)
                .map(auditLogMapper::toResponse);
    }

    @Override
    public List<AuditLogResponse> getLogsByEntity(String entityName, String entityId) {
        return auditLogRepository.findByEntityNameAndEntityIdOrderByCreatedAtDesc(entityName, entityId)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    public List<AuditLogResponse> getLogsByActor(Long actorId) {
        return auditLogRepository.findByActor_IdOrderByCreatedAtDesc(actorId)
                .stream()
                .map(auditLogMapper::toResponse)
                .toList();
    }

    @Override
    public Page<AuditLogResponse> getLogsByTimeRange(LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable)
                .map(auditLogMapper::toResponse);
    }
}

