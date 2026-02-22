package com.mkwang.backend.modules.user.entity;

import com.mkwang.backend.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * UserSecuritySettings entity - Stores transaction PIN and security settings.
 * One-to-One relationship with User (Shares primary key).
 */
@Entity
@Table(name = "user_security_settings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSecuritySettings extends BaseEntity {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "transaction_pin", length = 100)
  private String transactionPin; // Hashed PIN

  @Column(name = "retry_count")
  @Builder.Default
  private Integer retryCount = 0;

  @Column(name = "locked_until")
  private LocalDateTime lockedUntil;

  /**
   * Check if the PIN is currently locked
   */
  public boolean isPinLocked() {
    return lockedUntil != null && LocalDateTime.now().isBefore(lockedUntil);
  }

  /**
   * Increment retry count after failed PIN attempt
   */
  public void incrementRetryCount() {
    this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
  }

  /**
   * Reset retry count after successful PIN verification
   */
  public void resetRetryCount() {
    this.retryCount = 0;
    this.lockedUntil = null;
  }

  /**
   * Lock PIN for specified duration
   */
  public void lockPin(int minutes) {
    this.lockedUntil = LocalDateTime.now().plusMinutes(minutes);
  }
}
