package com.mkwang.backend.modules.request.service;

import com.mkwang.backend.common.dto.PageResponse;
import com.mkwang.backend.modules.request.dto.request.ApproveRequestRequest;
import com.mkwang.backend.modules.request.dto.request.CreateRequestRequest;
import com.mkwang.backend.modules.request.dto.request.DisburseRequest;
import com.mkwang.backend.modules.request.dto.request.RejectRequestRequest;
import com.mkwang.backend.modules.request.dto.request.UpdateRequestRequest;
import com.mkwang.backend.modules.request.dto.response.AccountantDisbursementDetailResponse;
import com.mkwang.backend.modules.request.dto.response.AccountantDisbursementSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.AccountantRejectResponse;
import com.mkwang.backend.modules.request.dto.response.DisburseResponse;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApprovalDetailResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApprovalSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerApproveResponse;
import com.mkwang.backend.modules.request.dto.response.ManagerRejectResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TlApprovalDetailResponse;
import com.mkwang.backend.modules.request.dto.response.TlApprovalSummaryResponse;
import com.mkwang.backend.modules.request.dto.response.TlApproveResponse;
import com.mkwang.backend.modules.request.dto.response.TlRejectResponse;
import com.mkwang.backend.modules.request.entity.RequestStatus;
import com.mkwang.backend.modules.request.entity.RequestType;

public interface RequestService {

    PageResponse<RequestSummaryResponse> getMyRequests(
            Long userId, RequestType type, RequestStatus status, String search, int page, int limit);

    Object getMyRequestSummary(Long userId, String roleName);

    RequestDetailResponse getRequestDetail(Long id, Long userId);

    RequestDetailResponse createRequest(CreateRequestRequest req, Long userId);

    RequestDetailResponse updateRequest(Long id, UpdateRequestRequest req, Long userId);

    void cancelRequest(Long id, Long userId);

    PageResponse<TlApprovalSummaryResponse> getTlApprovals(
            Long leaderId, RequestType type, Long projectId, String search, int page, int size);

    TlApprovalDetailResponse getTlApprovalDetail(Long id, Long leaderId);

    TlApproveResponse approveTlRequest(Long id, Long leaderId, ApproveRequestRequest req);

    TlRejectResponse rejectTlRequest(Long id, Long leaderId, RejectRequestRequest req);

    PageResponse<ManagerApprovalSummaryResponse> getManagerApprovals(Long managerId, String search, int page, int size);

    ManagerApprovalDetailResponse getManagerApprovalDetail(Long id, Long managerId);

    ManagerApproveResponse approveManagerRequest(Long id, Long managerId, ApproveRequestRequest req);

    ManagerRejectResponse rejectManagerRequest(Long id, Long managerId, RejectRequestRequest req);

    PageResponse<AccountantDisbursementSummaryResponse> getAccountantDisbursements(
            RequestType type, String search, int page, int size);

    AccountantDisbursementDetailResponse getAccountantDisbursementDetail(Long id);

    DisburseResponse disburse(Long id, Long accountantId, DisburseRequest req);

    AccountantRejectResponse accountantReject(Long id, Long accountantId, RejectRequestRequest req);
}

