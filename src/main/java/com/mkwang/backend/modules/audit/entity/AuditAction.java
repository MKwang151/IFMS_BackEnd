package com.mkwang.backend.modules.audit.entity;

/**
 * AuditAction enum - Classifies all auditable system actions.
 * Used in audit_logs table to identify what type of operation occurred.
 */
public enum AuditAction {

    // 1. User management
    USER_CREATED,
    USER_UPDATED,
    USER_LOCKED,
    USER_UNLOCKED,
    BANK_INFO_UPDATED, // Add this!

    // 2. Role & Permission management
    ROLE_ASSIGNED,
    ROLE_REVOKED,
    PERMISSION_GRANTED,
    PERMISSION_REVOKED,

    // 3. Department & Budget (Core Admin powers)
    DEPARTMENT_CREATED,
    DEPARTMENT_UPDATED,
    DEPARTMENT_DELETED,
    QUOTA_TOPUP,
    QUOTA_ADJUSTED,

    // 4. System & Fund Config
    CONFIG_UPDATED,
    SYSTEM_FUND_ADJUSTED,

    // 5. Security & Access (Crucial for defense)
    PIN_RESET,
    PIN_LOCKED,
    USER_LOGIN_SUCCESS,
    USER_LOGIN_FAILED,
    DATA_EXPORTED,

    // 6. Generic fallback
    MANUAL_ADJUSTMENT
}
