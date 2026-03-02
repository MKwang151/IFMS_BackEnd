package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.mkwang.backend.common.utils.CodeFormatUtils.padLeft;

/**
 * MK{SEQ:03d} → MK008, MK009, MK100
 */
@Service
@RequiredArgsConstructor
public class EmployeeCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.EMPLOYEE;
    }

    @Override
    public String generate(long sequence, String... params) {
        String prefix = (params.length > 0 && params[0] != null && !params[0].isBlank())
                ? formatUtils.sanitizeSlug(params[0], 5)
                : "MK";
        return prefix + padLeft(sequence, 3);
    }
}
