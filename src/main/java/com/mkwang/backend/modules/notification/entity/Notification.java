package com.mkwang.backend.modules.notification.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * Notification entity - Stores notification history for persistence.
 * Ensures users don't lose notifications when offline.
 * Works with WebSocket for real-time delivery.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 30)
  @Builder.Default
  private NotificationType type = NotificationType.SYSTEM;

  @Column(name = "ref_id")
  private Long refId; // Reference ID of related entity (Request, Payslip, etc.)

  @Column(name = "ref_type", length = 50)
  private String refType; // Type of reference: "REQUEST", "PAYSLIP", "PROJECT", etc.

  @Column(name = "is_read", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
  @Builder.Default
  private Boolean isRead = false;

  /**
   * Mark notification as read
   */
  public void markAsRead() {
    this.isRead = true;
  }

  /**
   * Build a reference link for frontend navigation
   */
  public String getReferenceLink() {
    if (refId == null || refType == null) {
      return null;
    }
    return "/" + refType.toLowerCase() + "s/" + refId;
  }
}
