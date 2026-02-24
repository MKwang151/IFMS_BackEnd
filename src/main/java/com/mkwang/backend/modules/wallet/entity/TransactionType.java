package com.mkwang.backend.modules.wallet.entity;

/**
 * Enum representing the type of wallet transaction.
 */
public enum TransactionType {
  DEPOSIT,
  WITHDRAW,
  REQUEST_PAYMENT,
  PAYSLIP_PAYMENT,
  SYSTEM_ADJUSTMENT
}
