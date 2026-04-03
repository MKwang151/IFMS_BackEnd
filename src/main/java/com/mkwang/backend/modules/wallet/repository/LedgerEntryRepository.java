package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.LedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

  /**
   * Paginated transaction history for a wallet — used for statement display.
   */
  Page<LedgerEntry> findByWalletIdOrderByCreatedAtDesc(Long walletId, Pageable pageable);

  /**
   * All entries for a transaction — used to verify double-entry integrity.
   */
  List<LedgerEntry> findByTransactionId(Long transactionId);

  /**
   * Latest entry for a wallet — used to get current balance_after snapshot.
   */
  @Query("SELECT e FROM LedgerEntry e WHERE e.wallet.id = :walletId ORDER BY e.createdAt DESC LIMIT 1")
  java.util.Optional<LedgerEntry> findLatestByWalletId(@Param("walletId") Long walletId);
}
