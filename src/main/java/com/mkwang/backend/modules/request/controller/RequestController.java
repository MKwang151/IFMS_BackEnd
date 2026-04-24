package com.mkwang.backend.modules.request.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.common.dto.PageResponse;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.request.dto.request.CreateRequestRequest;
import com.mkwang.backend.modules.request.dto.request.UpdateRequestRequest;
import com.mkwang.backend.modules.request.dto.response.RequestDetailResponse;
import com.mkwang.backend.modules.request.dto.response.RequestSummaryResponse;
import com.mkwang.backend.modules.request.entity.RequestStatus;
import com.mkwang.backend.modules.request.entity.RequestType;
import com.mkwang.backend.modules.request.service.RequestService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Tag(name = "Request", description = "Employee request management")
@SecurityRequirement(name = "bearerAuth")
public class RequestController {

    private final RequestService requestService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<RequestSummaryResponse>>> getMyRequests(
            @AuthenticationPrincipal UserDetailsAdapter principal,
            @RequestParam(required = false) RequestType type,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.getMyRequests(principal.getUser().getId(), type, status, search, page, limit)
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Object>> getMyRequestSummary(
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        String roleName = principal.getUser().getRole() != null ? principal.getUser().getRole().getName() : null;
        return ResponseEntity.ok(ApiResponse.success(
                requestService.getMyRequestSummary(principal.getUser().getId(), roleName)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> getRequestDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.getRequestDetail(id, principal.getUser().getId())
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequestDetailResponse>> createRequest(
            @Valid @RequestBody CreateRequestRequest req,
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                requestService.createRequest(req, principal.getUser().getId())
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RequestDetailResponse>> updateRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequestRequest req,
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        return ResponseEntity.ok(ApiResponse.success(
                requestService.updateRequest(id, req, principal.getUser().getId())
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, String>>> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        requestService.cancelRequest(id, principal.getUser().getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "Request cancelled successfully")));
    }
}

