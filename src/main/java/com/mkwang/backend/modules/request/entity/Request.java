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
@Table(name = "requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Request extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  // --- CẬP NHẬT: THAY ĐỔI TỪ SINGLE FILE SANG LIST FILES ---

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JoinTable(
          name = "request_attachments", // Tên bảng trung gian
          joinColumns = @JoinColumn(name = "request_id"), // Khóa ngoại trỏ về Request
          inverseJoinColumns = @JoinColumn(name = "file_id") // Khóa ngoại trỏ về FileStorage
  )
  @Builder.Default
  private List<FileStorage> attachments = new ArrayList<>();

  // -----------------------------------------------------------

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
}