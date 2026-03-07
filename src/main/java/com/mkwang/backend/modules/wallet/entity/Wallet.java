package com.mkwang.backend.modules.wallet.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Wallet entity - Core wallet for each user.
 * Concurrency Strategy:
 *   - Primary: Pessimistic Lock via findByIdForUpdate() in Repository
 *   - Safety Net: @Version (Optimistic Locking) as fallback for edge cases
 */
@Entity
@Table(name = "wallets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", unique = true, nullable = false)
  private User user;

  @Column(name = "balance", precision = 19, scale = 2, nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
  @Builder.Default
  private BigDecimal balance = BigDecimal.ZERO;

  @Column(name = "pending_balance", precision = 19, scale = 2, nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
  @Builder.Default
  private BigDecimal pendingBalance = BigDecimal.ZERO;

  @Column(name = "debt_balance", precision = 19, scale = 2, nullable = false, columnDefinition = "DECIMAL(19,2) DEFAULT 0")
  @Builder.Default
  private BigDecimal debtBalance = BigDecimal.ZERO;

  @Version
  @Column(name = "version")
  private Long version;

  /**
   * Get available balance (balance - pendingBalance)
   */
  public BigDecimal getAvailableBalance() {
    return balance.subtract(pendingBalance);
  }

  /**
   * Check if wallet has sufficient balance for withdrawal
   */
  public boolean hasSufficientBalance(BigDecimal amount) {
    return getAvailableBalance().compareTo(amount) >= 0;
  }

  /**
   * Credit money to wallet
   */
  public void credit(BigDecimal amount) {
    this.balance = this.balance.add(amount);
  }

  /**
   * Debit money from wallet
   */
  public void debit(BigDecimal amount) {
    if (!hasSufficientBalance(amount)) {
      throw new IllegalStateException("Insufficient balance");
    }
    this.balance = this.balance.subtract(amount);
  }

  /**
   * Add to debt balance
   */
  public void addDebt(BigDecimal amount) {
    this.debtBalance = this.debtBalance.add(amount);
  }

  /**
   * Reduce debt balance
   */
  public void reduceDebt(BigDecimal amount) {
    this.debtBalance = this.debtBalance.subtract(amount);
    if (this.debtBalance.compareTo(BigDecimal.ZERO) < 0) {
      this.debtBalance = BigDecimal.ZERO;
    }
  }
}
