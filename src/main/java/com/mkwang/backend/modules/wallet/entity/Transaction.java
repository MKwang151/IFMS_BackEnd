package com.mkwang.backend.modules.wallet.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Transaction entity - Immutable Master Ledger for ALL wallet movements.
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

  // LƯU Ý: Không dùng unique=true vì các giao dịch nội bộ sẽ có paymentRef = null
  @Column(name = "payment_ref", length = 100)
  private String paymentRef;

  @Enumerated(EnumType.STRING)
  @Column(name = "gateway_provider", length = 20)
  private PaymentProvider gatewayProvider; // PAYOS, MOMO, VNPAY, INTERNAL

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  @Column(name = "amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal amount;

  // Bức ảnh chụp số dư ví ngay sau khi giao dịch thành công
  @Column(name = "balance_after", precision = 19, scale = 2, nullable = false)
  private BigDecimal balanceAfter;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  private TransactionType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private TransactionStatus status = TransactionStatus.SUCCESS;

  // ------------------------------------------------------------------
  // ĐA HÌNH THAM CHIẾU (Polymorphic Reference) - DÙNG ENUM
  // ------------------------------------------------------------------
  @Enumerated(EnumType.STRING)
  @Column(name = "reference_type", nullable = false, length = 30)
  private ReferenceType referenceType;

  // ID tương ứng của entity sinh ra giao dịch này.
  // Ví dụ: referenceType = REQUEST, thì referenceId = 99 (ID của bảng requests)
  @Column(name = "reference_id")
  private Long referenceId;

  // ------------------------------------------------------------------
  // LIÊN KẾT BÚT TOÁN KÉP (Dòng tiền đối ứng)
  // ------------------------------------------------------------------
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "related_transaction_id")
  private Transaction relatedTransaction;

  // ------------------------------------------------------------------
  // KIỂM TOÁN (Ai bấm nút?)
  // ------------------------------------------------------------------
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;
}