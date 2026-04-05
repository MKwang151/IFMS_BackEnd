package com.mkwang.backend.config;

import com.mkwang.backend.modules.accounting.entity.CompanyFund;
import com.mkwang.backend.modules.accounting.repository.CompanyFundRepository;
import com.mkwang.backend.modules.config.entity.SystemConfig;
import com.mkwang.backend.modules.config.repository.SystemConfigRepository;
import com.mkwang.backend.modules.expense.entity.ExpenseCategory;
import com.mkwang.backend.modules.expense.repository.ExpenseCategoryRepository;
import com.mkwang.backend.modules.organization.entity.Department;
import com.mkwang.backend.modules.organization.repository.DepartmentRepository;
import com.mkwang.backend.modules.profile.entity.UserProfile;
import com.mkwang.backend.modules.user.entity.*;
import com.mkwang.backend.modules.user.repository.RoleRepository;
import com.mkwang.backend.modules.user.repository.UserRepository;
import com.mkwang.backend.modules.wallet.entity.Wallet;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import com.mkwang.backend.modules.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * DataInitializer – Seeds all reference/master data required for the IFMS system.
 * Idempotent: safe to run on every startup. Only creates data that does not already exist.
 *
 * Seeding order (respects FK constraints):
 *   1. Roles + Permissions (EMPLOYEE, TEAM_LEADER, MANAGER, ACCOUNTANT, ADMIN)
 *   2. Departments (manager_id = null initially, updated after users are created)
 *   3. Users (role + department assigned)
 *   4. Update Department managers
 *   5. UserProfiles
 *   6. Wallets
 *   7. CompanyFund + Wallets (COMPANY_FUND + FLOAT_MAIN)
 *   8. SystemConfig
 *   9. ExpenseCategories (system defaults)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository            roleRepository;
    private final UserRepository            userRepository;
    private final DepartmentRepository      departmentRepository;
    private final WalletRepository          walletRepository;
    private final CompanyFundRepository     companyFundRepository;
    private final SystemConfigRepository    systemConfigRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final PasswordEncoder           passwordEncoder;

    private static final String DEFAULT_PASSWORD     = "Ifms@2026";
    private static final BigDecimal INITIAL_FUND     = new BigDecimal("50000000000"); // 50 tỷ VND

    // =========================================================
    // ENTRY POINT
    // =========================================================
    @Override
    @Transactional
    public void run(String... args) {
        log.info("╔══════════════════════════════════════════╗");
        log.info("║      🚀  IFMS DataInitializer START      ║");
        log.info("╚══════════════════════════════════════════╝");

        initRoles();
        initDepartments();
        initUsers();
        initCompanyFund();
        initSystemConfigs();
        initExpenseCategories();

        log.info("╔══════════════════════════════════════════╗");
        log.info("║   ✅  DataInitializer completed OK       ║");
        log.info("╚══════════════════════════════════════════╝");
    }

    // =========================================================
    // 1. ROLES & PERMISSIONS
    // =========================================================
    private void initRoles() {
        log.info("── [1/5] Seeding Roles ...");

        // EMPLOYEE – Nhân viên
        createRoleIfNotExists("EMPLOYEE", "Nhân viên – tạo yêu cầu, xem ví cá nhân", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD
        ));

        // TEAM_LEADER – Quản lý dự án (duyệt chi tiêu cá nhân — Luồng 1)
        createRoleIfNotExists("TEAM_LEADER", "Quản lý dự án – duyệt MỌI chi tiêu Member, chia budget Phase/Category", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD,
                // Team Leader-specific
                Permission.REQUEST_APPROVE_TEAM_LEADER,
                Permission.PROJECT_CATEGORY_MANAGE,
                Permission.PROJECT_BUDGET_ALLOCATE,
                Permission.PROJECT_PHASE_MANAGE,
                Permission.PROJECT_MEMBER_MANAGE
        ));

        // MANAGER – Trưởng phòng (duyệt cấp vốn dự án — Luồng 2)
        createRoleIfNotExists("MANAGER", "Trưởng phòng – tạo dự án, chỉ định Team Leader, duyệt cấp vốn dự án", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD,
                // Manager-specific
                Permission.PROJECT_CREATE,
                Permission.PROJECT_UPDATE,
                Permission.PROJECT_STATUS_MANAGE,
                Permission.PROJECT_ASSIGN_LEADER,
                Permission.REQUEST_VIEW_DEPT,
                Permission.REQUEST_APPROVE_PROJECT_TOPUP,
                Permission.REQUEST_REJECT,
                Permission.DEPT_VIEW_DASHBOARD
        ));

        // ACCOUNTANT – Kế toán (daily ops: payout, payroll, fund monitoring)
        createRoleIfNotExists("ACCOUNTANT", "Kế toán – giải ngân, chi lương, theo dõi quỹ", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD,
                // Accountant-specific
                Permission.PROJECT_VIEW_ALL,
                Permission.REQUEST_VIEW_APPROVED,
                Permission.REQUEST_PAYOUT,
                Permission.TRANSACTION_APPROVE_WITHDRAW,
                Permission.PAYROLL_MANAGE,
                Permission.PAYROLL_EXECUTE,
                Permission.COMPANY_FUND_VIEW,
                Permission.COMPANY_FUND_TOPUP
        ));

        // CFO – Giám đốc Tài chính (financial governance: approve Flow 3, global dashboard)
        createRoleIfNotExists("CFO", "Giám đốc Tài chính – duyệt cấp quota phòng ban, giám sát dòng tiền toàn công ty", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD,
                // CFO-specific: financial governance
                Permission.PROJECT_VIEW_ALL,
                Permission.REQUEST_VIEW_ALL,
                Permission.REQUEST_APPROVE_DEPT_TOPUP,
                Permission.REQUEST_REJECT,
                Permission.TRANSACTION_APPROVE_WITHDRAW,
                Permission.COMPANY_FUND_VIEW,
                Permission.COMPANY_FUND_TOPUP,
                Permission.DEPT_BUDGET_ALLOCATE,
                Permission.DASHBOARD_VIEW_GLOBAL
        ));

        // ADMIN – Quản trị hệ thống (IAM, org structure, system config — NO financial approvals)
        createRoleIfNotExists("ADMIN", "Quản trị viên – cấu hình hệ thống, quản lý tài khoản & tổ chức", Set.of(
                Permission.USER_PROFILE_VIEW,
                Permission.USER_PROFILE_UPDATE,
                Permission.USER_PIN_UPDATE,
                Permission.NOTIFICATION_VIEW,
                Permission.WALLET_VIEW_SELF,
                Permission.WALLET_DEPOSIT,
                Permission.WALLET_WITHDRAW,
                Permission.WALLET_TRANSACTION_VIEW,
                Permission.PROJECT_VIEW_ACTIVE,
                Permission.REQUEST_CREATE,
                Permission.REQUEST_VIEW_SELF,
                Permission.PAYROLL_VIEW_SELF,
                Permission.PAYROLL_DOWNLOAD,
                // Admin-specific: IAM & system config
                Permission.USER_VIEW_LIST,
                Permission.USER_CREATE,
                Permission.USER_UPDATE,
                Permission.USER_LOCK,
                Permission.ROLE_MANAGE,
                Permission.DEPT_MANAGE,
                Permission.SYSTEM_CONFIG_MANAGE,
                Permission.AUDIT_LOG_VIEW
        ));
    }

    // =========================================================
    // 2. DEPARTMENTS
    // =========================================================
    private void initDepartments() {
        log.info("── [2/5] Seeding Departments ...");

        createDeptIfNotExists("Ban Giám Đốc",          "BGD",  new BigDecimal("5000000000"),  new BigDecimal("5000000000"));
        createDeptIfNotExists("Phòng Công Nghệ",        "IT",   new BigDecimal("20000000000"), new BigDecimal("20000000000"));
        createDeptIfNotExists("Phòng Kế Toán – Tài Chính", "FIN", new BigDecimal("10000000000"), new BigDecimal("10000000000"));
        createDeptIfNotExists("Phòng Kinh Doanh",       "SALES", new BigDecimal("15000000000"), new BigDecimal("15000000000"));
    }

    // =========================================================
    // 3. USERS  →  then link managers to departments
    // =========================================================
    private void initUsers() {
        log.info("── [3/5] Seeding Users, Profiles & Wallets ...");

        Role adminRole      = roleRepository.findByName("ADMIN").orElseThrow();
        Role cfoRole        = roleRepository.findByName("CFO").orElseThrow();
        Role managerRole    = roleRepository.findByName("MANAGER").orElseThrow();
        Role teamLeaderRole = roleRepository.findByName("TEAM_LEADER").orElseThrow();
        Role accountantRole = roleRepository.findByName("ACCOUNTANT").orElseThrow();
        Role employeeRole   = roleRepository.findByName("EMPLOYEE").orElseThrow();

        Department bgd   = departmentRepository.findByCode("BGD").orElseThrow();
        Department it    = departmentRepository.findByCode("IT").orElseThrow();
        Department fin   = departmentRepository.findByCode("FIN").orElseThrow();
        Department sales = departmentRepository.findByCode("SALES").orElseThrow();

        // ---- ADMIN (system config only) ----
        User admin = createUserIfNotExists(
                "admin@ifms.vn", "MK000", "Phạm Thị Thanh Hà",
                adminRole, bgd,
                "System Administrator", "0901000001", "Hà Nội",
                "VCB", "0011004000001", "PHAM THI THANH HA"
        );

        User supportAdmin = createUserIfNotExists(
                "ifms.support.noreply@gmail.com", "MK999", "System Support",
                adminRole, it,
                "System Admin", "0999999999", "Hà Nội",
                "VCB", "0999999999999", "SYSTEM SUPPORT"
        );

        // ---- CFO (financial governance) ----
        User cfo = createUserIfNotExists(
                "cfo@ifms.vn", "MK010", "Nguyễn Văn Minh",
                cfoRole, bgd,
                "Chief Financial Officer", "0901000010", "Hà Nội",
                "VCB", "0011004000010", "NGUYEN VAN MINH"
        );

        // ---- ACCOUNTANT ----
        User accountant = createUserIfNotExists(
                "accountant@ifms.vn", "MK001", "Lê Văn Cường",
                accountantRole, fin,
                "Chief Accountant", "0901000002", "Hà Nội",
                "MBBank", "0011004000002", "LE VAN CUONG"
        );

        // ---- MANAGERS ----
        User managerIT = createUserIfNotExists(
                "manager.it@ifms.vn", "MK002", "Trần Thị Bích",
                managerRole, it,
                "IT Manager", "0901000003", "Hà Nội",
                "Techcombank", "0011004000003", "TRAN THI BICH"
        );

        User managerSales = createUserIfNotExists(
                "manager.sales@ifms.vn", "MK003", "Nguyễn Hồng Sơn",
                managerRole, sales,
                "Sales Manager", "0901000004", "TP.HCM",
                "ACB", "0011004000004", "NGUYEN HONG SON"
        );

        // ---- TEAM LEADERS ----
        User teamLeadIT = createUserIfNotExists(
                "tl.it@ifms.vn", "MK008", "Hoàng Minh Tuấn",
                teamLeaderRole, it,
                "Technical Lead", "0901000009", "Hà Nội",
                "VCB", "0011004000009", "HOANG MINH TUAN"
        );

        // ---- EMPLOYEES ----
        createUserIfNotExists(
                "emp.it1@ifms.vn", "MK004", "Đỗ Quốc Bảo",
                employeeRole, it,
                "Senior Backend Developer", "0901000005", "Hà Nội",
                "VCB", "0011004000005", "DO QUOC BAO"
        );

        createUserIfNotExists(
                "emp.it2@ifms.vn", "MK005", "Vũ Thị Lan",
                employeeRole, it,
                "Frontend Developer", "0901000006", "Hà Nội",
                "MBBank", "0011004000006", "VU THI LAN"
        );

        createUserIfNotExists(
                "emp.sales1@ifms.vn", "MK006", "Phạm Văn Đức",
                employeeRole, sales,
                "Sales Executive", "0901000007", "TP.HCM",
                "Techcombank", "0011004000007", "PHAM VAN DUC"
        );

        createUserIfNotExists(
                "emp.fin1@ifms.vn", "MK007", "Nguyễn Thị Minh",
                employeeRole, fin,
                "Junior Accountant", "0901000008", "Hà Nội",
                "VCB", "0011004000008", "NGUYEN THI MINH"
        );

        // Gán manager cho từng phòng ban (sau khi user đã được persist)
        assignManagerToDept(bgd,   admin);
        assignManagerToDept(fin,   accountant);
        assignManagerToDept(it,    managerIT);
        assignManagerToDept(sales, managerSales);
    }

    // =========================================================
    // 4. COMPANY FUND + WALLETS (COMPANY_FUND + FLOAT_MAIN)
    // =========================================================
    private void initCompanyFund() {
        log.info("── [4/5] Seeding CompanyFund + system wallets ...");

        // 4a. CompanyFund metadata record (singleton id=1)
        if (companyFundRepository.count() == 0) {
            CompanyFund fund = CompanyFund.builder()
                    .bankName("Vietcombank")
                    .bankAccount("0011004999999")
                    .externalBankBalance(INITIAL_FUND)
                    .build();
            companyFundRepository.save(fund);
            log.info("   💰 CompanyFund metadata created");
        } else {
            log.info("   💰 CompanyFund already exists, skipping.");
        }

        // 4b. Wallet(COMPANY_FUND, ownerId=1) — company's operational cash
        if (!walletRepository.existsByOwnerTypeAndOwnerId(WalletOwnerType.COMPANY_FUND, 1L)) {
            walletRepository.save(Wallet.builder()
                    .ownerType(WalletOwnerType.COMPANY_FUND)
                    .ownerId(1L)
                    .balance(INITIAL_FUND)
                    .lockedBalance(BigDecimal.ZERO)
                    .build());
            log.info("   💰 Wallet(COMPANY_FUND) created: {} VND", INITIAL_FUND);
        } else {
            log.info("   💰 Wallet(COMPANY_FUND) already exists, skipping.");
        }

        // 4c. Wallet(FLOAT_MAIN, ownerId=0) — system-wide control wallet
        //     balance = SUM of all non-FLOAT_MAIN wallets at this point
        if (!walletRepository.existsByOwnerTypeAndOwnerId(WalletOwnerType.FLOAT_MAIN, 0L)) {
            BigDecimal floatBalance = walletRepository.sumAllBalancesExcept(WalletOwnerType.FLOAT_MAIN);
            walletRepository.save(Wallet.builder()
                    .ownerType(WalletOwnerType.FLOAT_MAIN)
                    .ownerId(0L)
                    .balance(floatBalance)
                    .lockedBalance(BigDecimal.ZERO)
                    .build());
            log.info("   🔍 Wallet(FLOAT_MAIN) created: {} VND", floatBalance);
        } else {
            log.info("   🔍 Wallet(FLOAT_MAIN) already exists, skipping.");
        }
    }

    // =========================================================
    // 5. SYSTEM CONFIGS
    // =========================================================
    private void initSystemConfigs() {
        log.info("── [5/5] Seeding SystemConfig ...");

        List<Object[]> configs = List.of(
                // key, value, description
                new Object[]{"PIN_MAX_RETRY",           "5",            "Số lần nhập sai PIN tối đa trước khi bị khóa"},
                new Object[]{"PIN_LOCK_MINUTES",        "30",           "Thời gian khóa PIN (phút) sau khi vượt quá số lần thử"},
                new Object[]{"WITHDRAW_AUTO_APPROVE_LIMIT", "5000000",  "Số tiền rút tối đa tự động duyệt (VND). Vượt ngưỡng sẽ chuyển Pending cho Accountant duyệt"},
                new Object[]{"PAYROLL_ADVANCE_NETTING", "true",         "Tự động trừ nợ tạm ứng khi chi lương (true/false)"},
                new Object[]{"SYSTEM_MAINTENANCE_MODE", "false",        "Chế độ bảo trì hệ thống – chặn toàn bộ giao dịch nếu true"},
                new Object[]{"DEFAULT_CURRENCY",        "VND",          "Đơn vị tiền tệ mặc định của hệ thống"},
                new Object[]{"MAX_ATTACHMENT_SIZE_MB",  "10",           "Dung lượng tối đa mỗi file đính kèm yêu cầu (MB)"},
                new Object[]{"MAX_ATTACHMENT_COUNT",    "5",            "Số file đính kèm tối đa cho một yêu cầu"},
                new Object[]{"JWT_REFRESH_EXPIRY_DAYS", "7",            "Thời hạn Refresh Token (ngày)"},
                new Object[]{"NOTIFICATION_RETAIN_DAYS","90",           "Số ngày lưu giữ thông báo trước khi tự xóa"}
        );

        for (Object[] row : configs) {
            String key   = (String) row[0];
            String value = (String) row[1];
            String desc  = (String) row[2];
            if (!systemConfigRepository.existsById(key)) {
                systemConfigRepository.save(
                        SystemConfig.builder()
                                .key(key)
                                .value(value)
                                .description(desc)
                                .build()
                );
                log.info("   ⚙️  Config [{}] = {}", key, value);
            }
        }
    }

    // =========================================================
    // 6. EXPENSE CATEGORIES (System defaults)
    // =========================================================
    private void initExpenseCategories() {
        log.info("── [6/6] Seeding Expense Categories ...");

        List<Object[]> categories = List.of(
                new Object[]{"Travel & Accommodation", "Công tác phí, di chuyển, khách sạn, vé máy bay"},
                new Object[]{"Equipment & Software", "Mua sắm thiết bị, bản quyền phần mềm, server, license"},
                new Object[]{"Meals & Entertainment", "Ăn uống, tiếp khách, team building, sự kiện"},
                new Object[]{"Outsourcing & Services", "Thuê ngoài, dịch vụ tư vấn, freelancer"}
        );

        for (Object[] row : categories) {
            String name = (String) row[0];
            String desc = (String) row[1];
            if (expenseCategoryRepository.findByName(name).isEmpty()) {
                expenseCategoryRepository.save(ExpenseCategory.builder()
                        .name(name)
                        .description(desc)
                        .isSystemDefault(true)
                        .build());
                log.info("   📂 Category created: {}", name);
            }
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private void createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
        if (roleRepository.findByName(name).isEmpty()) {
            roleRepository.save(Role.builder()
                    .name(name)
                    .description(description)
                    .permissions(permissions)
                    .build());
            log.info("   🔐 Role created: {} ({} permissions)", name, permissions.size());
        } else {
            log.info("   🔐 Role [{}] already exists.", name);
        }
    }

    private void createDeptIfNotExists(String name, String code,
                                       BigDecimal quota, BigDecimal available) {
        if (departmentRepository.findByCode(code).isEmpty()) {
            departmentRepository.save(Department.builder()
                    .name(name)
                    .code(code)
                    .totalProjectQuota(quota)
                    .totalAvailableBalance(available)
                    .build());
            log.info("   🏢 Department created: {} [{}]", name, code);
        } else {
            log.info("   🏢 Department [{}] already exists.", code);
        }
    }

    /**
     * Creates a User + UserProfile + Wallet in one shot.
     * Returns the persisted User (or null if already exists).
     */
    private User createUserIfNotExists(
            String email, String employeeCode, String fullName,
            Role role, Department department,
            String jobTitle, String phone, String address,
            String bankName, String bankAccount, String bankOwner) {

        if (userRepository.findByEmail(email).isPresent()) {
            log.info("   👤 User [{}] already exists.", email);
            return userRepository.findByEmail(email).orElseThrow();
        }

        User user = User.builder()
                .email(email)
                .fullName(fullName)
                .department(department)
                .password(passwordEncoder.encode(DEFAULT_PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .build();

        user = userRepository.save(user);

        // UserProfile
        UserProfile profile = UserProfile.builder()
                .user(user)
                .employeeCode(employeeCode)
                .jobTitle(jobTitle)
                .phoneNumber(phone)
                .address(address)
                .bankName(bankName)
                .bankAccountNum(bankAccount)
                .bankAccountOwner(bankOwner)
                .build();
        user.setProfile(profile);
        userRepository.save(user);

        // Wallet
        createWalletIfNotExists(user);

        log.info("   👤 User created: {} | {} | {} | dept={}", employeeCode, fullName, email, department.getCode());
        return user;
    }

    private void createWalletIfNotExists(User user) {
        if (!walletRepository.existsByOwnerTypeAndOwnerId(WalletOwnerType.USER, user.getId())) {
            walletRepository.save(Wallet.builder()
                    .ownerType(WalletOwnerType.USER)
                    .ownerId(user.getId())
                    .balance(BigDecimal.ZERO)
                    .lockedBalance(BigDecimal.ZERO)
                    .build());
        }
    }

    private void assignManagerToDept(Department dept, User manager) {
        if (dept.getManager() == null) {
            dept.setManager(manager);
            departmentRepository.save(dept);
            log.info("   🏢 Dept [{}] → manager set: {}", dept.getCode(), manager.getEmail());
        }
    }
}
