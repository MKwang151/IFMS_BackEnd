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
import com.mkwang.backend.modules.project.service.CategoryBudgetService;
import com.mkwang.backend.modules.project.service.ProjectQueryService;
import com.mkwang.backend.modules.request.dto.request.AttachmentRequest;
import com.mkwang.backend.modules.request.dto.request.ApproveRequestRequest;
import com.mkwang.backend.modules.request.dto.request.CreateRequestRequest;
import com.mkwang.backend.modules.request.dto.request.DisburseRequest;
import com.mkwang.backend.modules.request.dto.request.RejectRequestRequest;
import com.mkwang.backend.modules.request.dto.request.UpdateRequestRequest;
import com.mkwang.backend.modules.request.dto.response.AccountantDisbursementDetailResponse;
import com.mkwang.backend.modules.request.dto.response.AccountantDisbursementSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.AccountantRejectResponse;
import com.mkwang.backend.modules.request.dto.response.DisburseResponse;
import com.mkwang.backend.modules.request.dto.response.EmployeeRequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApprovalDetailResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApprovalSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApproveResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerRejectResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerRequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.RequestHistoryResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TeamLeaderRequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TlApprovalDetailResponse;
import com.mkwang.backend.modules.request.dto.response.TlApprovalSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TlApproveResponse;
import com.mkwang.backend.modules.request.dto.response.TlRejectResponse;
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
import com.mkwang.backend.modules.profile.dto.request.VerifyMyPinRequest;
import com.mkwang.backend.modules.profile.service.ProfileService;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.service.UserService;
import com.mkwang.backend.modules.wallet.entity.ReferenceType;
import com.mkwang.backend.modules.wallet.entity.TransactionType;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import com.mkwang.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private static final List<RequestType> FLOW1_TYPES =
            List.of(RequestType.ADVANCE, RequestType.EXPENSE, RequestType.REIMBURSE);
    private static final RequestType FLOW2_TYPE = RequestType.PROJECT_TOPUP;

    private final RequestRepository requestRepository;
    private final AdvanceBalanceRepository advanceBalanceRepository;
    private final FileStorageService fileStorageService;
    private final UserService userService;
    private final ProfileService profileService;
    private final ProjectQueryService projectQueryService;
    private final CategoryBudgetService categoryBudgetService;
    private final BusinessCodeGenerator codeGenerator;
    private final RequestMapper requestMapper;
    private final WalletService walletService;

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

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_TEAM_LEADER')")
    public PageResponse<TlApprovalSummaryResponse> getTlApprovals(
            Long leaderId, RequestType type, Long projectId, String search, int page, int size) {

        if (type != null && !FLOW1_TYPES.contains(type)) {
            throw new BadRequestException("Team Leader approvals only support ADVANCE/EXPENSE/REIMBURSE");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        List<Long> leaderProjectIds = projectQueryService.getLeaderProjectIds(leaderId);
        if (leaderProjectIds.isEmpty()) {
            return PageResponse.<TlApprovalSummaryResponse>builder()
                    .items(List.of())
                    .total(0L)
                    .page(safePage)
                    .size(safeSize)
                    .totalPages(0)
                    .build();
        }

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Request> spec = RequestSpecification.filterForTlApprovals(leaderProjectIds, type, projectId, search);

        Page<TlApprovalSummaryResponse> result = requestRepository
                .findAll(spec, pageable)
                .map(requestMapper::toTlApprovalSummaryResponse);

        return PageResponse.<TlApprovalSummaryResponse>builder()
                .items(result.getContent())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_TEAM_LEADER')")
    public TlApprovalDetailResponse getTlApprovalDetail(Long id, Long leaderId) {
        List<Long> leaderProjectIds = projectQueryService.getLeaderProjectIds(leaderId);
        if (leaderProjectIds.isEmpty()) {
            throw new ResourceNotFoundException("Request not found");
        }

        Request request = requestRepository.findDetailByIdForTl(id, leaderProjectIds)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        return requestMapper.toTlApprovalDetailResponse(request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_TEAM_LEADER')")
    public TlApproveResponse approveTlRequest(Long id, Long leaderId, ApproveRequestRequest req) {
        List<Long> leaderProjectIds = projectQueryService.getLeaderProjectIds(leaderId);
        if (leaderProjectIds.isEmpty()) {
            throw new ResourceNotFoundException("Request not found");
        }

        Request request = requestRepository.findDetailByIdForTl(id, leaderProjectIds)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved");
        }
        if (!FLOW1_TYPES.contains(request.getType())) {
            throw new BadRequestException("Only ADVANCE/EXPENSE/REIMBURSE requests are handled by Team Leader");
        }

        BigDecimal effectiveAmount = req.getApprovedAmount() != null ? req.getApprovedAmount() : request.getAmount();
        if (effectiveAmount.compareTo(request.getAmount()) > 0) {
            throw new BadRequestException("approvedAmount cannot exceed requested amount");
        }

        request.setStatus(RequestStatus.APPROVED_BY_TEAM_LEADER);
        request.setApprovedAmount(effectiveAmount);

        User actor = userService.getUserById(leaderId);
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.APPROVE)
                .statusAfterAction(RequestStatus.APPROVED_BY_TEAM_LEADER)
                .comment(req.getComment())
                .build());

        requestRepository.save(request);

        if (request.getType() == RequestType.ADVANCE || request.getType() == RequestType.EXPENSE) {
            walletService.lockFunds(WalletOwnerType.PROJECT, request.getProject().getId(), effectiveAmount);
        }

        return requestMapper.toTlApproveResponse(request, req.getComment());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_TEAM_LEADER')")
    public TlRejectResponse rejectTlRequest(Long id, Long leaderId, RejectRequestRequest req) {
        List<Long> leaderProjectIds = projectQueryService.getLeaderProjectIds(leaderId);
        if (leaderProjectIds.isEmpty()) {
            throw new ResourceNotFoundException("Request not found");
        }

        Request request = requestRepository.findDetailByIdForTl(id, leaderProjectIds)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected");
        }
        if (!FLOW1_TYPES.contains(request.getType())) {
            throw new BadRequestException("Only ADVANCE/EXPENSE/REIMBURSE requests are handled by Team Leader");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectReason(req.getReason());

        User actor = userService.getUserById(leaderId);
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.REJECT)
                .statusAfterAction(RequestStatus.REJECTED)
                .comment(req.getReason())
                .build());

        requestRepository.save(request);
        return requestMapper.toTlRejectResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_PROJECT_TOPUP')")
    public PageResponse<ManagerApprovalSummaryResponse> getManagerApprovals(Long managerId, String search, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);

        Long departmentId = getManagerDepartmentId(managerId);

        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Request> spec = RequestSpecification.filterForManagerApprovals(departmentId, search);

        Page<ManagerApprovalSummaryResponse> result = requestRepository
                .findAll(spec, pageable)
                .map(requestMapper::toManagerApprovalSummaryResponse);

        return PageResponse.<ManagerApprovalSummaryResponse>builder()
                .items(result.getContent())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_PROJECT_TOPUP')")
    public ManagerApprovalDetailResponse getManagerApprovalDetail(Long id, Long managerId) {
        Long departmentId = getManagerDepartmentId(managerId);

        Request request = requestRepository.findDetailByIdForManager(id, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        List<RequestHistoryResponse> timeline = requestRepository.findHistoriesByRequestId(id)
                .stream()
                .map(requestMapper::toHistoryResponse)
                .toList();

        return requestMapper.toManagerApprovalDetailResponse(request, timeline);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_PROJECT_TOPUP')")
    public ManagerApproveResponse approveManagerRequest(Long id, Long managerId, ApproveRequestRequest req) {
        Long departmentId = getManagerDepartmentId(managerId);

        Request request = requestRepository.findDetailByIdForManager(id, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be approved");
        }
        if (request.getType() != FLOW2_TYPE) {
            throw new BadRequestException("Only PROJECT_TOPUP requests are handled by Manager");
        }

        BigDecimal effectiveAmount = req.getApprovedAmount() != null ? req.getApprovedAmount() : request.getAmount();
        if (effectiveAmount.compareTo(request.getAmount()) > 0) {
            throw new BadRequestException("approvedAmount cannot exceed requested amount");
        }

        User actor = userService.getUserById(managerId);

        request.setStatus(RequestStatus.APPROVED_BY_MANAGER);
        request.setApprovedAmount(effectiveAmount);
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.APPROVE)
                .statusAfterAction(RequestStatus.APPROVED_BY_MANAGER)
                .comment(req.getComment())
                .build());

        Long projectId = request.getProject().getId();
        walletService.transfer(
                WalletOwnerType.DEPARTMENT, departmentId,
                WalletOwnerType.PROJECT, projectId,
                effectiveAmount,
                TransactionType.PROJECT_QUOTA_ALLOCATION,
                ReferenceType.REQUEST, request.getId(),
                "PROJECT_TOPUP approved by manager — request " + request.getRequestCode()
        );

        request.setStatus(RequestStatus.PAID);
        request.setPaidAt(LocalDateTime.now());
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.PAYOUT)
                .statusAfterAction(RequestStatus.PAID)
                .comment("Auto-paid on manager approval")
                .build());

        requestRepository.save(request);
        return requestMapper.toManagerApproveResponse(request, req.getComment());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_PROJECT_TOPUP')")
    public ManagerRejectResponse rejectManagerRequest(Long id, Long managerId, RejectRequestRequest req) {
        Long departmentId = getManagerDepartmentId(managerId);

        Request request = requestRepository.findDetailByIdForManager(id, departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BadRequestException("Only PENDING requests can be rejected");
        }
        if (request.getType() != FLOW2_TYPE) {
            throw new BadRequestException("Only PROJECT_TOPUP requests are handled by Manager");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.setRejectReason(req.getReason());

        User actor = userService.getUserById(managerId);
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.REJECT)
                .statusAfterAction(RequestStatus.REJECTED)
                .comment(req.getReason())
                .build());

        requestRepository.save(request);
        return requestMapper.toManagerRejectResponse(request);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_APPROVED')")
    public PageResponse<AccountantDisbursementSummaryResponse> getAccountantDisbursements(
            RequestType type, String search, int page, int size) {

        if (type != null && !FLOW1_TYPES.contains(type)) {
            throw new BadRequestException("Accountant disbursements only support ADVANCE/EXPENSE/REIMBURSE");
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<Request> spec = RequestSpecification.filterForAccountantDisbursements(type, search);

        Page<AccountantDisbursementSummaryResponse> result = requestRepository
                .findAll(spec, pageable)
                .map(requestMapper::toAccountantDisbursementSummaryResponse);

        return PageResponse.<AccountantDisbursementSummaryResponse>builder()
                .items(result.getContent())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(result.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_APPROVED')")
    public AccountantDisbursementDetailResponse getAccountantDisbursementDetail(Long id) {
        Request request = requestRepository.findDetailByIdForAccountant(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        List<RequestHistoryResponse> timeline = requestRepository.findHistoriesByRequestId(id)
                .stream()
                .map(requestMapper::toHistoryResponse)
                .toList();

        return requestMapper.toAccountantDisbursementDetailResponse(request, timeline);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_PAYOUT')")
    public DisburseResponse disburse(Long id, Long accountantId, DisburseRequest req) {
        profileService.verifyMyPin(accountantId, new VerifyMyPinRequest(req.getPin()));

        Request request = requestRepository.findDetailByIdForAccountant(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.APPROVED_BY_TEAM_LEADER) {
            throw new BadRequestException("Only APPROVED_BY_TEAM_LEADER requests can be disbursed");
        }

        BigDecimal amount = request.getApprovedAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("approvedAmount is required before disbursement");
        }

        String description = (req.getNote() != null && !req.getNote().isBlank())
                ? req.getNote().trim()
                : request.getType() + " payout - " + request.getRequestCode();

        String transactionCode = null;

        if (request.getType() == RequestType.ADVANCE || request.getType() == RequestType.EXPENSE) {
            Long projectId = request.getProject().getId();
            Long requesterId = request.getRequester().getId();

            var txn = walletService.settleAndTransfer(
                    WalletOwnerType.PROJECT, projectId,
                    WalletOwnerType.USER, requesterId,
                    amount,
                    TransactionType.REQUEST_PAYMENT,
                    ReferenceType.REQUEST, request.getId(),
                    description
            );
            transactionCode = txn.getTransactionCode();

            if (request.getType() == RequestType.ADVANCE) {
                AdvanceBalance advanceBalance = AdvanceBalance.builder()
                        .user(request.getRequester())
                        .advanceRequest(request)
                        .originalAmount(amount)
                        .remainingAmount(amount)
                        .build();
                advanceBalanceRepository.save(advanceBalance);
            }
        } else if (request.getType() == RequestType.REIMBURSE) {
            AdvanceBalance advanceBalance = request.getAdvanceBalance();
            if (advanceBalance == null) {
                throw new BadRequestException("REIMBURSE request must have a linked advance balance");
            }
            advanceBalance.reimburse(amount);
            advanceBalanceRepository.save(advanceBalance);
        }

        if (request.getPhase() != null && request.getCategory() != null) {
            categoryBudgetService.incrementSpent(
                    request.getPhase().getId(),
                    request.getCategory().getId(),
                    amount
            );
        }

        User actor = userService.getUserById(accountantId);
        request.setStatus(RequestStatus.PAID);
        request.setPaidAt(LocalDateTime.now());
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.PAYOUT)
                .statusAfterAction(RequestStatus.PAID)
                .comment(req.getNote())
                .build());

        requestRepository.save(request);
        return requestMapper.toDisburseResponse(request, transactionCode);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('REQUEST_PAYOUT')")
    public AccountantRejectResponse accountantReject(Long id, Long accountantId, RejectRequestRequest req) {
        Request request = requestRepository.findDetailByIdForAccountant(id)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        if (request.getStatus() != RequestStatus.APPROVED_BY_TEAM_LEADER) {
            throw new BadRequestException("Only APPROVED_BY_TEAM_LEADER requests can be rejected at this stage");
        }

        if (request.getType() == RequestType.ADVANCE || request.getType() == RequestType.EXPENSE) {
            walletService.unlockFunds(
                    WalletOwnerType.PROJECT,
                    request.getProject().getId(),
                    request.getApprovedAmount()
            );
        }

        User actor = userService.getUserById(accountantId);
        request.setStatus(RequestStatus.REJECTED);
        request.setRejectReason(req.getReason());
        request.getHistories().add(RequestHistory.builder()
                .request(request)
                .actor(actor)
                .action(RequestAction.REJECT)
                .statusAfterAction(RequestStatus.REJECTED)
                .comment(req.getReason())
                .build());

        requestRepository.save(request);
        return requestMapper.toAccountantRejectResponse(request);
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

    private Long getManagerDepartmentId(Long managerId) {
        User manager = userService.getUserById(managerId);
        if (manager.getDepartment() == null || manager.getDepartment().getId() == null) {
            throw new BadRequestException("Manager must belong to a department");
        }
        return manager.getDepartment().getId();
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

