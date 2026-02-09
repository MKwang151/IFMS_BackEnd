package com.mkwang.backend.modules.auth.service;

import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.UnauthorizedException;
import com.mkwang.backend.modules.auth.dto.response.AuthenticationResponse;
import com.mkwang.backend.modules.auth.dto.response.UserInfoResponse;
import com.mkwang.backend.modules.auth.dto.request.LoginRequest;
import com.mkwang.backend.modules.auth.dto.request.RegisterRequest;
import com.mkwang.backend.modules.auth.security.JwtService;
import com.mkwang.backend.modules.auth.security.UserDetailsAdapter;
import com.mkwang.backend.modules.user.entity.Role;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.repository.RoleRepository;
import com.mkwang.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

        private final UserRepository userRepository;
        private final RoleRepository roleRepository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;

        private static final String DEFAULT_ROLE = "USER";

        @Value("${application.security.jwt.expiration}")
        private long jwtExpiration;

        @Override
        @Transactional
        public AuthenticationResponse register(RegisterRequest request) {
                // Check if email already exists
                if (userRepository.existsByEmail(request.getEmail())) {
                        throw new BadRequestException("Email already registered");
                }

                // Get default USER role from database
                Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                                .orElseThrow(() -> new BadRequestException(
                                                "Default role not found. Please contact admin."));

                // Create new user
                User user = User.builder()
                                .fullName(request.getFirstName() + " " + request.getLastName())
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(defaultRole)
                                .enabled(true)
                                .build();

                userRepository.save(user);
                log.info("User registered successfully: {}", user.getEmail());

                return generateTokenResponse(user);
        }

        @Override
        public AuthenticationResponse login(LoginRequest request) {
                authenticationManager.authenticate(
                                new UsernamePasswordAuthenticationToken(
                                                request.getEmail(),
                                                request.getPassword()));

                var user = userRepository.findByEmail(request.getEmail())
                                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

                log.info("User logged in successfully: {}", user.getEmail());

                return generateTokenResponse(user);
        }

        @Override
        public AuthenticationResponse refreshToken(String refreshToken) {
                // Validate token format and extract username safely
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

                // Create UserDetailsAdapter for token validation
                UserDetailsAdapter userDetails = new UserDetailsAdapter(user);

                // Use isRefreshTokenValid to check DB revocation status
                if (!jwtService.isRefreshTokenValid(refreshToken, userDetails)) {
                        throw new UnauthorizedException("Invalid or revoked refresh token");
                }

                return generateTokenResponse(user);
        }

        private AuthenticationResponse generateTokenResponse(User user) {
                // Use Adapter Pattern to convert User to UserDetails
                UserDetailsAdapter userDetails = new UserDetailsAdapter(user);

                var accessToken = jwtService.generateToken(userDetails);
                var refreshToken = jwtService.generateRefreshToken(userDetails);

                // Build user info with role and permissions
                UserInfoResponse userInfo = UserInfoResponse.builder()
                                .id(user.getId())
                                .email(user.getEmail())
                                .fullName(user.getFullName())
                                .role(user.getRole().getName())
                                .permissions(user.getRole().getPermissions().stream()
                                                .map(Enum::name)
                                                .collect(Collectors.toSet()))
                                .build();

                return AuthenticationResponse.builder()
                                .accessToken(accessToken)
                                .refreshToken(refreshToken)
                                .tokenType("Bearer")
                                .expiresIn(jwtExpiration / 1000) // Convert to seconds
                                .user(userInfo)
                                .build();
        }
}
