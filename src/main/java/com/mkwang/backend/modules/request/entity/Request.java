package com.mkwang.backend.modules.request.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.project.entity.Project;
import com.mkwang.backend.modules.project.entity.ProjectPhase;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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

  /**
   * Human-readable request / document code. Used by accounting for reconciliation and PDF printing.
   * Format: REQ-{TYPE}-{MMYY}-{SEQ} e.g. REQ-IT-2602-001
   * Auto-generated at application layer before persistence.
   */
  @Column(name = "request_code", nullable = false, unique = true, length = 30)
  private String requestCode;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "requester_id", nullable = false)
  private User requester;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "phase_id", nullable = false)
  private ProjectPhase phase;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 20)
  private RequestType type;

  @Column(name = "amount", precision = 19, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "approved_amount", precision = 19, scale = 2)
  private BigDecimal approvedAmount;

  /**
   * List of attachments (invoices, receipts, PDFs, Excel files) for this request.
   * Mapped via RequestAttachment entity to have proper composite PK (request_id, file_id).
   */
  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @Builder.Default
  private List<RequestAttachment> attachments = new ArrayList<>();

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private RequestStatus status = RequestStatus.PENDING_MANAGER;

  @Column(name = "reject_reason", columnDefinition = "TEXT")
  private String rejectReason;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<RequestHistory> histories = new ArrayList<>();

  // Logic nghiệp vụ
  public boolean isCancellable() {
    return status == RequestStatus.PENDING_MANAGER || status == RequestStatus.PENDING_ADMIN;
  }

  public boolean isPending() {
    return status == RequestStatus.PENDING_MANAGER || status == RequestStatus.PENDING_ADMIN;
  }

  public boolean requiresProof() {
    return type == RequestType.EXPENSE || type == RequestType.REIMBURSE;
  }

  // Helper methods for managing attachments
  /**
   * Add an attachment file to this request.
   * Creates the RequestAttachment association entity automatically.
   */
  public void addAttachment(FileStorage file) {
    RequestAttachment attachment = RequestAttachment.builder()
            .request(this)
            .file(file)
            .build();
    this.attachments.add(attachment);
  }

  /**
   * Remove an attachment by file ID.
   */
  public void removeAttachment(Long fileId) {
    this.attachments.removeIf(att -> att.getFile().getId().equals(fileId));
  }

  /**
   * Get list of FileStorage from attachments (convenience method for business logic).
   */
  public List<FileStorage> getAttachmentFiles() {
    return this.attachments.stream()
            .map(RequestAttachment::getFile)
            .toList();
  }
}