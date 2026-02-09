package com.mkwang.backend.modules.project.entity;

import com.mkwang.backend.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ProjectMember entity - Many-to-Many relationship between Project and User.
 * Uses composite primary key (projectId, userId).
 */
@Entity
@Table(name = "project_members")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMember {

  @EmbeddedId
  private ProjectMemberId id;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("projectId")
  @JoinColumn(name = "project_id")
  private Project project;

  @ManyToOne(fetch = FetchType.LAZY)
  @MapsId("userId")
  @JoinColumn(name = "user_id")
  private User user;

  @Column(name = "role", length = 50)
  private String role; // Role in project (Dev, Tester, Designer, etc.)

  @Column(name = "joined_at", nullable = false)
  @Builder.Default
  private LocalDateTime joinedAt = LocalDateTime.now();
}
