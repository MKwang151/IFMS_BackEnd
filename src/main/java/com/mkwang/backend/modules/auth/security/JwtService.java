package com.mkwang.backend.modules.auth.security;

import com.mkwang.backend.modules.auth.entity.JwtToken;
import com.mkwang.backend.modules.auth.entity.TokenType;
import com.mkwang.backend.modules.auth.repository.JwtTokenRepository;
import com.mkwang.backend.modules.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * JWT Service — quản lý toàn bộ vòng đời token.
 * <p>
 * Tối ưu:
 * <ul>
 *   <li>{@code SecretKey} decode 1 lần duy nhất khi bean khởi tạo (không decode mỗi request)</li>
 *   <li>{@code JwtParser} build 1 lần, reuse thread-safe (không tạo mới mỗi lần parse)</li>
 *   <li>Access Token chứa roles + permissions claim → Filter không cần query DB authorities</li>
 *   <li>Mỗi token có {@code jti} (JWT ID) → trace, audit, revoke chính xác từng token</li>
 * </ul>
 */
@Slf4j
@Service
public class JwtService {

    private final JwtTokenRepository jwtTokenRepository;

    /** Decode 1 lần khi bean init — tránh BASE64.decode() mỗi request */
    private final SecretKey signingKey;

    /** Build 1 lần, thread-safe — tránh Jwts.parser().verifyWith().build() mỗi request */
    private final JwtParser jwtParser;

    private final long jwtExpiration;
    private final long refreshExpiration;

    public JwtService(
            JwtTokenRepository jwtTokenRepository,
            @Value("${application.security.jwt.secret-key}") String secretKey,
            @Value("${application.security.jwt.expiration}") long jwtExpiration,
            @Value("${application.security.jwt.refresh-token.expiration}") long refreshExpiration) {
        this.jwtTokenRepository = jwtTokenRepository;
        this.jwtExpiration = jwtExpiration;
        this.refreshExpiration = refreshExpiration;

        // Decode SecretKey 1 lần duy nhất
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);

        // Build JwtParser 1 lần — thread-safe, reuse cho mọi request
        this.jwtParser = Jwts.parser()
                .verifyWith(this.signingKey)
                .build();
    }

    // ── Extract Claims ─────────────────────────────────────────

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractJti(String token) {
        return extractClaim(token, Claims::getId);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse & verify token. Reuse cached JwtParser (thread-safe).
     * Phân biệt rõ: expired vs malformed vs invalid signature.
     */
    private Claims extractAllClaims(String token) {
        try {
            return jwtParser
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // Token hợp lệ nhưng hết hạn — vẫn trả Claims để extract username cho logging
            log.debug("JWT expired for subject: {}", e.getClaims().getSubject());
            throw e;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature/format: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT: {}", e.getMessage());
            throw e;
        }
    }

    // ── Generate Tokens ────────────────────────────────────────

    public String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        String token = buildToken(Map.of(), userDetails, refreshExpiration);
        saveToken(token, userDetails, refreshExpiration, TokenType.REFRESH);
        return token;
    }

    /**
     * Build JWT với:
     * - jti (JWT ID) — unique ID cho mỗi token, dùng để trace & revoke
     * - roles claim — ROLE_ADMIN, ROLE_USER (prefix ROLE_)
     * - permissions claim — READ_USER, WRITE_USER, etc.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {

        long now = System.currentTimeMillis();

        // Tách roles vs permissions từ authorities
        Collection<? extends GrantedAuthority> authorities = userDetails.getAuthorities();
        List<String> roles = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .collect(Collectors.toList());
        List<String> permissions = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .collect(Collectors.toList());

        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("roles", roles);
        claims.put("permissions", permissions);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())       // jti — unique per token
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiration))
                .signWith(signingKey)                    // Reuse cached key
                .compact();
    }

    // ── Validate Tokens ────────────────────────────────────────

    /** Access token — stateless, chỉ verify signature + expiry */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (JwtException e) {
            return false;
        }
    }

    /** Refresh token — stateful, check DB revocation */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && !jwtTokenRepository.existsByTokenAndRevokedTrue(token);
        } catch (JwtException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // ── Token Persistence (Refresh only) ───────────────────────

    private void saveToken(String token, UserDetails userDetails, long expiration, TokenType tokenType) {
        UserDetailsAdapter adapter = (UserDetailsAdapter) userDetails;
        User user = adapter.getUser();

        JwtToken jwtToken = JwtToken.builder()
                .token(token)
                .user(user)
                .tokenType(tokenType)
                .expiryDate(LocalDateTime.now().plusSeconds(expiration / 1000))
                .revoked(false)
                .build();

        jwtTokenRepository.save(jwtToken);
    }

    // ── Revocation ─────────────────────────────────────────────

    public void revokeToken(String token) {
        jwtTokenRepository.findByToken(token).ifPresent(jwtToken -> {
            jwtToken.setRevoked(true);
            jwtToken.setRevokedAt(LocalDateTime.now());
            jwtTokenRepository.save(jwtToken);
        });
    }

    public void revokeAllUserTokens(User user) {
        jwtTokenRepository.revokeAllUserTokens(user, LocalDateTime.now());
    }

    // ── Getters for config values ──────────────────────────────

    public long getJwtExpiration() { return jwtExpiration; }
    public long getRefreshExpiration() { return refreshExpiration; }
}
