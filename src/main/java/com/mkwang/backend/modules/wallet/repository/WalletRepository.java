package com.mkwang.backend.modules.wallet.repository;

import com.mkwang.backend.modules.wallet.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for Wallet entity.
 */
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

  /**
   * Find wallet by user ID
   */
  Optional<Wallet> findByUserId(Long userId);

  /**
   * Check if wallet exists for a user
   */
  boolean existsByUserId(Long userId);
}
