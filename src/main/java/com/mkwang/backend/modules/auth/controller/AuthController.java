package com.mkwang.backend.modules.auth.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.LoginRequest;
import com.mkwang.backend.modules.auth.dto.request.RefreshTokenRequest;
import com.mkwang.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mkwang.backend.modules.auth.dto.response.AuthenticationResponse;
import com.mkwang.backend.modules.auth.dto.response.UserInfoResponse;
import com.mkwang.backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {

    private final AuthService authService;

    private static final String BEARER_PREFIX = "Bearer ";

    // ── POST /auth/login ─────────────────────────────────────────

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthenticationResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    // ── POST /auth/logout ────────────────────────────────────────

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Logout and revoke all refresh tokens")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader) {
        String token = extractToken(authHeader);
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully",
                Map.of("message", "Logged out successfully")));
    }

    // ── POST /auth/refresh-token ─────────────────────────────────

    @PostMapping("/refresh-token")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        AuthenticationResponse response = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    // ── POST /auth/forgot-password ───────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset email")
    public ResponseEntity<ApiResponse<Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Success",
                Map.of("message", "Reset email sent if account exists")));
    }

    // ── POST /auth/reset-password ────────────────────────────────

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token from email")
    public ResponseEntity<ApiResponse<Map<String, String>>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Success",
                Map.of("message", "Password reset successfully")));
    }

    // ── POST /auth/change-password ───────────────────────────────

    @PostMapping("/change-password")
    @Operation(summary = "Change password on first login")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        authService.changePassword(request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Success",
                Map.of("message", "Password changed successfully")));
    }

    // ── GET /auth/me ─────────────────────────────────────────────

    @GetMapping("/me")
    @Operation(summary = "Get current user info (restore session)")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getCurrentUser(
            Authentication authentication) {
        UserInfoResponse response = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }

    // ── Private helpers ──────────────────────────────────────────

    private String extractToken(String authHeader) {
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("Invalid Authorization header");
    }
}
