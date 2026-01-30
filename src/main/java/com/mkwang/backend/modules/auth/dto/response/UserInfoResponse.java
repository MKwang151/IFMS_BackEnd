package com.mkwang.backend.modules.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * User info included in login response for FE to display correct UI
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoResponse {

  private Long id;

  private String email;

  @JsonProperty("full_name")
  private String fullName;

  private String role;

  private Set<String> permissions;
}
