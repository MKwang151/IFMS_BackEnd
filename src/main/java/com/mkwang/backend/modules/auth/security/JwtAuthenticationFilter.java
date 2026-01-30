package com.mkwang.backend.modules.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip filter for public endpoints
        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Check account status before validating token
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    String errorMessage = !userDetails.isEnabled()
                            ? "User account is disabled"
                            : "User account is locked";

                    log.warn("Authentication failed for user {}: {}", userEmail, errorMessage);
                    sendErrorResponse(response, "Unauthorized: " + errorMessage);
                    return; // Stop filter chain
                }

                // Validate token and check if it's revoked
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    log.warn("Invalid or revoked token for user {}", userEmail);
                    sendErrorResponse(response, "Unauthorized: Invalid or revoked token");
                    return; // Stop filter chain
                }

                // Set authentication in context
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            log.error("Unexpected authentication error: {}", e.getMessage(), e);
            sendErrorResponse(response, "Unauthorized: Authentication failed");
        }
    }

    /**
     * Helper method to write error response in JSON format
     * Cannot use ResponseEntity here because we're in Filter layer, not Controller layer
     */
    private void sendErrorResponse(HttpServletResponse response, String errorMessage) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                objectMapper.writeValueAsString(
                        ApiResponse.error(errorMessage)
                )
        );
        response.getWriter().flush();
    }
}
