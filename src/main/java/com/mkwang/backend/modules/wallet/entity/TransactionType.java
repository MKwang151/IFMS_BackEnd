package com.mkwang.backend.modules.wallet.entity;

/**
 * Enum representing the type of wallet transaction.
 */
public enum TransactionType {
  DEPOSIT, // Nạp tiền vào ví
  WITHDRAW, // Rút tiền về ngân hàng
  EXPENSE, // Chi tiêu (Request được duyệt)
  SALARY, // Nhận lương
  DEBT // Phát sinh công nợ (Tạm ứng)
}
