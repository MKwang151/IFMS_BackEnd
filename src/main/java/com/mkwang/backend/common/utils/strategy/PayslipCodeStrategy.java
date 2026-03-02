package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.mkwang.backend.common.utils.CodeFormatUtils.padLeft;

/**
 * Payslip Code Strategy — Format: PSL-{EMP_CODE}-{MMYY}
 * <p>
 * Mã phiếu lương cá nhân — dùng làm mã chứng từ khi xuất PDF.
 * Deterministic: sinh từ employeeCode + month + year, không cần sequence.
 * <p>
 * Example: PSL-MK001-0326, PSL-MK002-0326
 * Stored in: payslips.payslip_code
 * Sequence: Không dùng (deterministic)
 */
@Service
@RequiredArgsConstructor
public class PayslipCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.PAYSLIP;
    }

    /**
     * @param sequence ignored (không dùng sequence)
     * @param params   [0] = employeeCode (e.g. "MK001"),
     *                 [1] = month (String, e.g. "3"),
     *                 [2] = year (String, e.g. "2026")
     */
    @Override
    public String generate(long sequence, String... params) {
        if (params.length < 3 || params[0] == null || params[1] == null || params[2] == null) {
            throw new IllegalArgumentException("Payslip requires: employeeCode, month, year");
        }
        String empCode = formatUtils.sanitizeSlug(params[0], 10);
        int month = Integer.parseInt(params[1]);
        int year = Integer.parseInt(params[2]);
        // "PSL-" + empCode + "-" + MM + YY
        return "PSL-" + empCode + '-' + padLeft(month, 2) + padLeft(year % 100, 2);
    }
}
