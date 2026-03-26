package com.mkwang.backend.modules.mail.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * BrevoMailService — Gửi email qua Brevo Transactional API v3.
 * <p>
 * Mọi method đều @Async("mailExecutor") — không block caller thread.
 * Return CompletableFuture<Boolean>: true = thành công, false = thất bại (logged).
 */
public interface BrevoMailService {

    /**
     * Gửi email HTML tùy ý. Method nền cho tất cả nghiệp vụ.
     *
     * @param to          email người nhận
     * @param subject     tiêu đề email
     * @param htmlContent nội dung HTML đầy đủ
     */
    CompletableFuture<Boolean> sendHtml(String to, String subject, String htmlContent);


}
