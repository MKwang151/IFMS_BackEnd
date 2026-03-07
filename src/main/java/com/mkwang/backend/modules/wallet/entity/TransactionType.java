package com.mkwang.backend.modules.wallet.entity;

/**
 * Enum representing the type of wallet transaction.
 */
public enum TransactionType {
  DEPOSIT,                    // Nạp tiền vào ví
  WITHDRAW,                   // Rút tiền từ ví
  REQUEST_PAYMENT,            // Giải ngân cho Request (Project Fund → Ví NV)
  PAYSLIP_PAYMENT,            // Chi lương (System → Ví NV)
  SYSTEM_ADJUSTMENT,          // Điều chỉnh hệ thống
  DEPT_QUOTA_ALLOCATION,      // Admin cấp vốn Phòng ban (System Fund → Dept Fund)
  PROJECT_QUOTA_ALLOCATION    // Manager cấp vốn Dự án (Dept Fund → Project Fund)
}
