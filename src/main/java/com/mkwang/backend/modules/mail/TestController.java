package com.mkwang.backend.modules.mail;

import com.mkwang.backend.modules.mail.consumers.MailEvent;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TestController {

    @Value("${spring.rabbitmq.mail.exchange}")
    String exchange;
    @Value("${spring.rabbitmq.mail.onboard.routing-key}")
    String routingKey;
    final RabbitTemplate rabbitTemplate;

    @PostMapping("/test")
    public ResponseEntity<String> test(
            @RequestBody MailEvent mail
    ) {
        rabbitTemplate.convertAndSend(exchange, routingKey, mail);
        return ResponseEntity
                .status(200)
                .body("Message sent: " + mail);
    }
}
