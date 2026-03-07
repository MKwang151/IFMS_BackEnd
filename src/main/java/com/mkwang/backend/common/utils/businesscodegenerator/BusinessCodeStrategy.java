package com.mkwang.backend.common.utils.businesscodegenerator;

/**
 * Strategy Pattern Interface — Hợp đồng mà tất cả chiến lược sinh Business Code phải tuân thủ.
 * <p>
 * Mỗi class {@code @Service} implement interface này sẽ:
 * <ol>
 *   <li>Tự khai báo loại code mà nó xử lý qua {@link #getType()}</li>
 *   <li>Implement logic sinh mã riêng trong {@link #generate(long, String...)}</li>
 * </ol>
 * <p>
 * Spring sẽ tự collect tất cả implementations vào {@link BusinessCodeStrategyRegistry}
 * thông qua constructor injection ({@code List<BusinessCodeStrategy>}).
 *
 * @see BusinessCodeType
 * @see BusinessCodeStrategyRegistry
 */
public interface BusinessCodeStrategy {

    /**
     * Trả về loại Business Code mà strategy này xử lý.
     * Dùng để Registry đăng ký strategy vào đúng slot trong EnumMap.
     *
     * @return enum constant xác định loại code
     */
    BusinessCodeType getType();

    /**
     * Sinh mã Business Code.
     *
     * @param sequence giá trị nextval() từ PostgreSQL Sequence (0 nếu loại không dùng sequence)
     * @param params   tham số bổ sung tùy loại:
     *                 <ul>
     *                   <li>EMPLOYEE: [prefix?] — default "MK"</li>
     *                   <li>DEPARTMENT: [rawCode] — bắt buộc</li>
     *                   <li>PROJECT: [projectNameSlug]</li>
     *                   <li>PHASE: [phaseNameSlug]</li>
     *                   <li>REQUEST: [departmentCode]</li>
     *                   <li>TRANSACTION: (không cần)</li>
     *                   <li>PERIOD: [year, month]</li>
     *                   <li>PAYSLIP: [employeeCode, month, year]</li>
     *                 </ul>
     * @return mã code đã format, đảm bảo UNIQUE
     */
    String generate(long sequence, String... params);
}

