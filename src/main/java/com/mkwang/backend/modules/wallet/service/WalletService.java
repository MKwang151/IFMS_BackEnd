package com.mkwang.backend.modules.wallet.service;

import com.mkwang.backend.modules.wallet.dto.LedgerEntryDto;
import com.mkwang.backend.modules.wallet.dto.TransactionDto;
import com.mkwang.backend.modules.wallet.dto.WalletDto;
import com.mkwang.backend.modules.wallet.entity.ReferenceType;
import com.mkwang.backend.modules.wallet.entity.Transaction;
import com.mkwang.backend.modules.wallet.entity.TransactionType;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Core wallet service — handles all fund operations in the 4-tier wallet system.
 *
 * Write operations (mutate balance, create Transaction + LedgerEntry):
 *   - transfer:           Direct fund movement (SYSTEM_TOPUP, DEPT/PROJECT allocation, PAYSLIP, ADVANCE_RETURN)
 *   - lockFunds:          Reserve funds for pending payout (TL approves request → lock Project wallet)
 *   - unlockFunds:        Release reservation (request rejected/cancelled after lock)
 *   - settleAndTransfer:  Settle locked amount + transfer (Accountant executes ADVANCE/EXPENSE payout)
 *   - reversal:           Reverse a completed transaction (cancel after PAID)
 *
 * Read operations:
 *   - getWallet:          Balance snapshot for a wallet owner
 *   - getLedgerHistory:   Paginated ledger entries (sao kê)
 *   - getTransactionsByReference: All transactions linked to a business entity
 *
 * Lifecycle:
 *   - createWallet:       Create wallet for new User/Department/Project/SystemFund
 */
public interface WalletService {

    // ── Write Operations ─────────────────────────────────────────────

    /**
     * Transfer funds directly between two wallets (no prior lock required).
     * Creates 1 Transaction + 2 LedgerEntries (DEBIT source, CREDIT destination).
     *
     * Used for: SYSTEM_TOPUP, DEPT_QUOTA_ALLOCATION, PROJECT_QUOTA_ALLOCATION,
     *           PAYSLIP_PAYMENT, ADVANCE_RETURN
     */
    Transaction transfer(WalletOwnerType fromType, Long fromId,
                         WalletOwnerType toType, Long toId,
                         BigDecimal amount, TransactionType txnType,
                         ReferenceType refType, Long refId,
                         String description);

    /**
     * Lock (reserve) funds in a wallet. Balance stays the same, lockedBalance increases.
     * No Transaction created — this is just a reservation.
     *
     * Used when: TEAM_LEADER approves ADVANCE/EXPENSE/REIMBURSE → lock Project wallet.
     */
    void lockFunds(WalletOwnerType ownerType, Long ownerId, BigDecimal amount);

    /**
     * Unlock (release) a previously locked amount. No Transaction created.
     *
     * Used when: Request rejected or cancelled after approval (funds were locked but not disbursed).
     */
    void unlockFunds(WalletOwnerType ownerType, Long ownerId, BigDecimal amount);

    /**
     * Settle a locked amount and transfer to destination wallet.
     * Source wallet: settle(amount) → locked decreases, balance decreases.
     * Destination wallet: credit(amount) → balance increases.
     * Creates 1 Transaction + 2 LedgerEntries.
     *
     * Used when: ACCOUNTANT executes payment for ADVANCE/EXPENSE/REIMBURSE.
     */
    Transaction settleAndTransfer(WalletOwnerType fromType, Long fromId,
                                  WalletOwnerType toType, Long toId,
                                  BigDecimal amount, TransactionType txnType,
                                  ReferenceType refType, Long refId,
                                  String description);

    /**
     * Reverse a completed transaction. Creates a new REVERSAL transaction
     * with opposite-direction entries. Original transaction is NOT modified (immutable).
     *
     * Used when: Request cancelled after PAID (money already in User wallet).
     */
    Transaction reversal(Long originalTransactionId, String reason);

    // ── Lifecycle ────────────────────────────────────────────────────

    /**
     * Create a new wallet for a given owner. Balance starts at 0.
     */
    WalletDto createWallet(WalletOwnerType ownerType, Long ownerId);

    // ── Read Operations ──────────────────────────────────────────────

    /**
     * Get wallet balance snapshot.
     */
    WalletDto getWallet(WalletOwnerType ownerType, Long ownerId);

    /**
     * Paginated ledger history (sao kê) for a wallet, optionally filtered by date range.
     */
    Page<LedgerEntryDto> getLedgerHistory(WalletOwnerType ownerType, Long ownerId,
                                          LocalDate from, LocalDate to,
                                          Pageable pageable);

    /**
     * All transactions linked to a business entity (Request, Payslip, etc.)
     */
    List<TransactionDto> getTransactionsByReference(ReferenceType refType, Long refId);
}
