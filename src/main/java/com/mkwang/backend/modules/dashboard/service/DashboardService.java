package com.mkwang.backend.modules.dashboard.service;

import com.mkwang.backend.modules.dashboard.dto.response.AccountantDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.AdminDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.CfoDashboardResponse;
import com.mkwang.backend.modules.dashboard.dto.response.ManagerDashboardResponse;

public interface DashboardService {

    ManagerDashboardResponse getManagerDashboard(Long managerId);

    AccountantDashboardResponse getAccountantDashboard();

    CfoDashboardResponse getCfoDashboard();

    AdminDashboardResponse getAdminDashboard();
}
