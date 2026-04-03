package com.mkwang.backend.modules.request.entity;

/**
 * Status lifecycle of a request in the approval workflow.
 *
 * Flow 1 (ADVANCE / EXPENSE / REIMBURSE):
 *   PENDING_APPROVAL → PENDING_ACCOUNTANT → PAID
 *                    ↘ REJECTED
 *
 * Flow 2 (PROJECT_TOPUP):
 *   PENDING_APPROVAL → PAID  (auto on Manager approval)
 *                    ↘ REJECTED
 *
 * Flow 3 (DEPARTMENT_TOPUP):
 *   PENDING_APPROVAL → PAID  (auto on CFO approval)
 *                    ↘ REJECTED
 *
 * Any flow (only while PENDING_APPROVAL):
 *   PENDING_APPROVAL → CANCELLED
 *
 * NO escalation — each flow has exactly one approval level.
 * PENDING_APPROVAL is unified — the approver is inferred from request.type.
 */
public enum RequestStatus {
  PENDING_APPROVAL,   // Chờ duyệt nghiệp vụ (TL / Manager / CFO tùy type)
  PENDING_ACCOUNTANT, // Flow 1 only — chờ Accountant kiểm tra chứng từ & giải ngân
  APPROVED,           // Đã duyệt nghiệp vụ — Flow 2 & 3 tự chuyển PAID ngay sau đây
  PAID,               // Đã giải ngân / đã cấp vốn hoàn tất
  REJECTED,           // Bị từ chối bởi cấp duyệt hoặc Accountant
  CANCELLED           // Người tạo tự hủy (chỉ được phép khi đang PENDING_APPROVAL)
}
