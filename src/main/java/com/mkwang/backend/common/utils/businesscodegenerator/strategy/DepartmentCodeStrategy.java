package com.mkwang.backend.common.utils.businesscodegenerator.strategy;

import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeType;
import com.mkwang.backend.common.utils.businesscodegenerator.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Normalize manual input → IT, FIN, HR01
 */
@Service
@RequiredArgsConstructor
public class DepartmentCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.DEPARTMENT;
    }

    @Override
    public String generate(long sequence, String... params) {
        if (params.length == 0 || params[0] == null || params[0].isBlank()) {
            throw new IllegalArgumentException("Department code must be provided manually");
        }
        return formatUtils.sanitizeSlug(params[0], 20);
    }
}
