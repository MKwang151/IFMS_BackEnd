package com.mkwang.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration — uses StringRedisSerializer for both key and value.
 * <p>
 * WHY String serializer instead of JdkSerializationRedisSerializer?
 * <ul>
 *   <li><b>Human-readable:</b> Stored data in Redis is plain JSON strings,
 *       debuggable with redis-cli or RedisInsight.</li>
 *   <li><b>No class-path coupling:</b> JDK serializer embeds Java class metadata
 *       into the bytes — any class rename/move breaks deserialization.</li>
 *   <li><b>Cross-language:</b> Other services (Node.js, Python) can read/write
 *       the same Redis queue if needed.</li>
 *   <li><b>Smaller payload:</b> JSON string is ~30-50% smaller than JDK serialized bytes.</li>
 * </ul>
 * <p>
 * JSON serialization/deserialization is handled explicitly via Jackson ObjectMapper
 * in the Producer and Worker classes, giving us full control over the format.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate<String, String> — serialize everything as plain strings.
     * <p>
     * The Producer will serialize EmailPayload → JSON String → LPUSH.
     * The Worker will RPOP → JSON String → deserialize to EmailPayload.
     *
     * @param connectionFactory auto-configured by Spring Boot from application.yml
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Both key and value use String serializer
        StringRedisSerializer serializer = new StringRedisSerializer();
        template.setKeySerializer(serializer);
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(serializer);
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }
}

