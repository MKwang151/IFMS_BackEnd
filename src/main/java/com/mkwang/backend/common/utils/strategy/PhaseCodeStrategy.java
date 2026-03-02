package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.mkwang.backend.common.utils.CodeFormatUtils.padLeft;

/**
 * PH-{SLUG}-{SEQ:02d} → PH-UIUX-01, PH-DEV-02
 */
@Service
@RequiredArgsConstructor
public class PhaseCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.PHASE;
    }

    @Override
    public String generate(long sequence, String... params) {
        String slug = (params.length > 0 && params[0] != null && !params[0].isBlank())
                ? formatUtils.sanitizeSlug(params[0], 10)
                : "PH";
        return "PH-" + slug + '-' + padLeft(sequence, 2);
    }
}
