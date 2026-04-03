package com.mkwang.backend.modules.user.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.user.dto.request.OnboardUserRequest;
import com.mkwang.backend.modules.user.dto.response.OnboardUserResponse;
import com.mkwang.backend.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;

    // ── POST /users/onboard ──────────────────────────────────────

    @PostMapping("/onboard")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Onboard a new employee — creates account, wallet and sends credential email (Admin only)")
    public ResponseEntity<ApiResponse<OnboardUserResponse>> onboardUser(
            @Valid @RequestBody OnboardUserRequest request) {
        OnboardUserResponse response = userService.onboardUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User onboarded successfully", response));
    }
}
