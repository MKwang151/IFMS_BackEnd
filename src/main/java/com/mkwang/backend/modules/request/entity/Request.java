package com.mkwang.backend.modules.request.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.expense.entity.ExpenseCategory;
import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.project.entity.Project;
import com.mkwang.backend.modules.project.entity.ProjectPhase;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Request entity - Represents a financial request in the approval workflow.
 *
 * Three flows (NO escalation):
 * - Flow 1 (ADVANCE/EXPENSE/REIMBURSE): Member → Team Leader → Accountant
 * - Flow 2 (PROJECT_TOPUP): Team Leader → Manager → Auto
 * - Flow 3 (QUOTA_TOPUP): Manager → Admin → Auto
 */
@Entity
@Table(name = "requests", indexes = {
    @Index(name = "idx_requests_request_code", columnList = "request_code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "request_code", nullable = false, unique = true, length = 30)
  private String requestCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  /**
   * Nullable: NULL for QUOTA_TOPUP (department-level request).
   * Required for ADVANCE/EXPENSE/REIMBURSE/PROJECT_TOPUP.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id")
  private Project project;

  /**
   * Nullable: NULL for PROJECT_TOPUP and QUOTA_TOPUP.
   * Required for ADVANCE/EXPENSE/REIMBURSE.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "phase_id")
  private ProjectPhase phase;

  /**
   * Nullable: NULL for PROJECT_TOPUP and QUOTA_TOPUP.
   * Required for ADVANCE/EXPENSE/REIMBURSE.
   * Links to expense_categories table for budget category tracking.
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private ExpenseCategory category;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private RequestType type;

  /**
   * Amount requested. Must be <= available balance of corresponding fund at creation time.
   */
  @Column(name = "amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "approved_amount", precision = 19, scale = 2)
  private BigDecimal approvedAmount;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<RequestAttachment> attachments = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 25)
  @Builder.Default
  private RequestStatus status = RequestStatus.PENDING_APPROVAL;

  @Column(name = "reject_reason", columnDefinition = "TEXT")
  private String rejectReason;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<RequestHistory> histories = new ArrayList<>();

  // ======================== Business Logic ========================

  /**
   * Can only cancel when status is PENDING_APPROVAL.
   */
  public boolean isCancellable() {
    return status == RequestStatus.PENDING_APPROVAL;
  }

  /**
   * Whether the request is awaiting any form of approval/processing.
   */
  public boolean isPending() {
    return status == RequestStatus.PENDING_APPROVAL || status == RequestStatus.PENDING_ACCOUNTANT;
  }

  /**
   * Whether this request type requires proof of expense (attachments).
   */
  public boolean requiresProof() {
    return type == RequestType.EXPENSE || type == RequestType.REIMBURSE;
  }

  /**
   * Whether this is a personal expense request (Flow 1).
   */
  public boolean isPersonalExpense() {
    return type == RequestType.ADVANCE || type == RequestType.EXPENSE || type == RequestType.REIMBURSE;
  }

  /**
   * Whether this is a fund top-up request (Flow 2 or 3).
   */
  public boolean isTopUp() {
    return type == RequestType.PROJECT_TOPUP || type == RequestType.QUOTA_TOPUP;
  }

  // ======================== Attachment Helpers ========================

  public void addAttachment(FileStorage file) {
    RequestAttachment attachment = RequestAttachment.builder()
            .request(this)
            .file(file)
            .build();
    this.attachments.add(attachment);
  }

  public void removeAttachment(Long fileId) {
    this.attachments.removeIf(att -> att.getFile().getId().equals(fileId));
  }

  public List<FileStorage> getAttachmentFiles() {
    return this.attachments.stream()
            .map(RequestAttachment::getFile)
            .toList();
  }
}