package com.mkwang.backend.modules.auth.task;

import com.mkwang.backend.modules.auth.repository.JwtTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupTask {

    private final JwtTokenRepository jwtTokenRepository;

    /**
     * Cleanup expired tokens every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting expired token cleanup task");
        try {
            jwtTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired token cleanup completed successfully");
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage(), e);
        }
    }
}
