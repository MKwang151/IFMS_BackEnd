package com.mkwang.backend.modules.auth.service;

import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.common.exception.UnauthorizedException;
import com.mkwang.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.LoginRequest;
import com.mkwang.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mkwang.backend.modules.auth.dto.response.AuthenticationResponse;
import com.mkwang.backend.modules.auth.dto.response.UserInfoResponse;
import com.mkwang.backend.modules.auth.security.JwtService;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    // ── POST /auth/login ─────────────────────────────────────────

    @Override
    @Transactional
    public AuthenticationResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        var user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        // Single-session: tăng tokenVersion → invalidate mọi token cũ
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);

        // Cache version mới vào Redis
        jwtService.cacheTokenVersion(user.getId(), user.getTokenVersion());

        log.info("User logged in: {}", user.getEmail());
        return generateTokenResponse(user);
    }

    // ── POST /auth/refresh-token ─────────────────────────────────

    @Override
    public AuthenticationResponse refreshToken(String refreshToken) {
        String userEmail;
        try {
            userEmail = jwtService.extractUsername(refreshToken);
        } catch (Exception e) {
            log.warn("Invalid refresh token format: {}", e.getMessage());
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (userEmail == null) {
            throw new UnauthorizedException("Invalid refresh token");
        }

        var user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        UserDetailsAdapter userDetails = new UserDetailsAdapter(user);

        // Stateless: chỉ check chữ ký + expiry + version
        if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        if (!jwtService.isTokenVersionValid(refreshToken, user.getId())) {
            throw new UnauthorizedException("Session expired. Please login again.");
        }

        log.info("Token refreshed for user: {}", user.getEmail());
        return generateTokenResponse(user); // giữ tokenVersion hiện tại
    }

    // ── POST /auth/logout ────────────────────────────────────────

    @Override
    @Transactional
    public void logout(String accessToken) {
        try {
            String userEmail = jwtService.extractUsername(accessToken);
            var user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UnauthorizedException("User not found"));

            // Stateless logout: tăng tokenVersion → mọi token hiện tại đều bị invalidate
            user.setTokenVersion(user.getTokenVersion() + 1);
            userRepository.save(user);
            jwtService.cacheTokenVersion(user.getId(), user.getTokenVersion());

            log.info("User logged out: {}", userEmail);
        } catch (Exception e) {
            log.warn("Logout failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid token");
        }
    }

    // ── POST /auth/forgot-password ───────────────────────────────

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        var userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            log.debug("Forgot password requested for non-existing email: {}", request.getEmail());
            return; // Silent — không leak thông tin email tồn tại
        }
        // TODO: Generate reset token, lưu DB, gửi email qua BrevoMailService
        log.info("Password reset requested for: {}", request.getEmail());
    }

    // ── POST /auth/reset-password ────────────────────────────────

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }
        // TODO: Validate reset token từ DB (bảng password_reset_tokens)
        throw new BadRequestException("Password reset token validation not yet implemented");
    }

    // ── POST /auth/change-password ───────────────────────────────

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String username) {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!user.getIsFirstLogin()) {
            throw new BadRequestException("This endpoint is only for first login password change");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setIsFirstLogin(false);
        userRepository.save(user);

        log.info("First login password changed for: {}", username);
    }

    // ── GET /auth/me ─────────────────────────────────────────────

    @Override
    public UserInfoResponse getCurrentUser(String username) {
        var user = userRepository.findByEmail(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return buildUserInfo(user);
    }

    // ── Private helpers ──────────────────────────────────────────

    private AuthenticationResponse generateTokenResponse(User user) {
        UserDetailsAdapter userDetails = new UserDetailsAdapter(user);
        int version = user.getTokenVersion();

        var accessToken = jwtService.generateToken(userDetails, version);
        var refreshToken = jwtService.generateRefreshToken(userDetails, version);

        return AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(buildUserInfo(user))
                .build();
    }

    private UserInfoResponse buildUserInfo(User user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .departmentId(user.getDepartment() != null ? user.getDepartment().getId() : null)
                .departmentName(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .avatar(null)
                .isFirstLogin(user.getIsFirstLogin())
                .status(user.getStatus().name())
                .build();
    }
}
