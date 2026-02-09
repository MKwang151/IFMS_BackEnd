package com.mkwang.backend.modules.notification.entity;

/**
 * Enum representing the type of notification.
 */
public enum NotificationType {
  SYSTEM, // Thông báo hệ thống chung
  REQUEST_APPROVED, // Yêu cầu đã được duyệt
  REQUEST_REJECTED, // Yêu cầu bị từ chối
  SALARY_PAID, // Lương đã được thanh toán
  WALLET_UPDATED, // Biến động số dư ví
  WARN // Cảnh báo (VD: PIN sắp bị khóa)
}
