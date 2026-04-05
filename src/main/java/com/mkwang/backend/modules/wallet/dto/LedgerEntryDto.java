package com.mkwang.backend.modules.wallet.dto;

import com.mkwang.backend.modules.wallet.entity.TransactionDirection;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class LedgerEntryDto {

    private Long id;
    private String transactionCode;
    private TransactionDirection direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;
}
