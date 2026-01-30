package com.mkwang.backend.modules.auth.entity;

/**
 * Enum to distinguish between different types of JWT tokens
 */
public enum TokenType {
  /**
   * Short-lived access token (15-30 minutes)
   * Not stored in database for performance optimization
   */
  ACCESS,

  /**
   * Long-lived refresh token (7+ days)
   * Stored in database to enable revocation
   */
  REFRESH
}
