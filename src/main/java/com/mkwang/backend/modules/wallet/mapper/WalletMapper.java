package com.mkwang.backend.modules.wallet.mapper;

import com.mkwang.backend.modules.wallet.dto.response.LedgerEntryResponse;
import com.mkwang.backend.modules.wallet.dto.response.TransactionResponse;
import com.mkwang.backend.modules.wallet.dto.response.WalletResponse;
import com.mkwang.backend.modules.wallet.entity.LedgerEntry;
import com.mkwang.backend.modules.wallet.entity.Transaction;
import com.mkwang.backend.modules.wallet.entity.Wallet;
import org.springframework.stereotype.Component;

@Component
public class WalletMapper {

    public WalletResponse toDto(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .ownerType(wallet.getOwnerType())
                .ownerId(wallet.getOwnerId())
                .balance(wallet.getBalance())
                .lockedBalance(wallet.getLockedBalance())
                .availableBalance(wallet.getAvailableBalance())
                .build();
    }

    public TransactionResponse toDto(Transaction txn) {
        return TransactionResponse.builder()
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

    public LedgerEntryResponse toDto(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .transactionCode(entry.getTransaction().getTransactionCode())
                .direction(entry.getDirection())
                .amount(entry.getAmount())
                .balanceAfter(entry.getBalanceAfter())
                .createdAt(entry.getCreatedAt())
                .build();
    }
}
