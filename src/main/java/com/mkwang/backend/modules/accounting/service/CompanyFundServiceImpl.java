package com.mkwang.backend.modules.accounting.service;

import com.mkwang.backend.common.exception.InternalSystemException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.modules.accounting.dto.CompanyFundDto;
import com.mkwang.backend.modules.accounting.dto.ReconciliationReportDto;
import com.mkwang.backend.modules.accounting.dto.SystemTopupRequest;
import com.mkwang.backend.modules.accounting.dto.UpdateBankStatementRequest;
import com.mkwang.backend.modules.accounting.entity.CompanyFund;
import com.mkwang.backend.modules.accounting.repository.CompanyFundRepository;
import com.mkwang.backend.modules.wallet.dto.TransactionDto;
import com.mkwang.backend.modules.wallet.dto.WalletDto;
import com.mkwang.backend.modules.wallet.entity.Transaction;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import com.mkwang.backend.modules.wallet.mapper.WalletMapper;
import com.mkwang.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompanyFundServiceImpl implements CompanyFundService {

    private final CompanyFundRepository companyFundRepository;
    private final WalletService         walletService;
    private final WalletMapper          walletMapper;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('COMPANY_FUND_VIEW')")
    public CompanyFundDto getCompanyFund() {
        CompanyFund fund = loadDefault();
        WalletDto wallet = walletService.getWallet(WalletOwnerType.COMPANY_FUND, 1L);
        return toDto(fund, wallet);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('COMPANY_FUND_TOPUP')")
    public TransactionDto topup(SystemTopupRequest request) {
        Transaction txn = walletService.systemTopup(
                request.getAmount(),
                request.getPaymentRef(),
                request.getDescription()
        );
        return walletMapper.toDto(txn);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('COMPANY_FUND_TOPUP')")
    public CompanyFundDto updateBankStatement(UpdateBankStatementRequest request) {
        CompanyFund fund = loadDefault();
        fund.setExternalBankBalance(request.getExternalBankBalance());
        fund.setLastStatementDate(request.getLastStatementDate());
        fund.setLastStatementUpdatedBy(currentUserId());
        companyFundRepository.save(fund);

        WalletDto wallet = walletService.getWallet(WalletOwnerType.COMPANY_FUND, 1L);
        return toDto(fund, wallet);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('COMPANY_FUND_VIEW')")
    public ReconciliationReportDto getReconciliationReport() {
        CompanyFund fund = loadDefault();
        WalletDto companyFundWallet = walletService.getWallet(WalletOwnerType.COMPANY_FUND, 1L);
        WalletDto floatMainWallet   = walletService.getWallet(WalletOwnerType.FLOAT_MAIN, 0L);

        BigDecimal floatMainBalance  = floatMainWallet.getBalance();
        BigDecimal computedWalletSum = walletService.sumAllBalancesExceptFloatMain();
        BigDecimal systemDiscrepancy = floatMainBalance.subtract(computedWalletSum);

        BigDecimal companyFundBalance   = companyFundWallet.getBalance();
        BigDecimal totalDeptWallets     = walletService.sumBalancesByType(WalletOwnerType.DEPARTMENT);
        BigDecimal totalProjectWallets  = walletService.sumBalancesByType(WalletOwnerType.PROJECT);
        BigDecimal totalUserWallets     = walletService.sumBalancesByType(WalletOwnerType.USER);

        BigDecimal externalBankBalance  = fund.getExternalBankBalance() != null
                ? fund.getExternalBankBalance() : BigDecimal.ZERO;
        BigDecimal bankDiscrepancy      = companyFundBalance.subtract(externalBankBalance);

        return ReconciliationReportDto.builder()
                .generatedAt(LocalDateTime.now())
                // Integrity check
                .floatMainBalance(floatMainBalance)
                .computedWalletSum(computedWalletSum)
                .systemDiscrepancy(systemDiscrepancy)
                .systemIntegrityValid(systemDiscrepancy.compareTo(BigDecimal.ZERO) == 0)
                // Breakdown
                .companyFundBalance(companyFundBalance)
                .totalDeptWallets(totalDeptWallets)
                .totalProjectWallets(totalProjectWallets)
                .totalUserWallets(totalUserWallets)
                // Bank statement
                .externalBankBalance(externalBankBalance)
                .lastStatementDate(fund.getLastStatementDate())
                .bankDiscrepancy(bankDiscrepancy)
                .build();
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private CompanyFund loadDefault() {
        return companyFundRepository.findDefault()
                .orElseThrow(() -> new InternalSystemException("CompanyFund record not found — system not initialized"));
    }

    private CompanyFundDto toDto(CompanyFund fund, WalletDto wallet) {
        String updatedBy = null;
        if (fund.getLastStatementUpdatedBy() != null) {
            updatedBy = "userId:" + fund.getLastStatementUpdatedBy();
        }

        BigDecimal walletBalance  = wallet.getBalance();
        BigDecimal externalBal    = fund.getExternalBankBalance() != null
                ? fund.getExternalBankBalance() : BigDecimal.ZERO;

        return CompanyFundDto.builder()
                .id(fund.getId())
                .bankName(fund.getBankName())
                .bankAccount(fund.getBankAccount())
                .currentWalletBalance(walletBalance)
                .externalBankBalance(externalBal)
                .bankDiscrepancy(walletBalance.subtract(externalBal))
                .lastStatementDate(fund.getLastStatementDate())
                .lastStatementUpdatedBy(updatedBy)
                .build();
    }

    private Long currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof com.mkwang.backend.modules.user.entity.User user) {
            return user.getId();
        }
        return null;
    }
}
