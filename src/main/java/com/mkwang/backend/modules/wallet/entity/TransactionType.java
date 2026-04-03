package com.mkwang.backend.modules.wallet.entity;

/**
 * Business type of a wallet transaction.
 *
 * Fund flow overview (4-tier architecture):
 *   External Bank   →  SystemFund      : SYSTEM_TOPUP
 *   SystemFund      →  Department      : DEPT_QUOTA_ALLOCATION
 *   Department      →  Project         : PROJECT_QUOTA_ALLOCATION
 *   Project         →  User            : REQUEST_PAYMENT  (advance/expense payout)
 *   SystemFund      →  User            : PAYSLIP_PAYMENT
 *   User            →  Project         : ADVANCE_RETURN   (remaining advance returned)
 *   User            ↔  External        : DEPOSIT / WITHDRAW
 *   Any             ←  Correction      : REVERSAL / SYSTEM_ADJUSTMENT
 */
public enum TransactionType {

  // ── Deposit / Withdraw (User ↔ External) ─────────────────────────
  DEPOSIT,                  // Nạp tiền vào ví cá nhân qua payment gateway
  WITHDRAW,                 // Rút tiền từ ví cá nhân ra ngân hàng

  // ── Internal fund allocation ──────────────────────────────────────
  SYSTEM_TOPUP,             // Accountant/CFO nạp tiền từ ngân hàng vào SystemFund
  DEPT_QUOTA_ALLOCATION,    // CFO cấp quota Phòng ban (SystemFund → Department)
  PROJECT_QUOTA_ALLOCATION, // Manager cấp vốn Dự án (Department → Project)

  // ── Disbursement ──────────────────────────────────────────────────
  REQUEST_PAYMENT,          // Accountant giải ngân đơn chi tiêu (Project → User)
  PAYSLIP_PAYMENT,          // Kế toán chi lương (SystemFund → User)

  // ── Advance settlement ────────────────────────────────────────────
  ADVANCE_RETURN,           // Nhân viên hoàn trả phần tạm ứng còn dư (User → Project)
                            // Phát sinh khi: hoàn tiền mặt thủ công hoặc cấn trừ lương

  // ── Correction ───────────────────────────────────────────────────
  REVERSAL,                 // Hoàn tiền khi đơn bị huỷ sau khi đã giải ngân (đảo bút toán)
  SYSTEM_ADJUSTMENT         // Điều chỉnh số dư thủ công bởi Admin/Accountant
}
