package com.mkwang.backend.modules.config.dto;

import com.mkwang.backend.modules.config.entity.SystemConfig;

import java.time.LocalDateTime;

/**
 * Response DTO cho SystemConfig.
 */
public record SystemConfigDto(
        String key,
        String value,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static SystemConfigDto from(SystemConfig config) {
        return new SystemConfigDto(
                config.getKey(),
                config.getValue(),
                config.getDescription(),
                config.getCreatedAt(),
                config.getUpdatedAt()
        );
    }
}
