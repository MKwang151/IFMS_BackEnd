package com.mkwang.backend.modules.mail.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * EmailPayload — DTO representing a single email job to be queued in Redis.
 * <p>
 * This object is serialized to JSON → pushed to Redis List (LPUSH).
 * The Worker pops it (RPOP) → deserializes → sends via JavaMailSender.
 * <p>
 * Design decisions:
 * <ul>
 *   <li><b>templateName:</b> Maps to a Thymeleaf template in classpath:/templates/
 *       (e.g. "payslip" → payslip.html). If null, sends plain-text email.</li>
 *   <li><b>variables:</b> Key-value pairs injected into the Thymeleaf template context.</li>
 *   <li><b>attachmentBase64:</b> Base64-encoded file bytes for small attachments (&lt;500KB).
 *       For large files, store in Cloudinary and include download URL in template variables.</li>
 *   <li><b>retryCount:</b> Tracks how many times this job has been retried.
 *       Managed by the Worker — incremented on failure, checked against max-retry config.</li>
 * </ul>
 *
 * @see com.mkwang.backend.modules.mail.producer.EmailRedisProducer
 * @see com.mkwang.backend.modules.mail.worker.EmailRedisWorker
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmailPayload {

    /**
     * Recipient email address.
     * Required — must be a valid email.
     */
    private String to;

    /**
     * Email subject line.
     * Required.
     */
    private String subject;

    /**
     * Thymeleaf template name (without .html extension).
     * e.g. "payslip", "request-approved", "welcome".
     * If null, the email will be sent as plain text using the 'body' field.
     */
    private String templateName;

    /**
     * Plain-text body — used when templateName is null.
     * Ignored if templateName is provided.
     */
    private String body;

    /**
     * Template variables — injected into Thymeleaf context.
     * e.g. {"employeeName": "Nguyen Van A", "netSalary": "15,000,000 VND"}
     */
    private Map<String, Object> variables;

    /**
     * Attachment file name (with extension).
     * e.g. "payslip-EMP001-0226.pdf"
     * Null if no attachment.
     */
    private String attachmentName;

    /**
     * Base64-encoded attachment content.
     * <p>
     * WHY Base64 instead of raw byte[]?
     * - JSON serialization with Jackson handles String natively.
     * - Avoids binary encoding issues in Redis String values.
     * - Base64 overhead (~33%) is acceptable for attachments &lt;500KB.
     * <p>
     * For large files (&gt;500KB), prefer storing in Cloudinary and sending
     * a download link in the template variables instead.
     */
    private String attachmentBase64;

    /**
     * MIME type of the attachment.
     * e.g. "application/pdf", "image/png"
     * Required if attachmentBase64 is not null.
     */
    private String attachmentContentType;

    /**
     * Retry counter — managed by EmailRedisWorker.
     * Starts at 0. Incremented on each failed send attempt.
     * When retryCount >= max-retry config, message goes to Dead Letter Queue.
     */
    @Builder.Default
    private int retryCount = 0;
}

