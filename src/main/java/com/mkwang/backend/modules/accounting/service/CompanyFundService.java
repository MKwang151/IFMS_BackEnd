package com.mkwang.backend.modules.accounting.service;

import com.mkwang.backend.modules.accounting.dto.CompanyFundDto;
import com.mkwang.backend.modules.accounting.dto.ReconciliationReportDto;
import com.mkwang.backend.modules.accounting.dto.SystemTopupRequest;
import com.mkwang.backend.modules.accounting.dto.UpdateBankStatementRequest;
import com.mkwang.backend.modules.wallet.dto.TransactionDto;

public interface CompanyFundService {

    /**
     * Get company fund metadata and current wallet balance.
     */
    CompanyFundDto getCompanyFund();

    /**
     * Record an external bank transfer that increases the company fund.
     * Calls WalletService.systemTopup() which also updates FLOAT_MAIN.
     */
    TransactionDto topup(SystemTopupRequest request);

    /**
     * Update the bank statement figures for external reconciliation.
     * Records what the actual bank account shows (entered manually by Accountant).
     */
    CompanyFundDto updateBankStatement(UpdateBankStatementRequest request);

    /**
     * Generate a full reconciliation report:
     * - FLOAT_MAIN invariant check (internal integrity)
     * - Wallet breakdown by type
     * - Bank statement comparison (external reconciliation)
     */
    ReconciliationReportDto getReconciliationReport();
}
