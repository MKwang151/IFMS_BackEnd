package com.mkwang.backend.modules.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Danh sách toàn bộ Quyền hạn (Permissions) trong hệ thống.
 * Sử dụng cho Dynamic RBAC (Role-Based Access Control).
 */
@Getter
@RequiredArgsConstructor
public enum Permission {

  // ================================================================
  // 1. NHÓM IAM & BẢO MẬT (Identity & Security)
  // ================================================================

  // --- Cá nhân (Dành cho tất cả User) ---
  USER_PROFILE_VIEW("Xem hồ sơ cá nhân"),
  USER_PROFILE_UPDATE("Cập nhật hồ sơ (Avatar, SĐT, Địa chỉ)"),
  USER_PIN_UPDATE("Thiết lập hoặc Đổi mã PIN giao dịch"),
  NOTIFICATION_VIEW("Xem thông báo biến động số dư/lương"),

  // --- Quản trị (Dành cho Admin) ---
  USER_VIEW_LIST("Xem danh sách nhân viên toàn hệ thống"),
  USER_CREATE("Cấp tài khoản mới (Onboarding)"),
  USER_UPDATE("Chỉnh sửa thông tin & Điều chuyển nhân sự"),
  USER_LOCK("Khóa/Mở khóa tài khoản"),

  // --- Phân quyền Động (Quan trọng) ---
  ROLE_MANAGE("Quản lý Vai trò & Phân quyền (Tạo Role, Gán quyền)"),

  // ================================================================
  // 2. NHÓM VÍ ĐIỆN TỬ (Core Wallet)
  // ================================================================

  // --- Cá nhân (Dành cho tất cả User) ---
  WALLET_VIEW_SELF("Xem Dashboard ví cá nhân (Số dư, Tiền treo, Dư nợ)"),
  WALLET_DEPOSIT("Nạp tiền vào ví (Nhập tiền + PIN/QR)"),
  WALLET_WITHDRAW("Rút tiền về ngân hàng (Nhập TK + PIN)"),
  WALLET_TRANSACTION_VIEW("Xem lịch sử giao dịch cá nhân"),

  // --- Rủi ro (Dành cho Accountant/Admin) ---
  TRANSACTION_APPROVE_WITHDRAW("Duyệt các lệnh rút tiền lớn hoặc bị treo"),

  // ================================================================
  // 3. NHÓM DỰ ÁN & TIẾN ĐỘ (Project Lifecycle)
  // ================================================================

  // --- Employee ---
  PROJECT_VIEW_ACTIVE("Xem danh sách Đề án/Phase đang Active (Để tạo Request)"),

  // --- Manager ---
  PROJECT_CREATE("Khởi tạo đề án mới"),
  PROJECT_UPDATE("Cập nhật thông tin chung dự án (Tên, Deadline)"),
  PROJECT_PHASE_MANAGE("Quản lý Phase (Tạo mới, Cấp vốn Phase, Đóng/Mở Phase)"),
  PROJECT_MEMBER_MANAGE("Thêm hoặc Xóa thành viên khỏi dự án"),
  PROJECT_STATUS_MANAGE("Tạm dừng (Pause) chặn chi tiêu hoặc Đóng (Close) đề án"),

  // --- Admin/Accountant ---
  PROJECT_VIEW_ALL("Xem danh sách tất cả dự án (Để Audit/Chi tiền)"),

  // ================================================================
  // 4. NHÓM QUẢN LÝ YÊU CẦU (Request Flow)
  // ================================================================

  // --- Employee (Tạo) ---
  REQUEST_CREATE("Tạo yêu cầu (Chi/Ứng/Hoàn ứng) & Upload chứng từ"),
  REQUEST_VIEW_SELF("Xem danh sách & trạng thái yêu cầu của chính mình"),

  // --- Manager (Duyệt Cấp 1) ---
  REQUEST_VIEW_DEPT("Xem các yêu cầu thuộc phòng ban mình quản lý"),
  REQUEST_APPROVE_TIER1("Duyệt yêu cầu cấp 1 (Trong hạn mức Manager)"),
  REQUEST_REJECT("Từ chối yêu cầu (Bắt buộc nhập lý do)"),

  // --- Admin (Duyệt Cấp 2 - Leo thang) ---
  REQUEST_VIEW_ALL("Xem tất cả yêu cầu toàn hệ thống"),
  REQUEST_APPROVE_TIER2("Duyệt yêu cầu cấp 2 (Vượt hạn mức Manager/Leo thang)"),

  // --- Accountant (Chi tiền) ---
  REQUEST_VIEW_APPROVED("Xem danh sách yêu cầu ĐÃ DUYỆT (Chờ giải ngân)"),
  REQUEST_PAYOUT("Thực hiện Thanh toán/Giải ngân (Trừ quỹ -> Cộng ví NV)"),

  // ================================================================
  // 5. NHÓM LƯƠNG & KẾ TOÁN (Payroll & Accounting)
  // ================================================================

  // --- Employee ---
  PAYROLL_VIEW_SELF("Xem danh sách kỳ lương & Chi tiết phiếu lương"),
  PAYROLL_DOWNLOAD("Tải phiếu lương cá nhân (PDF)"),

  // --- Accountant ---
  PAYROLL_MANAGE("Quản lý kỳ lương (Tạo mới, Upload Excel, Validate)"),
  PAYROLL_EXECUTE("Chốt & Chi lương hàng loạt (Kèm Auto-netting trừ nợ)"),

  SYSTEM_FUND_VIEW("Xem số dư Quỹ hệ thống (Mock Bank)"),
  SYSTEM_FUND_TOPUP("Nạp tiền vào Quỹ hệ thống (Top-up)"),

  // ================================================================
  // 6. NHÓM TỔ CHỨC & CẤU HÌNH (Org & Config)
  // ================================================================

  // --- Manager ---
  DEPT_VIEW_DASHBOARD("Xem Dashboard phòng ban (Ngân sách, Báo cáo chi tiêu)"),

  // --- Admin ---
  DEPT_MANAGE("Quản lý danh sách phòng ban (Tạo, Sửa tên, Mã phòng)"),
  DEPT_BUDGET_ALLOCATE("Cấp vốn tổng (Top-up Quota) cho Manager phân bổ"),

  SYSTEM_CONFIG_MANAGE("Cấu hình tham số hệ thống (Hạn mức Rút/Duyệt, Whitelist)"),
  DASHBOARD_VIEW_GLOBAL("Xem Dashboard tổng quan (Dòng tiền, Dư nợ toàn cty)"),
  AUDIT_LOG_VIEW("Xem nhật ký hệ thống (Audit Logs)");

  private final String description;
}