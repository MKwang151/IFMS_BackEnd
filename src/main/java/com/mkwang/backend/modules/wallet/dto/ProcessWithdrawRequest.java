package com.mkwang.backend.modules.wallet.dto;

/**
 * Request body for PUT /api/v1/withdrawals/{id}/execute
 * and PUT /api/v1/withdrawals/{id}/reject.
 */
public record ProcessWithdrawRequest(
    String note   // accountant note — optional for execute, required for reject
) {}
