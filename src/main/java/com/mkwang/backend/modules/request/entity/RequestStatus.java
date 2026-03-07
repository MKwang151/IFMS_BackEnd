package com.mkwang.backend.modules.request.entity;

/**
 * Enum representing the status of a request in the approval workflow.
 *
 * NO ESCALATION — each flow has exactly ONE approval level.
 * PENDING_APPROVAL is a unified status — the approver is determined by request.type.
 */
public enum RequestStatus {
  PENDING_APPROVAL,     // Chờ DUY NHẤT 1 cấp duyệt (TL/Manager/Admin tùy type)
  PENDING_ACCOUNTANT,   // Chỉ Luồng 1 — chờ Kế toán kiểm tra chứng từ & giải ngân
  APPROVED,             // Đã duyệt nghiệp vụ (Luồng 2&3 auto → PAID)
  PAID,                 // Đã giải ngân / đã cấp vốn xong
  REJECTED,             // Bị từ chối bởi cấp duyệt hoặc Accountant
  CANCELLED             // Người tạo tự hủy (chỉ khi đang PENDING_APPROVAL)
}
