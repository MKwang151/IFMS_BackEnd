package com.mkwang.backend.modules.accounting.entity;

import com.mkwang.backend.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * SystemFund entity - Represents the company's system fund (Mock Bank).
 * Typically has only one record (id = 1).
 */
@Entity
@Table(name = "system_funds")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemFund extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "total_balance", precision = 19, scale = 2, nullable = false)
  @Builder.Default
  private BigDecimal totalBalance = BigDecimal.ZERO;

  @Column(name = "bank_account", length = 30)
  private String bankAccount;

  @Column(name = "bank_name", length = 100)
  private String bankName;

  /**
   * Check if fund has sufficient balance
   */
  public boolean hasSufficientFund(BigDecimal amount) {
    return totalBalance.compareTo(amount) >= 0;
  }

  /**
   * Debit from system fund (for payouts)
   */
  public void debit(BigDecimal amount) {
    if (!hasSufficientFund(amount)) {
      throw new IllegalStateException("Insufficient system fund");
    }
    this.totalBalance = this.totalBalance.subtract(amount);
  }

  /**
   * Credit to system fund (top-up)
   */
  public void credit(BigDecimal amount) {
    this.totalBalance = this.totalBalance.add(amount);
  }
}
