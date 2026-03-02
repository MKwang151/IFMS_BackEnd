package com.mkwang.backend.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Facade duy nhất sinh Business Code — delegate cho Strategy + SequenceService.
 * <p>
 * Usage:
 * <pre>{@code
 * @Autowired private BusinessCodeGenerator codeGen;
 *
 * codeGen.generateEmployeeCode();              // MK008
 * codeGen.generateProjectCode("ERP");          // PRJ-ERP-2026-001
 * codeGen.generateRequestCode("IT");           // REQ-IT-0326-001
 * codeGen.generateTransactionCode();           // TXN-8829145A
 * codeGen.generatePeriodCode(2026, 3);         // PR-2026-03
 * codeGen.generatePayslipCode("MK001", 3, 2026); // PSL-MK001-0326
 * }</pre>
 */
@Component
@RequiredArgsConstructor
public class BusinessCodeGenerator {

    private final SequenceService sequenceService;
    private final BusinessCodeStrategyRegistry registry;

    /**
     * Core method — lookup strategy → fetch sequence if needed → generate code.
     */
    public String generate(BusinessCodeType type, String... params) {
        long sequence = type.isRequiresSequence()
                ? sequenceService.getNextValue(type)
                : 0L;
        return registry.getStrategy(type).generate(sequence, params);
    }

    // ── Convenience wrappers (strongly-typed) ─────────────────────

    public String generateEmployeeCode() {
        return generate(BusinessCodeType.EMPLOYEE);
    }

    public String generateEmployeeCode(String prefix) {
        return generate(BusinessCodeType.EMPLOYEE, prefix);
    }

    public String normalizeDepartmentCode(String rawCode) {
        return generate(BusinessCodeType.DEPARTMENT, rawCode);
    }

    public String generateProjectCode(String projectNameSlug) {
        return generate(BusinessCodeType.PROJECT, projectNameSlug);
    }

    public String generatePhaseCode(String phaseNameSlug) {
        return generate(BusinessCodeType.PHASE, phaseNameSlug);
    }

    public String generateRequestCode(String departmentCode) {
        return generate(BusinessCodeType.REQUEST, departmentCode);
    }

    public String generateTransactionCode() {
        return generate(BusinessCodeType.TRANSACTION);
    }

    public String generatePeriodCode(int year, int month) {
        return generate(BusinessCodeType.PERIOD, String.valueOf(year), String.valueOf(month));
    }

    public String generatePeriodCode() {
        return generate(BusinessCodeType.PERIOD);
    }

    public String generatePayslipCode(String employeeCode, int month, int year) {
        return generate(BusinessCodeType.PAYSLIP, employeeCode, String.valueOf(month), String.valueOf(year));
    }
}
