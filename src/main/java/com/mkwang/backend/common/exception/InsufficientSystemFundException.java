package com.mkwang.backend.common.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class InsufficientSystemFundException extends BaseException {

    public InsufficientSystemFundException(BigDecimal requestedAmount, BigDecimal availableAmount) {
        super(
                "Insufficient system fund. Requested: " + requestedAmount + ", Available: " + availableAmount,
                HttpStatus.BAD_REQUEST,
                "INSUFFICIENT_SYSTEM_FUND"
        );
    }
}

