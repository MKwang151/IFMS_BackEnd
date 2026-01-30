package com.mkwang.backend.modules.auth.repository;

import com.mkwang.backend.modules.auth.entity.JwtToken;
import com.mkwang.backend.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    boolean existsByTokenAndRevokedTrue(String token);

    Optional<JwtToken> findByToken(String token);

    List<JwtToken> findAllByUserAndRevokedFalse(User user);

    @Modifying
    @Query("UPDATE JwtToken t SET t.revoked = true, t.revokedAt = :revokedAt WHERE t.user = :user AND t.revoked = false")
    void revokeAllUserTokens(User user, LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM JwtToken t WHERE t.expiryDate < :expiryDate")
    void deleteExpiredTokens(LocalDateTime expiryDate);
}
