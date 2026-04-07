package com.mkwang.backend.modules.profile.controller;

import com.mkwang.backend.common.dto.ApiResponse;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.profile.dto.request.UpdateMyProfileRequest;
import com.mkwang.backend.modules.profile.dto.response.MyProfileResponse;
import com.mkwang.backend.modules.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Current user profile APIs")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/me/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserDetailsAdapter principal) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(profileService.getMyProfile(userId)));
    }

    @PutMapping("/me/profile")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateMyProfile(
            @AuthenticationPrincipal UserDetailsAdapter principal,
            @Valid @RequestBody UpdateMyProfileRequest request) {
        Long userId = principal.getUser().getId();
        return ResponseEntity.ok(ApiResponse.success(profileService.updateMyProfile(userId, request)));
    }
}

