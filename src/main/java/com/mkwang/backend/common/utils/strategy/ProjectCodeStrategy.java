package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.mkwang.backend.common.utils.CodeFormatUtils.padLeft;

/**
 * Project Code Strategy — Format: PRJ-{SLUG}-{YYYY}-{SEQ:03d}
 * <p>
 * Example: PRJ-ERP-2026-001, PRJ-CRM-2026-002
 * Stored in: projects.project_code
 * Sequence: seq_project_code
 */
@Service
@RequiredArgsConstructor
public class ProjectCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.PROJECT;
    }

    /**
     * @param sequence nextval từ seq_project_code
     * @param params   [0] = projectNameSlug (e.g. "ERP", "HRMS", "CRM")
     */
    @Override
    public String generate(long sequence, String... params) {
        String slug = (params.length > 0 && params[0] != null && !params[0].isBlank())
                ? formatUtils.sanitizeSlug(params[0], 10)
                : "GEN";
        // "PRJ-" + slug + "-" + year + "-" + padded seq
        return "PRJ-" + slug + '-' + LocalDate.now().getYear() + '-' + padLeft(sequence, 3);
    }
}
