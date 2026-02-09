package com.mkwang.backend.modules.organization.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Department entity - Represents company departments.
 * Each department has a manager and budget allocation.
 */
@Entity
@Table(name = "departments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String name;

  @Column(unique = true, nullable = false, length = 20)
  private String code;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id")
  private User manager;

  @Column(name = "budget_quota", precision = 19, scale = 2)
  @Builder.Default
  private BigDecimal budgetQuota = BigDecimal.ZERO;

  @Column(name = "available_balance", precision = 19, scale = 2)
  @Builder.Default
  private BigDecimal availableBalance = BigDecimal.ZERO;
}
