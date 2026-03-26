package com.mkwang.backend.modules.mail.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * BrevoMailServiceImpl — Gửi email qua Brevo Transactional API v3.
 * <p>
 * POST https://api.brevo.com/v3/smtp/email
 * Header: api-key: <BREVO_API_KEY>
 * <p>
 * Mọi method gọi sendHtml() nền — không cần queue, không cần Redis.
 * @Async("mailExecutor") đảm bảo không block API thread.
 */
@Slf4j
@Service
public class BrevoMailServiceImpl implements BrevoMailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromEmail;
    private final String fromName;

    public BrevoMailServiceImpl(
            @Value("${application.mail.brevo-api-key}") String apiKey,
            @Value("${application.mail.from-email}") String fromEmail,
            @Value("${application.mail.from-name}") String fromName) {
        this.apiKey = apiKey;
        this.fromEmail = fromEmail;
        this.fromName = fromName;
        this.restClient = RestClient.builder()
                .baseUrl(BREVO_API_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("api-key", apiKey)
                .build();
    }

    // ── Core send ────────────────────────────────────────────────

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendHtml(String to, String subject, String htmlContent) {
        try {
            Map<String, Object> body = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent);

            var response = restClient.post()
                    .body(body)
                    .retrieve()
                    .toEntity(String.class);

            log.info("[Brevo] HTTP={} | to={} | subject={}", response.getStatusCode(), to, subject);
            log.debug("[Brevo] Response body: {}", response.getBody());
            return CompletableFuture.completedFuture(true);

        } catch (Exception e) {
            log.error("Failed to send email via Brevo: to={}, subject={}, error={}", to, subject, e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

}
