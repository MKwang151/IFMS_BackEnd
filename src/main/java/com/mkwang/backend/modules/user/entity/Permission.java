package com.mkwang.backend.modules.user.entity;

/**
 * Permission enum - Defines all available permissions in the system.
 * Managed by developers (hardcoded) to stay in sync with FE/BE code.
 */
public enum Permission {
  // --- 1. QUYỀN VÍ & GIAO DỊCH ---
  WALLET_VIEW_SELF("Xem ví của chính mình"),
  WALLET_WITHDRAW("Thực hiện rút tiền"),
  WALLET_DEPOSIT("Thực hiện nạp tiền (trả nợ)"),

  // --- 2. QUYỀN QUẢN LÝ YÊU CẦU (REQUEST) ---
  REQUEST_CREATE("Tạo yêu cầu chi tiêu/tạm ứng"),
  REQUEST_VIEW_ALL("Xem tất cả yêu cầu (Dành cho Manager/Admin)"),
  REQUEST_APPROVE_LEVEL_1("Duyệt cấp 1 (Manager - Hạn mức thấp)"),
  REQUEST_APPROVE_LEVEL_2("Duyệt cấp 2 (Admin - Hạn mức cao)"),

  // --- 3. QUYỀN KẾ TOÁN & QUỸ ---
  ACCOUNTING_PAYOUT("Thực hiện giải ngân/Chi tiền"),
  ACCOUNTING_PAYROLL("Chạy bảng lương"),
  SYSTEM_FUND_VIEW("Xem số dư quỹ công ty"),

  // --- 4. QUYỀN QUẢN TRỊ (ADMIN) ---
  USER_MANAGE("Tạo/Sửa/Khóa nhân viên"),
  DEPARTMENT_MANAGE("Quản lý phòng ban & Ngân sách"),
  RISK_CONFIG_MANAGE("Cấu hình hạn mức rủi ro");

  private final String description;

  Permission(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
