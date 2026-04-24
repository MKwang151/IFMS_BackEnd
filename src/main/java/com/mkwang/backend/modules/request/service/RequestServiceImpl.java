package com.mkwang.backend.modules.request.service;

import com.mkwang.backend.common.dto.PageResponse;
import com.mkwang.backend.common.exception.AdvanceBalanceAlreadySettledException;
import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeGenerator;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeType;
import com.mkwang.backend.modules.file.dto.request.FileStorageRequest;
import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.file.service.FileStorageService;
import com.mkwang.backend.modules.project.entity.ExpenseCategory;
import com.mkwang.backend.modules.project.entity.Project;
import com.mkwang.backend.modules.project.entity.ProjectPhase;
import com.mkwang.backend.modules.project.service.ProjectQueryService;
import com.mkwang.backend.modules.request.dto.request.AttachmentRequest;
import com.mkwang.backend.modules.request.dto.request.CreateRequestRequest;
import com.mkwang.backend.modules.request.dto.request.UpdateRequestRequest;
import com.mkwang.backend.modules.request.dto.response.EmployeeRequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerRequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.RequestHistoryResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TeamLeaderRequestSummaryResponse;
import com.mkwang.backend.modules.request.entity.AdvanceBalance;
import com.mkwang.backend.modules.request.entity.Request;
import com.mkwang.backend.modules.request.entity.RequestAction;
import com.mkwang.backend.modules.request.entity.RequestHistory;
import com.mkwang.backend.modules.request.entity.RequestStatus;
import com.mkwang.backend.modules.request.entity.RequestType;
import com.mkwang.backend.modules.request.mapper.RequestMapper;
import com.mkwang.backend.modules.request.repository.AdvanceBalanceRepository;
import com.mkwang.backend.modules.request.repository.RequestRepository;
import com.mkwang.backend.modules.request.repository.RequestSpecification;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final AdvanceBalanceRepository advanceBalanceRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final ProjectQueryService projectQueryService;
    private final BusinessCodeGenerator codeGenerator;
    private final RequestMapper requestMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_SELF')")
    public PageResponse<RequestSummaryResponse> getMyRequests(Long userId, RequestType type, RequestStatus status, String search, int page, int limit) {
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(safePage - 1, safeLimit);

        Specification<Request> spec = RequestSpecification.filter(userId, type, status, search);

        Page<RequestSummaryResponse> result = requestRepository
                .findAll(spec, pageable)
                .map(requestMapper::toSummaryResponse);

        return PageResponse.<RequestSummaryResponse>builder()
                .items(result.getContent())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeLimit)
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_SELF')")
    public Object getMyRequestSummary(Long userId, String roleName) {
        Map<RequestStatus, Long> countsByStatus = requestRepository.countByStatusForUser(userId)
                .stream()
                .collect(Collectors.toMap(
                        row -> (RequestStatus) row[0],
                        row -> (Long) row[1]));

        String normalizedRole = roleName == null ? "" : roleName.toUpperCase();

        return switch (normalizedRole) {
            case "EMPLOYEE" -> EmployeeRequestSummaryResponse.builder()
                    .totalPendingApproval(countsByStatus.getOrDefault(RequestStatus.PENDING, 0L))
                    .totalApproved(countsByStatus.getOrDefault(RequestStatus.APPROVED_BY_TEAM_LEADER, 0L))
                    .totalRejected(countsByStatus.getOrDefault(RequestStatus.REJECTED, 0L))
                    .totalPaid(countsByStatus.getOrDefault(RequestStatus.PAID, 0L))
                    .totalCancelled(countsByStatus.getOrDefault(RequestStatus.CANCELLED, 0L))
                    .build();

            case "TEAM_LEADER", "TEAMLEADER" -> TeamLeaderRequestSummaryResponse.builder()
                    .totalPendingManagerApproval(countsByStatus.getOrDefault(RequestStatus.PENDING, 0L))
                    .totalApproved(countsByStatus.getOrDefault(RequestStatus.APPROVED_BY_MANAGER, 0L))
                    .totalRejected(countsByStatus.getOrDefault(RequestStatus.REJECTED, 0L))
                    .totalPaid(countsByStatus.getOrDefault(RequestStatus.PAID, 0L))
                    .totalCancelled(countsByStatus.getOrDefault(RequestStatus.CANCELLED, 0L))
                    .build();

            case "MANAGER" -> ManagerRequestSummaryResponse.builder()
                    .totalPendingCfoApproval(countsByStatus.getOrDefault(RequestStatus.PENDING, 0L))
                    .totalApproved(countsByStatus.getOrDefault(RequestStatus.APPROVED_BY_CFO, 0L))
                    .totalRejected(countsByStatus.getOrDefault(RequestStatus.REJECTED, 0L))
                    .totalPaid(countsByStatus.getOrDefault(RequestStatus.PAID, 0L))
                    .totalCancelled(countsByStatus.getOrDefault(RequestStatus.CANCELLED, 0L))
                    .build();

            default -> throw new BadRequestException("Unsupported role for request summary: " + roleName);
        };
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_SELF')")
    public RequestDetailResponse getRequestDetail(Long id, Long userId) {
        Request request = requestRepository.findDetailByIdAndRequesterId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        List<RequestHistoryResponse> timeline = requestRepository.findHistoriesByRequestId(id)
                .stream()
                .map(requestMapper::toHistoryResponse)
                .toList();

        return requestMapper.toDetailResponse(request, timeline);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_CREATE')")
    public RequestDetailResponse createRequest(CreateRequestRequest req, Long userId) {
        User requester = userService.getUserById(userId);
        validateCreateRequest(req, requester);

        Project project = req.getProjectId() != null ? projectQueryService.getProjectEntityById(req.getProjectId()) : null;
        ProjectPhase phase = req.getPhaseId() != null ? projectQueryService.getPhaseEntityById(req.getPhaseId()) : null;
        ExpenseCategory category = req.getCategoryId() != null ? projectQueryService.getCategoryEntityById(req.getCategoryId()) : null;

        AdvanceBalance advanceBalance = null;
        if (req.getType() == RequestType.REIMBURSE) {
            advanceBalance = advanceBalanceRepository.findById(req.getAdvanceBalanceId())
                    .orElseThrow(() -> new ResourceNotFoundException("AdvanceBalance not found"));

            if (!advanceBalance.getUser().getId().equals(userId)) {
                throw new BadRequestException("Advance balance does not belong to you");
            }
            if (advanceBalance.isSettled()) {
                throw new AdvanceBalanceAlreadySettledException(advanceBalance.getId());
            }
        }

        String requestCode = codeGenerator.generate(BusinessCodeType.REQUEST, resolveDepartmentCode(requester));
        List<FileStorage> savedFiles = fileStorageService.saveAll(toFileStorageRequests(req.getAttachments()));

        Request request = Request.builder()
                .requestCode(requestCode)
                .requester(requester)
                .project(project)
                .phase(phase)
                .category(category)
                .advanceBalance(advanceBalance)
                .type(req.getType())
                .amount(req.getAmount())
                .description(req.getDescription())
                .status(RequestStatus.PENDING)
                .build();

        savedFiles.forEach(request::addAttachment);
        Request saved = requestRepository.save(request);
        return requestMapper.toDetailResponse(saved, List.of());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_CREATE')")
    public RequestDetailResponse updateRequest(Long id, UpdateRequestRequest req, Long userId) {
        Request request = requestRepository.findByIdAndRequesterId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Request cannot be modified after approval");
        }

        if (req.getAmount() != null) {
            request.setAmount(req.getAmount());
        }
        if (req.getDescription() != null) {
            request.setDescription(req.getDescription());
        }

        if (req.getAttachments() != null) {
            if (request.requiresProof() && req.getAttachments().isEmpty()) {
                throw new BadRequestException("Attachments required for " + request.getType());
            }

            request.getAttachments().clear();
            List<FileStorage> files = fileStorageService.saveAll(toFileStorageRequests(req.getAttachments()));
            files.forEach(request::addAttachment);
        }

        requestRepository.save(request);
        return getRequestDetail(id, userId);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_CREATE')")
    public void cancelRequest(Long id, Long userId) {
        Request request = requestRepository.findByIdAndRequesterId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (!request.isCancellable()) {
            throw new BadRequestException("Request can only be cancelled when PENDING");
        }

        User actor = userService.getUserById(userId);
        request.setStatus(RequestStatus.CANCELLED);
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.CANCEL)
                .statusAfterAction(RequestStatus.CANCELLED)
                .comment(null)
                .build());

        requestRepository.save(request);
    }

    private void validateCreateRequest(CreateRequestRequest req, User requester) {
        RequestType type = req.getType();
        validateRoleAllowedType(type, requester);

        boolean hasAttachments = req.getAttachments() != null && !req.getAttachments().isEmpty();

        switch (type) {
            case ADVANCE, EXPENSE, REIMBURSE -> {
                requireNotNull(req.getProjectId(), "projectId is required for type " + type);
                requireNotNull(req.getPhaseId(), "phaseId is required for type " + type);
                requireNotNull(req.getCategoryId(), "categoryId is required for type " + type);
            }
            case PROJECT_TOPUP -> {
                requireNotNull(req.getProjectId(), "projectId is required for type " + type);
                requireNull(req.getPhaseId(), "phaseId must be null for type PROJECT_TOPUP");
                requireNull(req.getCategoryId(), "categoryId must be null for type PROJECT_TOPUP");
                requireNull(req.getAdvanceBalanceId(), "advanceBalanceId must be null for type PROJECT_TOPUP");
                if (hasAttachments) {
                    throw new BadRequestException("Attachments are not allowed for type PROJECT_TOPUP");
                }
            }
            case DEPARTMENT_TOPUP -> {
                requireNull(req.getProjectId(), "projectId must be null for type DEPARTMENT_TOPUP");
                requireNull(req.getPhaseId(), "phaseId must be null for type DEPARTMENT_TOPUP");
                requireNull(req.getCategoryId(), "categoryId must be null for type DEPARTMENT_TOPUP");
                requireNull(req.getAdvanceBalanceId(), "advanceBalanceId must be null for type DEPARTMENT_TOPUP");
                if (hasAttachments) {
                    throw new BadRequestException("Attachments are not allowed for type DEPARTMENT_TOPUP");
                }
            }
            default -> throw new BadRequestException("Unsupported request type: " + type);
        }

        if (type == RequestType.REIMBURSE) {
            requireNotNull(req.getAdvanceBalanceId(), "advanceBalanceId is required for type REIMBURSE");
        } else {
            requireNull(req.getAdvanceBalanceId(), "advanceBalanceId must be null for type " + type);
        }

        if ((type == RequestType.EXPENSE || type == RequestType.REIMBURSE) && !hasAttachments) {
            throw new BadRequestException("Attachments required for " + type);
        }
    }

    private void validateRoleAllowedType(RequestType type, User requester) {
        if (requester.getRole() == null || requester.getRole().getName() == null) {
            throw new BadRequestException("Requester role is missing");
        }

        String roleName = requester.getRole().getName().toUpperCase();
        boolean allowed = switch (roleName) {
            case "EMPLOYEE" -> type == RequestType.ADVANCE
                    || type == RequestType.EXPENSE
                    || type == RequestType.REIMBURSE;
            case "TEAM_LEADER" -> type == RequestType.PROJECT_TOPUP;
            case "MANAGER" -> type == RequestType.DEPARTMENT_TOPUP;
            default -> false;
        };

        if (!allowed) {
            throw new BadRequestException("Role " + roleName + " is not allowed to create request type " + type);
        }
    }

    private List<FileStorageRequest> toFileStorageRequests(List<AttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return List.of();
        }

        return attachments.stream()
                .map(this::toFileStorageRequest)
                .toList();
    }

    private FileStorageRequest toFileStorageRequest(AttachmentRequest att) {
        FileStorageRequest req = new FileStorageRequest();
        req.setFileName(att.getFileName());
        req.setCloudinaryPublicId(att.getCloudinaryPublicId());
        req.setUrl(att.getUrl());
        req.setFileType(att.getFileType());
        req.setSize(att.getSize());
        return req;
    }

    private String resolveDepartmentCode(User requester) {
        if (requester.getDepartment() == null || requester.getDepartment().getCode() == null
                || requester.getDepartment().getCode().isBlank()) {
            throw new BadRequestException("Requester department code is missing");
        }
        return requester.getDepartment().getCode();
    }

    private void requireNotNull(Object value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
    }

    private void requireNull(Object value, String message) {
        if (value != null) {
            throw new BadRequestException(message);
        }
    }
}

