package com.mkwang.backend.modules.wallet.dto;

import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class WalletDto {

    private Long id;
    private WalletOwnerType ownerType;
    private Long ownerId;
    private BigDecimal balance;
    private BigDecimal lockedBalance;
    private BigDecimal availableBalance;
}
