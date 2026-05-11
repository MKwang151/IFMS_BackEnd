package com.mkwang.backend.modules.dashboard.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.dashboard.dto.response.AccountantDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.ManagerDashboardResponse;
import com.mkwang.backend.modules.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Role-specific dashboard snapshots")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/manager")
    @Operation(
        summary = "Manager dashboard snapshot",
        description = "Returns aggregated department budget, project status summary, pending approvals count, and team debt summary for the authenticated manager."
    )
    public ResponseEntity<ApiResponse<ManagerDashboardResponse>> getManagerDashboard(
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getManagerDashboard(principal.getUser().getId())
        ));
    }

    @GetMapping("/accountant")
    @Operation(
        summary = "Accountant dashboard snapshot",
        description = "Returns company fund balance, pending disbursements count, monthly inflow/outflow, and latest payroll period status."
    )
    public ResponseEntity<ApiResponse<AccountantDashboardResponse>> getAccountantDashboard() {
        return ResponseEntity.ok(ApiResponse.success(
                dashboardService.getAccountantDashboard()
        ));
    }
}
