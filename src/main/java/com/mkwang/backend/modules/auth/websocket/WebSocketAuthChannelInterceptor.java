package com.mkwang.backend.modules.auth.websocket;

import com.mkwang.backend.common.exception.WebSocketAccountException;
import com.mkwang.backend.common.exception.WebSocketAuthException;
import com.mkwang.backend.modules.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

/**
 * Interceptor xác thực JWT tại WebSocket handshake (STOMP CONNECT frame).
 * <p>
 * Flow: client gửi STOMP CONNECT với header
 * {@code Authorization: Bearer <token>}
 * → interceptor extract JWT → validate → set Authentication vào principal.
 * Các frame sau (SEND, SUBSCRIBE) kế thừa authentication đã được thiết lập.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        // Chỉ xác thực tại thời điểm CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("WebSocket CONNECT rejected: missing or invalid Authorization header");
                throw new WebSocketAuthException("Missing or invalid Authorization header");
            }

            String jwt = authHeader.substring(BEARER_PREFIX.length());

            try {
                String username = jwtService.extractUsername(jwt);

                if (username == null) {
                    throw new WebSocketAuthException("Cannot extract username from token");
                }

                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (!userDetails.isEnabled()) {
                    throw new WebSocketAccountException("Account is disabled. Contact administrator.");
                }
                if (!userDetails.isAccountNonLocked()) {
                    throw new WebSocketAccountException("Account is locked. Contact administrator.");
                }
                if (!jwtService.isTokenValid(jwt, userDetails)) {
                    throw new WebSocketAuthException("Token is invalid or expired");
                }

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities());

                // Đặt authentication vào STOMP session — được kế thừa bởi mọi frame sau
                accessor.setUser(authentication);

                log.debug("WebSocket authenticated: user={}", username);

            } catch (JwtException e) {
                log.warn("WebSocket CONNECT rejected: invalid JWT — {}", e.getMessage());
                throw new WebSocketAuthException("Invalid JWT token", e);
            }
        }

        return message;
    }
}
