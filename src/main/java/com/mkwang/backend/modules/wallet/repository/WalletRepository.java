package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.Wallet;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Wallet entity.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

  Optional<Wallet> findByOwnerTypeAndOwnerId(WalletOwnerType ownerType, Long ownerId);

  boolean existsByOwnerTypeAndOwnerId(WalletOwnerType ownerType, Long ownerId);

  /**
   * Pessimistic write lock — use this before any balance mutation.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT w FROM Wallet w WHERE w.ownerType = :ownerType AND w.ownerId = :ownerId")
  Optional<Wallet> findByOwnerTypeAndOwnerIdForUpdate(
      @Param("ownerType") WalletOwnerType ownerType,
      @Param("ownerId") Long ownerId
  );
}
