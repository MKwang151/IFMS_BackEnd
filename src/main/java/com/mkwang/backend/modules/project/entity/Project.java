package com.mkwang.backend.modules.project.entity;

import com.mkwang.backend.common.base.BaseEntity;
import com.mkwang.backend.modules.organization.entity.Department;
import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Project entity - Represents a project with lifecycle management.
 * Budget is allocated per Phase, not directly from project.
 */
@Entity
@Table(name = "projects", indexes = {
    @Index(name = "idx_projects_project_code", columnList = "project_code")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Human-readable project identifier (Cost Center).
   * Format: PRJ-{DEPT}-{YEAR} e.g. PRJ-ERP-2026
   * Auto-generated at application layer before persistence.
   */
  @Column(name = "project_code", nullable = false, unique = true, length = 30)
  private String projectCode;

  @Column(nullable = false)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id", nullable = false)
  private Department department;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "manager_id", nullable = false)
  private User manager;

  @Column(name = "total_budget", precision = 19, scale = 2)
  @Builder.Default
  private BigDecimal totalBudget = BigDecimal.ZERO;

  @Column(name = "total_spent", precision = 19, scale = 2)
  @Builder.Default
  private BigDecimal totalSpent = BigDecimal.ZERO;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  @Builder.Default
  private ProjectStatus status = ProjectStatus.PLANNING;

  @OneToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "current_phase_id")
  private ProjectPhase currentPhase;

  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProjectPhase> phases = new ArrayList<>();

  @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
  @Builder.Default
  private List<ProjectMember> members = new ArrayList<>();

  /**
   * Check if project allows new requests
   */
  public boolean isRequestable() {
    return status == ProjectStatus.ACTIVE;
  }

  /**
   * Add spent amount to total
   */
  public void addSpent(BigDecimal amount) {
    this.totalSpent = this.totalSpent.add(amount);
  }
}
