package com.mkwang.backend.modules.wallet.entity;

/**
 * Enum representing the status of a wallet transaction.
 */
public enum TransactionStatus {
  SUCCESS, // Giao dịch thành công
  PENDING, // Đang chờ xử lý (VD: Rút tiền lớn cần duyệt)
  FAILED // Giao dịch thất bại
}
