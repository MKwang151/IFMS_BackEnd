package com.mkwang.backend.modules.request.service;

import com.mkwang.backend.common.dto.PageResponse;
import com.mkwang.backend.modules.request.dto.request.CreateRequestRequest;
import com.mkwang.backend.modules.request.dto.request.UpdateRequestRequest;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
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
}

