package com.mkwang.backend.modules.mail.service;

import com.mkwang.backend.modules.mail.dto.EmailPayload;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * MailServiceImpl — Async email sending implementation.
 * <p>
 * Architecture:
 * <pre>
 *   ┌──────────────────┐
 *   │  Caller Thread   │  (API thread / Scheduler thread)
 *   └────────┬─────────┘
 *            │ @Async("mailExecutor")
 *            ▼
 *   ┌──────────────────┐
 *   │  Mail Thread Pool │  (ifms-mail-1, ifms-mail-2, ... max 5)
 *   │  ┌──────────────┐│
 *   │  │ Thymeleaf    ││  → Render HTML template
 *   │  │ MimeMessage  ││  → Build email + attachment
 *   │  │ JavaMailSend ││  → SMTP send (blocking I/O, 2-5s)
 *   │  └──────────────┘│
 *   └──────────────────┘
 * </pre>
 * <p>
 * WHY @Async on every method?
 * <ul>
 *   <li>SMTP send is blocking I/O (2-5 seconds per email). Without @Async, the caller
 *       thread is blocked — this means API response time includes email send time.</li>
 *   <li>With @Async("mailExecutor"), the caller returns immediately with a CompletableFuture.
 *       The actual SMTP work happens on a dedicated thread pool (ifms-mail-*).</li>
 *   <li>The "mailExecutor" pool (core=2, max=5) is isolated from the general async pool,
 *       preventing slow SMTP from starving other async tasks.</li>
 * </ul>
 * <p>
 * Return type: {@code CompletableFuture<Boolean>}
 * <ul>
 *   <li>{@code true} — email sent successfully.</li>
 *   <li>{@code false} — SMTP or template error. Exception is logged, NOT re-thrown.
 *       This is intentional: email failure should NOT break the main business flow.</li>
 * </ul>
 *
 * @see MailService
 * @see com.mkwang.backend.config.SchedulerConfig#mailExecutor()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${application.mail.from}")
    private String fromAddress;

    // ═══════════════════════════════════════════════════════════
    // GENERIC — process any EmailPayload (used by Redis Worker)
    // ═══════════════════════════════════════════════════════════

    /**
     * Send email from a generic payload. Runs on the dedicated mail thread pool.
     * <p>
     * IMPORTANT: @Async proxy requires this method to be called from OUTSIDE this class.
     * Internal calls (this.sendMail()) will NOT be async. The Redis Worker calls this
     * via the MailService interface → Spring proxy → async execution.
     */
    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendMail(EmailPayload payload) {
        try {
            doSend(payload);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send email: to={}, subject={}, error={}",
                    payload.getTo(), payload.getSubject(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // BUSINESS-SPECIFIC — each builds payload and delegates to doSend()
    // ═══════════════════════════════════════════════════════════

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendWelcomeEmail(String to, String fullName, String tempPassword) {
        EmailPayload payload = EmailPayload.builder()
                .to(to)
                .subject("Chào mừng bạn đến với IFMS — Thông tin đăng nhập")
                .templateName("welcome")
                .variables(Map.of(
                        "fullName", fullName,
                        "email", to,
                        "tempPassword", tempPassword
                ))
                .build();

        return doSendAndReturn(payload);
    }

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendPasswordResetEmail(String to, String fullName, String tempPassword) {
        EmailPayload payload = EmailPayload.builder()
                .to(to)
                .subject("IFMS — Mật khẩu của bạn đã được đặt lại")
                .templateName("password-reset")
                .variables(Map.of(
                        "fullName", fullName,
                        "tempPassword", tempPassword
                ))
                .build();

        return doSendAndReturn(payload);
    }

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendRequestApprovedEmail(String to, String employeeName,
                                                                String requestCode, String requestType,
                                                                String projectName, String approverName,
                                                                String approvedAmount) {
        EmailPayload payload = EmailPayload.builder()
                .to(to)
                .subject("IFMS — Đơn yêu cầu " + requestCode + " đã được duyệt")
                .templateName("request-approved")
                .variables(Map.of(
                        "employeeName", employeeName,
                        "requestCode", requestCode,
                        "requestType", requestType,
                        "projectName", projectName,
                        "approverName", approverName,
                        "approvedAmount", approvedAmount
                ))
                .build();

        return doSendAndReturn(payload);
    }

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendRequestRejectedEmail(String to, String employeeName,
                                                                String requestCode, String requestType,
                                                                String amount, String rejectorName,
                                                                String rejectReason) {
        EmailPayload payload = EmailPayload.builder()
                .to(to)
                .subject("IFMS — Đơn yêu cầu " + requestCode + " bị từ chối")
                .templateName("request-rejected")
                .variables(Map.of(
                        "employeeName", employeeName,
                        "requestCode", requestCode,
                        "requestType", requestType,
                        "amount", amount,
                        "rejectorName", rejectorName,
                        "rejectReason", rejectReason
                ))
                .build();

        return doSendAndReturn(payload);
    }

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Boolean> sendPayslipEmail(String to, String employeeName, String employeeCode,
                                                        String departmentName, String payslipCode,
                                                        String periodName, String paymentDate,
                                                        String baseSalary, String bonus, String allowance,
                                                        String deduction, String advanceDeduct,
                                                        String finalNetSalary) {
        EmailPayload payload = EmailPayload.builder()
                .to(to)
                .subject("IFMS — Phiếu lương " + periodName + " (" + payslipCode + ")")
                .templateName("payslip")
                .variables(Map.ofEntries(
                        Map.entry("employeeName", employeeName),
                        Map.entry("employeeCode", employeeCode),
                        Map.entry("departmentName", departmentName),
                        Map.entry("payslipCode", payslipCode),
                        Map.entry("periodName", periodName),
                        Map.entry("paymentDate", paymentDate),
                        Map.entry("baseSalary", baseSalary),
                        Map.entry("bonus", bonus),
                        Map.entry("allowance", allowance),
                        Map.entry("deduction", deduction),
                        Map.entry("advanceDeduct", advanceDeduct),
                        Map.entry("finalNetSalary", finalNetSalary)
                ))
                .build();

        return doSendAndReturn(payload);
    }

    // ═══════════════════════════════════════════════════════════
    // PRIVATE — shared internal methods
    // ═══════════════════════════════════════════════════════════

    /**
     * Internal helper — sends and wraps result in CompletableFuture.
     * Used by all business-specific methods to avoid duplicating try-catch.
     */
    private CompletableFuture<Boolean> doSendAndReturn(EmailPayload payload) {
        try {
            doSend(payload);
            return CompletableFuture.completedFuture(true);
        } catch (Exception e) {
            log.error("Failed to send email: to={}, subject={}, error={}",
                    payload.getTo(), payload.getSubject(), e.getMessage(), e);
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Core send logic — synchronous within the async thread.
     * <p>
     * Flow:
     * <ol>
     *   <li>Create MimeMessage with MULTIPART_MODE_MIXED_RELATED (for inline images + attachments)</li>
     *   <li>If templateName is set → render Thymeleaf HTML</li>
     *   <li>Otherwise → send plain-text body</li>
     *   <li>If attachment is present → decode Base64 and attach</li>
     *   <li>Send via SMTP</li>
     * </ol>
     *
     * @param payload the email payload
     * @throws Exception if template rendering or SMTP fails
     */
    private void doSend(EmailPayload payload) throws Exception {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper helper = new MimeMessageHelper(
                mimeMessage,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
        );

        helper.setFrom(fromAddress);
        helper.setTo(payload.getTo());
        helper.setSubject(payload.getSubject());

        // ── Render HTML or plain-text ──
        if (payload.getTemplateName() != null && !payload.getTemplateName().isBlank()) {
            String htmlContent = renderTemplate(payload);
            helper.setText(htmlContent, true);
        } else {
            String textBody = payload.getBody() != null ? payload.getBody() : "";
            helper.setText(textBody, false);
        }

        // ── Attach file if present ──
        if (payload.getAttachmentBase64() != null && !payload.getAttachmentBase64().isBlank()) {
            byte[] attachmentBytes = Base64.getDecoder().decode(payload.getAttachmentBase64());
            String contentType = payload.getAttachmentContentType() != null
                    ? payload.getAttachmentContentType()
                    : "application/octet-stream";
            String fileName = payload.getAttachmentName() != null
                    ? payload.getAttachmentName()
                    : "attachment";

            helper.addAttachment(fileName, new ByteArrayResource(attachmentBytes), contentType);
        }

        // ── Send via SMTP ──
        mailSender.send(mimeMessage);
        log.info("✅ Email sent: to={}, subject={}, template={}",
                payload.getTo(), payload.getSubject(), payload.getTemplateName());
    }

    /**
     * Render Thymeleaf template with variables from the payload.
     * Template resolution: classpath:/templates/{templateName}.html
     */
    private String renderTemplate(EmailPayload payload) {
        Context context = new Context();
        if (payload.getVariables() != null) {
            payload.getVariables().forEach(context::setVariable);
        }
        return templateEngine.process(payload.getTemplateName(), context);
    }
}
