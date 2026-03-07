package com.mkwang.backend.modules.request.entity;

import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * RequestHistory entity - Audit log for request approval workflow.
 * Records all actions taken on a request (approve, reject, payout, cancel).
 * NO ESCALATE action — abolished completely.
 *
 * Does NOT extend BaseEntity — only has created_at (append-only, no updated_at/created_by).
 */
@Entity
@Table(name = "request_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id", nullable = false)
  private Request request;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "actor_id", nullable = false)
  private User actor;

  @Enumerated(EnumType.STRING)
  @Column(name = "action", nullable = false, length = 20)
  private RequestAction action;

  /**
   * Snapshot of the Request status AFTER this action was performed.
   * Uses the same RequestStatus enum as Request.status for consistency.
   */
  @Enumerated(EnumType.STRING)
  @Column(name = "status_after_action", nullable = false, length = 25)
  private RequestStatus statusAfterAction;

  @Column(name = "comment", length = 500)
  private String comment;

  @Column(name = "created_at", nullable = false, updatable = false)
  @Builder.Default
  private LocalDateTime createdAt = LocalDateTime.now();
}
