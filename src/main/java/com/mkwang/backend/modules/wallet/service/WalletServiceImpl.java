package com.mkwang.backend.modules.wallet.service;

import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeGenerator;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeType;
import com.mkwang.backend.modules.wallet.dto.LedgerEntryDto;
import com.mkwang.backend.modules.wallet.dto.TransactionDto;
import com.mkwang.backend.modules.wallet.dto.WalletDto;
import com.mkwang.backend.modules.wallet.entity.*;
import com.mkwang.backend.modules.wallet.mapper.WalletMapper;
import com.mkwang.backend.modules.wallet.repository.LedgerEntryRepository;
import com.mkwang.backend.modules.wallet.repository.TransactionRepository;
import com.mkwang.backend.modules.wallet.repository.WalletRepository;
import com.mkwang.backend.modules.wallet.service.locking.LockedWalletPair;
import com.mkwang.backend.modules.wallet.service.locking.WalletKey;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final BusinessCodeGenerator codeGenerator;
    private final WalletMapper walletMapper;

    // ══════════════════════════════════════════════════════════════════
    //  WRITE OPERATIONS
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public Transaction transfer(WalletOwnerType fromType, Long fromId,
                                WalletOwnerType toType, Long toId,
                                BigDecimal amount, TransactionType txnType,
                                ReferenceType refType, Long refId,
                                String description) {
        validateAmount(amount);

        LockedWalletPair pair = lockPairDeterministically(fromType, fromId, toType, toId);
        Wallet source = pair.source();
        Wallet dest = pair.dest();

        source.debit(amount);
        dest.credit(amount);

        Transaction txn = buildTransaction(amount, txnType, refType, refId, description);
        txn.getEntries().add(LedgerEntry.debit(txn, source, amount, source.getBalance()));
        txn.getEntries().add(LedgerEntry.credit(txn, dest, amount, dest.getBalance()));

        walletRepository.save(source);
        walletRepository.save(dest);
        return transactionRepository.save(txn);
    }

    @Override
    @Transactional
    public void lockFunds(WalletOwnerType ownerType, Long ownerId, BigDecimal amount) {
        validateAmount(amount);
        Wallet wallet = getWalletForUpdate(ownerType, ownerId);
        wallet.lock(amount);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public void unlockFunds(WalletOwnerType ownerType, Long ownerId, BigDecimal amount) {
        validateAmount(amount);
        Wallet wallet = getWalletForUpdate(ownerType, ownerId);
        wallet.unlock(amount);
        walletRepository.save(wallet);
    }

    @Override
    @Transactional
    public Transaction settleAndTransfer(WalletOwnerType fromType, Long fromId,
                                         WalletOwnerType toType, Long toId,
                                         BigDecimal amount, TransactionType txnType,
                                         ReferenceType refType, Long refId,
                                         String description) {
        validateAmount(amount);

        LockedWalletPair pair = lockPairDeterministically(fromType, fromId, toType, toId);
        Wallet source = pair.source();
        Wallet dest = pair.dest();

        source.settle(amount);
        dest.credit(amount);

        Transaction txn = buildTransaction(amount, txnType, refType, refId, description);
        txn.getEntries().add(LedgerEntry.debit(txn, source, amount, source.getBalance()));
        txn.getEntries().add(LedgerEntry.credit(txn, dest, amount, dest.getBalance()));

        walletRepository.save(source);
        walletRepository.save(dest);
        return transactionRepository.save(txn);
    }

    @Override
    @Transactional
    public Transaction reversal(Long originalTransactionId, String reason) {
        Transaction original = transactionRepository.findById(originalTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction", "id", originalTransactionId));

        if (original.getStatus() != TransactionStatus.SUCCESS) {
            throw new BadRequestException("Only SUCCESS transactions can be reversed");
        }

        List<LedgerEntry> originalEntries = ledgerEntryRepository.findByTransactionId(originalTransactionId);
        if (originalEntries.size() != 2) {
            throw new BadRequestException("Transaction does not have exactly 2 ledger entries");
        }

        LedgerEntry debitEntry = originalEntries.stream()
                .filter(e -> e.getDirection() == TransactionDirection.DEBIT)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Original transaction missing DEBIT entry"));
        LedgerEntry creditEntry = originalEntries.stream()
                .filter(e -> e.getDirection() == TransactionDirection.CREDIT)
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Original transaction missing CREDIT entry"));

        BigDecimal amount = original.getAmount();

        LockedWalletPair pair = lockPairDeterministically(
                debitEntry.getWallet().getOwnerType(), debitEntry.getWallet().getOwnerId(),
                creditEntry.getWallet().getOwnerType(), creditEntry.getWallet().getOwnerId()
        );
        Wallet originalSource = pair.source();
        Wallet originalDest = pair.dest();

        originalDest.debit(amount);
        originalSource.credit(amount);

        String desc = "REVERSAL of " + original.getTransactionCode()
                + (reason != null ? " - " + reason : "");

        Transaction reversalTxn = buildTransaction(amount, TransactionType.REVERSAL,
                original.getReferenceType(), original.getReferenceId(), desc);

        reversalTxn.getEntries().add(LedgerEntry.debit(reversalTxn, originalDest, amount, originalDest.getBalance()));
        reversalTxn.getEntries().add(LedgerEntry.credit(reversalTxn, originalSource, amount, originalSource.getBalance()));

        walletRepository.save(originalSource);
        walletRepository.save(originalDest);
        return transactionRepository.save(reversalTxn);
    }

    // ══════════════════════════════════════════════════════════════════
    //  LIFECYCLE
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public WalletDto createWallet(WalletOwnerType ownerType, Long ownerId) {
        if (walletRepository.existsByOwnerTypeAndOwnerId(ownerType, ownerId)) {
            throw new BadRequestException("Wallet already exists for " + ownerType + ":" + ownerId);
        }

        Wallet wallet = Wallet.builder()
                .ownerType(ownerType)
                .ownerId(ownerId)
                .build();
        return walletMapper.toDto(walletRepository.save(wallet));
    }

    // ══════════════════════════════════════════════════════════════════
    //  READ OPERATIONS
    // ══════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public WalletDto getWallet(WalletOwnerType ownerType, Long ownerId) {
        Wallet wallet = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "owner",
                        ownerType + ":" + ownerId));
        return walletMapper.toDto(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LedgerEntryDto> getLedgerHistory(WalletOwnerType ownerType, Long ownerId,
                                                  LocalDate from, LocalDate to,
                                                  Pageable pageable) {
        Wallet wallet = walletRepository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "owner",
                        ownerType + ":" + ownerId));

        Page<LedgerEntry> entries;
        if (from != null && to != null) {
            entries = ledgerEntryRepository.findByWalletIdAndDateRange(
                    wallet.getId(),
                    from.atStartOfDay(),
                    to.atTime(LocalTime.MAX),
                    pageable);
        } else {
            entries = ledgerEntryRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable);
        }

        return entries.map(walletMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionDto> getTransactionsByReference(ReferenceType refType, Long refId) {
        return transactionRepository
                .findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(refType, refId)
                .stream()
                .map(walletMapper::toDto)
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════
    //  PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════

    private Wallet getWalletForUpdate(WalletOwnerType ownerType, Long ownerId) {
        return walletRepository.findByOwnerTypeAndOwnerIdForUpdate(ownerType, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", "owner",
                        ownerType + ":" + ownerId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
    }

    private Transaction buildTransaction(BigDecimal amount, TransactionType type,
                                          ReferenceType refType, Long refId,
                                          String description) {
        return Transaction.builder()
                .transactionCode(codeGenerator.generate(BusinessCodeType.TRANSACTION))
                .amount(amount)
                .type(type)
                .status(TransactionStatus.SUCCESS)
                .gatewayProvider(PaymentProvider.INTERNAL)
                .referenceType(refType)
                .referenceId(refId)
                .description(description)
                .build();
    }

    private LockedWalletPair lockPairDeterministically(
            WalletOwnerType fromType, Long fromId,
            WalletOwnerType toType, Long toId
    ) {
        WalletKey sourceKey = new WalletKey(fromType, fromId);
        WalletKey destKey = new WalletKey(toType, toId);

        WalletKey first = sourceKey.compareTo(destKey) <= 0 ? sourceKey : destKey;
        WalletKey second = sourceKey.compareTo(destKey) <= 0 ? destKey : sourceKey;

        Wallet firstLocked = getWalletForUpdate(first.ownerType(), first.ownerId());
        Wallet secondLocked = getWalletForUpdate(second.ownerType(), second.ownerId());

        Wallet source = sourceKey.equals(first) ? firstLocked : secondLocked;
        Wallet dest = sourceKey.equals(first) ? secondLocked : firstLocked;
        return new LockedWalletPair(source, dest);
    }
}
