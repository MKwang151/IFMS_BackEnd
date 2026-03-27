package com.mkwang.backend.modules.mail.consumers;

import com.mkwang.backend.modules.mail.service.BrevoMailService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * MailConsumer — nhận message từ RabbitMQ và gửi email qua Brevo.
 * <p>
 * Retry flow:
 * <ol>
 *   <li>Brevo call thất bại → Spring AMQP Retry tự thử lại (3 lần, exponential backoff 3s→9s)</li>
 *   <li>Hết 3 lần → NACK → RabbitMQ route sang DLQ</li>
 *   <li>DLQ consumer log WARN để alert/monitor</li>
 * </ol>
 * concurrency="2-5": Spring AMQP tạo từ 2 đến 5 listener thread tự động.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MailConsumer {

    BrevoMailService mailService;

    // ── Main listener ────────────────────────────────────────────

    @RabbitListener(
            queues = "${spring.rabbitmq.mail.onboard.queue}",
            concurrency = "2-5"
    )
    public void consumeOnBoard(TestMail email) {
        log.debug("[MailConsumer] Received onboard email for: {}", email.to());
        boolean success = mailService.sendOnBoard(email.to(), email.subject(), email.content());
        if (!success) {
            // Throw để trigger Spring AMQP retry → sau 3 lần sẽ NACK → DLQ
            throw new RuntimeException("Brevo send failed for: " + email.to());
        }
    }

    // ── DLQ listener ─────────────────────────────────────────────

    @RabbitListener(queues = "${spring.rabbitmq.mail.onboard.dlq}")
    public void consumeOnBoardDLQ(Message rawMessage) {
        log.warn("[MailConsumer][DLQ] Onboard email FAILED after all retries. " +
                        "messageId={}, body={}",
                rawMessage.getMessageProperties().getMessageId(),
                new String(rawMessage.getBody()));
        // TODO: có thể persist vào bảng failed_email_log hoặc gửi alert cho admin
    }
}

