package com.mkwang.backend.modules.wallet.mapper;

import com.mkwang.backend.modules.wallet.dto.LedgerEntryDto;
import com.mkwang.backend.modules.wallet.dto.TransactionDto;
import com.mkwang.backend.modules.wallet.dto.WalletDto;
import com.mkwang.backend.modules.wallet.entity.LedgerEntry;
import com.mkwang.backend.modules.wallet.entity.Transaction;
import com.mkwang.backend.modules.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletDto toDto(Wallet wallet) {
        return WalletDto.builder()
                .id(wallet.getId())
                .ownerType(wallet.getOwnerType())
                .ownerId(wallet.getOwnerId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .availableBalance(wallet.getAvailableBalance())
                .build();
    }

    public TransactionDto toDto(Transaction txn) {
        return TransactionDto.builder()
                .id(txn.getId())
                .transactionCode(txn.getTransactionCode())
                .amount(txn.getAmount())
                .type(txn.getType())
                .status(txn.getStatus())
                .referenceType(txn.getReferenceType())
                .referenceId(txn.getReferenceId())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }

    public LedgerEntryDto toDto(LedgerEntry entry) {
        return LedgerEntryDto.builder()
                .id(entry.getId())
                .transactionCode(entry.getTransaction().getTransactionCode())
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
