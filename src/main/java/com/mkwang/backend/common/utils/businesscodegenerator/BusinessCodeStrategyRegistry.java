package com.mkwang.backend.common.utils.businesscodegenerator;

import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Strategy Registry — gom strategies vào immutable {@link EnumMap}, lookup O(1).
 * <p>
 * Fail-fast khi startup: thiếu hoặc trùng strategy → throw ngay, không để lọt production.
 */
@Component
public class BusinessCodeStrategyRegistry {

    private final Map<BusinessCodeType, BusinessCodeStrategy> strategyMap;

    public BusinessCodeStrategyRegistry(List<BusinessCodeStrategy> strategies) {
        EnumMap<BusinessCodeType, BusinessCodeStrategy> map = new EnumMap<>(BusinessCodeType.class);

        for (BusinessCodeStrategy strategy : strategies) {
            BusinessCodeType type = strategy.getType();
            if (map.containsKey(type)) {
                throw new IllegalStateException(
                        "Duplicate BusinessCodeStrategy for type " + type +
                        ": [" + map.get(type).getClass().getSimpleName() +
                        "] vs [" + strategy.getClass().getSimpleName() + "]"
                );
            }
            map.put(type, strategy);
        }

        for (BusinessCodeType type : BusinessCodeType.values()) {
            if (!map.containsKey(type)) {
                throw new IllegalStateException(
                        "Missing BusinessCodeStrategy for type: " + type);
            }
        }

        // Wrap immutable — enforce read-only sau khi build xong
        this.strategyMap = Collections.unmodifiableMap(map);
    }

    public BusinessCodeStrategy getStrategy(BusinessCodeType type) {
        return strategyMap.get(type);
    }
}
