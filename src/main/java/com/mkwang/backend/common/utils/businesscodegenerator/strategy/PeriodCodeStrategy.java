package com.mkwang.backend.common.utils.businesscodegenerator.strategy;

import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeType;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.mkwang.backend.common.utils.businesscodegenerator.CodeFormatUtils.padLeft;

/**
 * PR-{YYYY}-{MM} → PR-2026-03 (deterministic, no DB call)
 */
@Service
public class PeriodCodeStrategy implements BusinessCodeStrategy {

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.PERIOD;
    }

    @Override
    public String generate(long sequence, String... params) {
        int year, month;
        if (params.length >= 2 && params[0] != null && params[1] != null) {
            year = Integer.parseInt(params[0]);
            month = Integer.parseInt(params[1]);
            if (month < 1 || month > 12) {
                throw new IllegalArgumentException("Month must be between 1 and 12, got: " + month);
            }
        } else {
            LocalDate now = LocalDate.now();
            year = now.getYear();
            month = now.getMonthValue();
        }
        return "PR-" + year + '-' + padLeft(month, 2);
    }
}
