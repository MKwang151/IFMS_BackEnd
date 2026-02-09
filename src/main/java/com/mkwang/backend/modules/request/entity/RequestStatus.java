package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing the status of a request in the approval workflow.
 */
public enum RequestStatus {
  PENDING_MANAGER, // Chờ Manager duyệt
  PENDING_ADMIN, // Chờ Admin duyệt (Leo thang)
  APPROVED, // Đã duyệt, chờ chi tiền
  PAID, // Đã chi tiền
  REJECTED, // Bị từ chối
  CANCELLED // Người tạo tự hủy
}
