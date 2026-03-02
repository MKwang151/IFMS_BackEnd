package com.mkwang.backend.common.utils.strategy;

import com.mkwang.backend.common.utils.BusinessCodeStrategy;
import com.mkwang.backend.common.utils.BusinessCodeType;
import com.mkwang.backend.common.utils.CodeFormatUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

import static com.mkwang.backend.common.utils.CodeFormatUtils.padLeft;

/**
 * Request Code Strategy — Format: REQ-{DEPT}-{MMYY}-{SEQ:03d}
 * <p>
 * Mã đơn từ / Tờ trình — dùng để kế toán đối soát và in PDF.
 * Example: REQ-IT-0326-001, REQ-FIN-0326-002
 * Stored in: requests.request_code
 * Sequence: seq_request_code
 */
@Service
@RequiredArgsConstructor
public class RequestCodeStrategy implements BusinessCodeStrategy {

    private final CodeFormatUtils formatUtils;

    @Override
    public BusinessCodeType getType() {
        return BusinessCodeType.REQUEST;
    }

    /**
     * @param sequence nextval từ seq_request_code
     * @param params   [0] = departmentCode (e.g. "IT", "FIN", "SALES")
     */
    @Override
    public String generate(long sequence, String... params) {
        String deptCode = (params.length > 0 && params[0] != null && !params[0].isBlank())
                ? formatUtils.sanitizeSlug(params[0], 10)
                : "GEN";
        LocalDate now = LocalDate.now();
        String mmyy = padLeft(now.getMonthValue(), 2) + padLeft(now.getYear() % 100, 2);
        return "REQ-" + deptCode + '-' + mmyy + '-' + padLeft(sequence, 3);
    }
}
