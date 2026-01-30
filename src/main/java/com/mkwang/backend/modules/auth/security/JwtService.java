package com.mkwang.backend.modules.auth.security;

import com.mkwang.backend.modules.auth.entity.JwtToken;
import com.mkwang.backend.modules.auth.entity.TokenType;
import com.mkwang.backend.modules.auth.repository.JwtTokenRepository;
import com.mkwang.backend.modules.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenRepository jwtTokenRepository;

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        // Access tokens are NOT saved to database for performance optimization
        // They are validated via signature and expiry only
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        String token = buildToken(new HashMap<>(), userDetails, refreshExpiration);
        // Refresh tokens ARE saved to database to enable revocation
        saveToken(token, userDetails, refreshExpiration, TokenType.REFRESH);
        return token;
    }

    private void saveToken(String token, UserDetails userDetails, long expiration, TokenType tokenType) {
        UserDetailsAdapter adapter = (UserDetailsAdapter) userDetails;
        User user = adapter.getUser();

        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(expiration / 1000);

        JwtToken jwtToken = JwtToken.builder()
                .token(token)
                .user(user)
                .tokenType(tokenType)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        jwtTokenRepository.save(jwtToken);
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts
                .builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate access token (stateless - no DB check)
     * Access tokens are short-lived and not stored in database
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isTokenExpired = isTokenExpired(token);

        // Access tokens are NOT stored in DB, so we only validate signature + expiry
        // Refresh tokens ARE stored in DB, so we check revocation status
        // Simple optimization: access tokens won't exist in DB, so no query needed

        return (username.equals(userDetails.getUsername())) && !isTokenExpired;
    }

    /**
     * Validate refresh token (stateful - checks DB for revocation)
     * Refresh tokens are long-lived and stored in database for revocation
     * capability
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isTokenExpired = isTokenExpired(token);
        boolean isTokenRevoked = jwtTokenRepository.existsByTokenAndRevokedTrue(token);

        return (username.equals(userDetails.getUsername()))
                && !isTokenExpired
                && !isTokenRevoked;
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts
                .parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

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
}
