package com.mkwang.backend.modules.wallet.entity;

import com.mkwang.backend.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Transaction entity - Immutable record of wallet transactions.
 * Once created, transactions should NEVER be updated or deleted.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  @Column(name = "amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private TransactionStatus status = TransactionStatus.SUCCESS;

  @Column(name = "ref_request_id")
  private Long refRequestId; // Reference to Request if applicable

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;
}
