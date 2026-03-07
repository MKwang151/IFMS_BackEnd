package com.mkwang.backend.modules.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkwang.backend.common.dto.ApiResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
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
    private final ObjectMapper objectMapper;   // Inject Spring-managed singleton (Jackson auto-config)

    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Skip filter for public endpoints (SecurityConfig whitelist handles this,
        // but early return avoids unnecessary header parsing)
        if (request.getServletPath().startsWith("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(BEARER_PREFIX.length());

        try {
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                // Check account status
                if (!userDetails.isEnabled()) {
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                            "Account is disabled. Contact administrator.");
                    return;
                }
                if (!userDetails.isAccountNonLocked()) {
                    sendErrorResponse(response, HttpServletResponse.SC_FORBIDDEN,
                            "Account is locked. Contact administrator.");
                    return;
                }

                // Validate token
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                            "Invalid or expired token.");
                    return;
                }

                // Set authentication in SecurityContext
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.debug("Expired JWT for user: {}", e.getClaims().getSubject());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Token has expired. Please refresh your token.");
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid token.");
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage(), e);
            sendErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Authentication failed.");
        }
    }

    private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message));
    }
}
