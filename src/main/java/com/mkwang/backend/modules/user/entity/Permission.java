package com.mkwang.backend.modules.user.entity;

/**
 * Permission enum - Defines all available permissions in the system.
 * Managed by developers (hardcoded) to stay in sync with FE/BE code.
 */
public enum Permission {
  // Basic user permissions
  USER_READ("View user profile and basic information"),
  USER_WRITE("Edit user profile"),

  // Admin permissions
  ADMIN_READ("View all system data"),
  ADMIN_WRITE("Modify system data"),
  ADMIN_DELETE("Delete system data");

  private final String description;

  Permission(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
