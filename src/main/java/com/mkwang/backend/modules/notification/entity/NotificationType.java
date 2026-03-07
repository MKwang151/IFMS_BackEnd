package com.mkwang.backend.modules.notification.entity;

/**
 * Enum representing the type of notification.
 * Matches Database.md Module 9.
 */
public enum NotificationType {
  SYSTEM,                       // Thông báo hệ thống chung
  REQUEST_APPROVED,             // Yêu cầu đã được duyệt
  REQUEST_REJECTED,             // Yêu cầu bị từ chối
  REQUEST_PENDING_ACCOUNTANT,   // Yêu cầu chờ Kế toán giải ngân
  SALARY_PAID,                  // Lương đã được thanh toán
  QUOTA_APPROVED,               // Cấp vốn phòng ban đã duyệt
  PROJECT_TOPUP_APPROVED,       // Cấp vốn dự án đã duyệt
  WARN                          // Cảnh báo (VD: PIN sắp bị khóa)
}
