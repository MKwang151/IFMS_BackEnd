package com.mkwang.backend.modules.test.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test controller to demonstrate Role and Permission-based access control
 */
@RestController
@RequestMapping("/api/v1/test")
@Tag(name = "Test", description = "Test endpoints for permission demo")
@SecurityRequirement(name = "bearerAuth")
public class TestController {

  @GetMapping("/public")
  @Operation(summary = "Public endpoint - No authentication required")
  public ResponseEntity<ApiResponse<String>> publicEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("This is a public endpoint"));
  }

  @GetMapping("/authenticated")
  @Operation(summary = "Authenticated endpoint - Any logged-in user")
  public ResponseEntity<ApiResponse<String>> authenticatedEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You are authenticated!"));
  }

  @GetMapping("/user-read")
  @PreAuthorize("hasAuthority('USER_READ')")
  @Operation(summary = "USER_READ permission required")
  public ResponseEntity<ApiResponse<String>> userReadEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have USER_READ permission"));
  }

  @GetMapping("/user-write")
  @PreAuthorize("hasAuthority('USER_WRITE')")
  @Operation(summary = "USER_WRITE permission required")
  public ResponseEntity<ApiResponse<String>> userWriteEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have USER_WRITE permission"));
  }

  @GetMapping("/admin-read")
  @PreAuthorize("hasAuthority('ADMIN_READ')")
  @Operation(summary = "ADMIN_READ permission required")
  public ResponseEntity<ApiResponse<String>> adminReadEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have ADMIN_READ permission"));
  }

  @GetMapping("/admin-write")
  @PreAuthorize("hasAuthority('ADMIN_WRITE')")
  @Operation(summary = "ADMIN_WRITE permission required")
  public ResponseEntity<ApiResponse<String>> adminWriteEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have ADMIN_WRITE permission"));
  }

  @GetMapping("/admin-delete")
  @PreAuthorize("hasAuthority('ADMIN_DELETE')")
  @Operation(summary = "ADMIN_DELETE permission required")
  public ResponseEntity<ApiResponse<String>> adminDeleteEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have ADMIN_DELETE permission"));
  }

  @GetMapping("/admin-role")
  @PreAuthorize("hasRole('ADMIN')")
  @Operation(summary = "ADMIN role required")
  public ResponseEntity<ApiResponse<String>> adminRoleEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have ADMIN role"));
  }

  @GetMapping("/user-role")
  @PreAuthorize("hasRole('USER')")
  @Operation(summary = "USER role required")
  public ResponseEntity<ApiResponse<String>> userRoleEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have USER role"));
  }

  @GetMapping("/admin-or-user-write")
  @PreAuthorize("hasRole('ADMIN') or hasAuthority('USER_WRITE')")
  @Operation(summary = "ADMIN role OR USER_WRITE permission required")
  public ResponseEntity<ApiResponse<String>> combinedEndpoint() {
    return ResponseEntity.ok(ApiResponse.success("You have ADMIN role or USER_WRITE permission"));
  }
}
