package com.mkwang.backend.common.utils;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum phân loại các loại Business Code trong hệ thống IFMS.
 * <p>
 * Enum này CHỈ chứa metadata (tên sequence, có cần sequence hay không).
 * Logic sinh mã nằm trong các class {@code @Service} implement {@link BusinessCodeStrategy}.
 *
 * <h3>Format reference:</h3>
 * <pre>
 * | Type        | Format                        | Example             | Sequence Name         | Needs Seq? |
 * |-------------|-------------------------------|---------------------|-----------------------|------------|
 * | EMPLOYEE    | MK{SEQ:03d}                   | MK008               | seq_employee_code     | ✅          |
 * | DEPARTMENT  | (manual input, normalized)    | IT, FIN, HR         | —                     | ❌          |
 * | PROJECT     | PRJ-{SLUG}-{YYYY}-{SEQ:03d}   | PRJ-ERP-2026-001    | seq_project_code      | ✅          |
 * | PHASE       | PH-{SLUG}-{SEQ:02d}           | PH-UIUX-01         | seq_phase_code        | ✅          |
 * | REQUEST     | REQ-{DEPT}-{MMYY}-{SEQ:03d}   | REQ-IT-0326-001     | seq_request_code      | ✅          |
 * | TRANSACTION | TXN-{8 hex}                   | TXN-8829145A        | —                     | ❌          |
 * | PERIOD      | PR-{YYYY}-{MM}                | PR-2026-03          | —                     | ❌          |
 * | PAYSLIP     | PSL-{EMP_CODE}-{MMYY}         | PSL-MK001-0326      | —                     | ❌          |
 * </pre>
 */
@Getter
@RequiredArgsConstructor
public enum BusinessCodeType {

    EMPLOYEE    ("seq_employee_code", true),
    DEPARTMENT  (null,                false),
    PROJECT     ("seq_project_code",  true),
    PHASE       ("seq_phase_code",    true),
    REQUEST     ("seq_request_code",  true),
    TRANSACTION (null,                false),
    PERIOD      (null,                false),
    PAYSLIP     (null,                false);

    /** Tên PostgreSQL sequence, null nếu không dùng sequence */
    private final String sequenceName;

    /** true = cần gọi nextval() từ DB, false = sinh mã không cần DB */
    private final boolean requiresSequence;
}
