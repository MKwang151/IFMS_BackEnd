package com.mkwang.backend.modules.mail.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MailRabbitMQConfig — khai báo Exchange, Queue, Binding cho mail service.
 * <p>
 * Topology:
 * <pre>
 *   mailExchange  ──(mailOnboard)──► mailOnboardQueue  ──(on reject)──► mailDLX ──► mailOnboardDLQ
 * </pre>
 * Main queue được gắn x-dead-letter-exchange: khi message bị reject sau khi
 * RetryTemplate hết số lần thử, RabbitMQ tự route sang DLX → DLQ.
 */
@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailRabbitMQConfig {

    // ── Main exchange & queue ────────────────────────────────────
    @Value("${spring.rabbitmq.mail.exchange}")
    String exchange;

    @Value("${spring.rabbitmq.mail.onboard.queue}")
    String onBoardQueue;

    @Value("${spring.rabbitmq.mail.onboard.routing-key}")
    String onBoardRoutingKey;

    // ── Dead Letter Exchange & Queue ─────────────────────────────
    @Value("${spring.rabbitmq.mail.dlx}")
    String dlx;

    @Value("${spring.rabbitmq.mail.onboard.dlq}")
    String onBoardDLQ;

    @Value("${spring.rabbitmq.mail.onboard.dlq-routing-key}")
    String onBoardDLQRoutingKey;

    // ── Beans: Main ──────────────────────────────────────────────

    @Bean
    public TopicExchange mailExchange() {
        return new TopicExchange(exchange);
    }

    /**
     * Main queue với dead-letter args — khi message bị NACK/reject,
     * RabbitMQ tự forward sang mailDLX với routing key mailOnboardDead.
     */
    @Bean
    public Queue onBoardQueue() {
        return QueueBuilder.durable(onBoardQueue)
                .withArgument("x-dead-letter-exchange", dlx)
                .withArgument("x-dead-letter-routing-key", onBoardDLQRoutingKey)
                .build();
    }

    @Bean
    public Binding onBoardBinding() {
        return BindingBuilder
                .bind(onBoardQueue())
                .to(mailExchange())
                .with(onBoardRoutingKey);
    }

    // ── Beans: DLX / DLQ ────────────────────────────────────────

    @Bean
    public DirectExchange mailDLX() {
        return new DirectExchange(dlx);
    }

    @Bean
    public Queue onBoardDLQ() {
        return QueueBuilder.durable(onBoardDLQ).build();
    }

    @Bean
    public Binding onBoardDLQBinding() {
        return BindingBuilder
                .bind(onBoardDLQ())
                .to(mailDLX())
                .with(onBoardDLQRoutingKey);
    }
}

