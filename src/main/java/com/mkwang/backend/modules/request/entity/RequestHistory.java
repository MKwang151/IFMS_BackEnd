package com.mkwang.backend.modules.request.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * RequestHistory entity - Audit log for request approval workflow.
 * Records all actions taken on a request (approve, reject, escalate).
 */
@Entity
@Table(name = "request_histories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestHistory extends BaseEntity {

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

  @Column(name = "comment", columnDefinition = "TEXT")
  private String comment;
}
