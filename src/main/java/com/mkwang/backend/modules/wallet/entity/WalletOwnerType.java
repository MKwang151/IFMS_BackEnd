package com.mkwang.backend.modules.wallet.entity;

/**
 * Identifies the type of entity that owns a wallet.
 * Supports the unified wallet model across all financial actors.
 */
public enum WalletOwnerType {
  USER,
  DEPARTMENT,
  PROJECT,
  SYSTEM_FUND
}
