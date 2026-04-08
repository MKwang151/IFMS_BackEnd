package com.mkwang.backend.modules.project.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * ExpenseCategory entity - Defines expense category types for project budget tracking.
 * Team Leader assigns categories to Phases and sets budget limits per category.
 *
 * Uses its own audit fields (created_at, updated_at, created_by, updated_by)
 * instead of BaseEntity because it has is_system_default flag.
 */
@Entity
@Table(name = "expense_categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * System default categories cannot be deleted.
     * Seeded by DataInitializer.
     */
    @Column(name = "is_system_default", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Builder.Default
    private Boolean isSystemDefault = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

