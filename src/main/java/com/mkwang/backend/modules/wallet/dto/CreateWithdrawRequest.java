package com.mkwang.backend.modules.wallet.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/withdrawals.
 * Bank info is read automatically from the user's UserProfile — not needed here.
 */
public record CreateWithdrawRequest(

    @NotNull(message = "Số tiền không được để trống")
    @DecimalMin(value = "10000", message = "Số tiền rút tối thiểu là 10,000 VNĐ")
    BigDecimal amount,

    String userNote   // optional
) {}
