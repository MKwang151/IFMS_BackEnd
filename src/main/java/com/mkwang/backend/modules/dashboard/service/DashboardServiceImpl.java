package com.mkwang.backend.modules.dashboard.service;

import com.mkwang.backend.modules.accounting.entity.PayrollPeriod;
import com.mkwang.backend.modules.accounting.service.PayrollManagementService;
import com.mkwang.backend.modules.audit.dto.response.AuditLogResponse;
import com.mkwang.backend.modules.audit.service.AuditLogService;
import com.mkwang.backend.modules.dashboard.dto.response.AccountantDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.AdminDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.CfoDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.ManagerDashboardResponse;
import com.mkwang.backend.modules.organization.service.DepartmentService;
import com.mkwang.backend.modules.project.entity.ProjectStatus;
import com.mkwang.backend.modules.project.service.ManagerProjectService;
import com.mkwang.backend.modules.request.dto.response.CfoDeptTopupItemResponse;
import com.mkwang.backend.modules.request.service.RequestService;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.service.UserService;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import com.mkwang.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final UserService userService;
    private final ManagerProjectService managerProjectService;
    private final RequestService requestService;
    private final WalletService walletService;
    private final PayrollManagementService payrollManagementService;
    private final AuditLogService auditLogService;
    private final DepartmentService departmentService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_VIEW_DEPT')")
    public ManagerDashboardResponse getManagerDashboard(Long managerId) {
        User manager = userService.getUserById(managerId);
        Long deptId = manager.getDepartment().getId();

        BigDecimal[] budgetSnapshot = managerProjectService.getDeptBudgetSnapshot(managerId);
        BigDecimal totalQuota     = budgetSnapshot[0];
        BigDecimal totalAvailable = budgetSnapshot[1];
        BigDecimal totalSpent     = totalQuota.subtract(totalAvailable);

        Map<ProjectStatus, Long> statusCounts = managerProjectService.getDeptProjectStatusCounts(managerId);

        long pendingApprovals = requestService.countDeptPendingProjectTopup(deptId);

        BigDecimal totalDebt       = requestService.sumDeptOutstandingAdvanceDebt(deptId);
        long employeesWithDebt     = requestService.countDeptEmployeesWithDebt(deptId);

        return ManagerDashboardResponse.builder()
                .departmentBudget(ManagerDashboardResponse.DepartmentBudget.builder()
                        .totalProjectQuota(totalQuota)
                        .totalAvailableBalance(totalAvailable)
                        .totalSpent(totalSpent)
                        .build())
                .projectStatusSummary(ManagerDashboardResponse.ProjectStatusSummary.builder()
                        .active(statusCounts.getOrDefault(ProjectStatus.ACTIVE, 0L))
                        .planning(statusCounts.getOrDefault(ProjectStatus.PLANNING, 0L))
                        .paused(statusCounts.getOrDefault(ProjectStatus.PAUSED, 0L))
                        .closed(statusCounts.getOrDefault(ProjectStatus.CLOSED, 0L))
                        .build())
                .pendingApprovalsCount(pendingApprovals)
                .teamDebtSummary(ManagerDashboardResponse.TeamDebtSummary.builder()
                        .totalDebt(totalDebt)
                        .employeesWithDebt(employeesWithDebt)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('PAYROLL_MANAGE')")
    public AccountantDashboardResponse getAccountantDashboard() {
        BigDecimal fundBalance = walletService.getWallet(WalletOwnerType.COMPANY_FUND, 1L).getBalance();

        long pendingDisbursements = requestService.countPendingDisbursements();

        LocalDate now = LocalDate.now();
        BigDecimal monthlyInflow  = walletService.getCompanyFundMonthlyInflow(now.getYear(), now.getMonthValue());
        BigDecimal monthlyOutflow = walletService.getCompanyFundMonthlyOutflow(now.getYear(), now.getMonthValue());

        Optional<PayrollPeriod> latestPeriod = payrollManagementService.getLatestPayrollPeriod();
        AccountantDashboardResponse.PayrollStatusSnapshot payrollSnapshot = latestPeriod
                .map(p -> AccountantDashboardResponse.PayrollStatusSnapshot.builder()
                        .latestPeriod(p.getName())
                        .status(p.getStatus())
                        .build())
                .orElse(null);

        return AccountantDashboardResponse.builder()
                .systemFundBalance(fundBalance)
                .pendingDisbursementsCount(pendingDisbursements)
                .monthlyInflow(monthlyInflow)
                .monthlyOutflow(monthlyOutflow)
                .payrollStatus(payrollSnapshot)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REQUEST_APPROVE_DEPT_TOPUP')")
    public CfoDashboardResponse getCfoDashboard() {
        BigDecimal fundBalance = walletService.getWallet(WalletOwnerType.COMPANY_FUND, 1L).getBalance();

        long pendingApprovals = requestService.countPendingDeptTopup();

        LocalDate now = LocalDate.now();
        BigDecimal monthlyApproved = requestService.sumMonthlyApprovedDeptTopup(now.getYear(), now.getMonthValue());
        long monthlyRejected       = requestService.countMonthlyRejectedDeptTopup(now.getYear(), now.getMonthValue());

        List<CfoDeptTopupItemResponse> recent = requestService.getRecentDeptTopups(5);
        List<CfoDashboardResponse.RecentApprovalItem> recentApprovals = recent.stream()
                .map(item -> CfoDashboardResponse.RecentApprovalItem.builder()
                        .id(item.id())
                        .requestCode(item.requestCode())
                        .departmentName(item.departmentName())
                        .amount(item.amount())
                        .status(item.status())
                        .createdAt(item.createdAt())
                        .build())
                .toList();

        return CfoDashboardResponse.builder()
                .companyFundBalance(fundBalance)
                .pendingApprovalsCount(pendingApprovals)
                .monthlyApprovedAmount(monthlyApproved)
                .monthlyRejectedCount(monthlyRejected)
                .recentApprovals(recentApprovals)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('USER_VIEW_LIST')")
    public AdminDashboardResponse getAdminDashboard() {
        long totalUsers       = userService.countActiveUsers();
        long totalDepartments = departmentService.countDepartments();
        BigDecimal totalWalletBalance = walletService.sumBalancesByType(WalletOwnerType.USER);

        List<AdminDashboardResponse.RecentAuditEvent> recentEvents = auditLogService
                .getAuditLogs(null, null, null, null, null, 1, 5)
                .getItems()
                .stream()
                .map(log -> AdminDashboardResponse.RecentAuditEvent.builder()
                        .id(log.id())
                        .actorName(log.actorName())
                        .action(log.action())
                        .entityName(log.entityName())
                        .createdAt(log.createdAt())
                        .build())
                .toList();

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalDepartments(totalDepartments)
                .totalWalletBalance(totalWalletBalance)
                .recentAuditEvents(recentEvents)
                .build();
    }
}
