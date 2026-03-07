package com.mkwang.backend.modules.mail.service;

import com.mkwang.backend.modules.mail.dto.EmailPayload;

import java.util.concurrent.CompletableFuture;

/**
 * MailService — Facade interface hiding the complexity of email sending.
 * <p>
 * Facade Pattern rationale:
 * <ul>
 *   <li>Consumers only interact with high-level methods — they don't need to know about
 *       JavaMailSender, Thymeleaf, MimeMessage, or SMTP internals.</li>
 *   <li>Implementation can be swapped (e.g. from Gmail SMTP to AWS SES) without changing callers.</li>
 *   <li>Testable — mock this interface in unit tests for modules that trigger emails.</li>
 * </ul>
 * <p>
 * All methods return {@link CompletableFuture CompletableFuture&lt;Boolean&gt;}:
 * <ul>
 *   <li>{@code true} — email sent successfully via SMTP.</li>
 *   <li>{@code false} — email sending failed (exception logged internally).</li>
 * </ul>
 * This allows callers to either fire-and-forget or chain callbacks for error handling.
 *
 * @see MailServiceImpl
 */
public interface MailService {

    // ═══════════════════════════════════════════════════════════
    // GENERIC — used by EmailRedisWorker to process any queued payload
    // ═══════════════════════════════════════════════════════════

    /**
     * Send an email based on a generic {@link EmailPayload}.
     * <p>
     * Supports both HTML template and plain-text modes.
     * Called by the Redis Worker to process queued email jobs.
     *
     * @param payload the email payload (to, subject, template, variables, attachment)
     * @return CompletableFuture&lt;Boolean&gt; — true if sent, false if failed
     */
    CompletableFuture<Boolean> sendMail(EmailPayload payload);

    // ═══════════════════════════════════════════════════════════
    // BUSINESS-SPECIFIC — called directly by other modules (fire-and-forget)
    // These methods build the EmailPayload internally and send via @Async.
    // ═══════════════════════════════════════════════════════════

    /**
     * Send welcome email with temporary password to a newly created user.
     * <p>
     * Triggered by: Admin User Management → POST /admin/users (create user)
     *
     * @param to           recipient email
     * @param fullName     user's display name
     * @param tempPassword temporary password (plain text, before BCrypt)
     * @return CompletableFuture&lt;Boolean&gt;
     */
    CompletableFuture<Boolean> sendWelcomeEmail(String to, String fullName, String tempPassword);

    /**
     * Send password reset email with temporary credentials.
     * <p>
     * Triggered by: POST /api/v1/auth/forgot-password
     *               POST /admin/users/:id/reset-password
     *
     * @param to           recipient email
     * @param fullName     user's display name
     * @param tempPassword new temporary password
     * @return CompletableFuture&lt;Boolean&gt;
     */
    CompletableFuture<Boolean> sendPasswordResetEmail(String to, String fullName, String tempPassword);

    /**
     * Send notification that a request has been approved.
     * <p>
     * Triggered by: Request approval flow (Manager/Admin approves)
     *
     * @param to             recipient email
     * @param employeeName   employee's display name
     * @param requestCode    request code (e.g. REQ-IT-0326-001)
     * @param requestType    type label (Tạm ứng / Chi phí / Hoàn ứng)
     * @param projectName    related project name
     * @param approverName   who approved it
     * @param approvedAmount formatted amount string (e.g. "5,000,000")
     * @return CompletableFuture&lt;Boolean&gt;
     */
    CompletableFuture<Boolean> sendRequestApprovedEmail(String to, String employeeName,
                                                        String requestCode, String requestType,
                                                        String projectName, String approverName,
                                                        String approvedAmount);

    /**
     * Send notification that a request has been rejected.
     * <p>
     * Triggered by: Request rejection flow (Manager/Admin rejects)
     *
     * @param to            recipient email
     * @param employeeName  employee's display name
     * @param requestCode   request code
     * @param requestType   type label
     * @param amount        formatted requested amount
     * @param rejectorName  who rejected it
     * @param rejectReason  mandatory rejection reason
     * @return CompletableFuture&lt;Boolean&gt;
     */
    CompletableFuture<Boolean> sendRequestRejectedEmail(String to, String employeeName,
                                                        String requestCode, String requestType,
                                                        String amount, String rejectorName,
                                                        String rejectReason);

    /**
     * Send payslip email with salary breakdown.
     * <p>
     * Triggered by: Payroll execution (Accountant runs payroll)
     *
     * @param to               recipient email
     * @param employeeName     employee's display name
     * @param employeeCode     employee code (e.g. MK-0001)
     * @param departmentName   department name
     * @param payslipCode      payslip code (e.g. PSL-EMP001-0226)
     * @param periodName       period name (e.g. Lương T02/2026)
     * @param paymentDate      formatted date (e.g. 05/03/2026)
     * @param baseSalary       formatted base salary
     * @param bonus            formatted bonus
     * @param allowance        formatted allowance
     * @param deduction        formatted deduction (negative)
     * @param advanceDeduct    formatted advance deduction (negative)
     * @param finalNetSalary   formatted net salary
     * @return CompletableFuture&lt;Boolean&gt;
     */
    CompletableFuture<Boolean> sendPayslipEmail(String to, String employeeName, String employeeCode,
                                                String departmentName, String payslipCode,
                                                String periodName, String paymentDate,
                                                String baseSalary, String bonus, String allowance,
                                                String deduction, String advanceDeduct,
                                                String finalNetSalary);
}
