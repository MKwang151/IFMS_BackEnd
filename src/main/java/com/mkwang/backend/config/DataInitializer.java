//package com.mkwang.backend.config;
//
//import com.mkwang.backend.modules.accounting.entity.SystemFund;
//import com.mkwang.backend.modules.accounting.repository.SystemFundRepository;
//import com.mkwang.backend.modules.user.entity.Permission;
//import com.mkwang.backend.modules.user.entity.Role;
//import com.mkwang.backend.modules.user.entity.User;
//import com.mkwang.backend.modules.user.entity.UserStatus;
//import com.mkwang.backend.modules.user.repository.RoleRepository;
//import com.mkwang.backend.modules.user.repository.UserRepository;
//import com.mkwang.backend.modules.wallet.entity.Wallet;
//import com.mkwang.backend.modules.wallet.repository.WalletRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.math.BigDecimal;
//import java.util.Set;
//
///**
// * Initialize default roles, users, wallets and system fund on application
// * startup.
// * Only creates data if it doesn't already exist (idempotent).
// */
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class DataInitializer implements CommandLineRunner {
//
//  private final RoleRepository roleRepository;
//  private final UserRepository userRepository;
//  private final WalletRepository walletRepository;
//  private final SystemFundRepository systemFundRepository;
//  private final PasswordEncoder passwordEncoder;
//
//  private static final String DEFAULT_PASSWORD = "123456";
//  private static final BigDecimal INITIAL_SYSTEM_FUND = new BigDecimal("10000000000"); // 10 tỷ VND
//
//  @Override
//  @Transactional
//  public void run(String... args) {
//    log.info("🚀 Starting DataInitializer...");
//
//    initRoles();
//    initUsers();
//    initSystemFund();
//
//    log.info("✅ DataInitializer completed successfully!");
//  }
//
//  /**
//   * Initialize 4 default roles with permissions
//   */
//  private void initRoles() {
//    // 1. EMPLOYEE Role
//    createRoleIfNotExists("EMPLOYEE", "Nhân viên - Tạo yêu cầu, xem ví cá nhân", Set.of(
//        Permission.USER_PROFILE_VIEW,
//        Permission.USER_PROFILE_UPDATE,
//        Permission.USER_PIN_UPDATE,
//        Permission.NOTIFICATION_VIEW,
//        Permission.WALLET_VIEW_SELF,
//        Permission.WALLET_DEPOSIT,
//        Permission.WALLET_WITHDRAW,
//        Permission.WALLET_TRANSACTION_VIEW,
//        Permission.PROJECT_VIEW_ACTIVE,
//        Permission.REQUEST_CREATE,
//        Permission.REQUEST_VIEW_SELF,
//        Permission.PAYROLL_VIEW_SELF,
//        Permission.PAYROLL_DOWNLOAD));
//
//    // 2. MANAGER Role (Inherits Employee + Manager permissions)
//    createRoleIfNotExists("MANAGER", "Trưởng phòng - Quản lý dự án, duyệt yêu cầu cấp 1", Set.of(
//        // Employee permissions
//        Permission.USER_PROFILE_VIEW,
//        Permission.USER_PROFILE_UPDATE,
//        Permission.USER_PIN_UPDATE,
//        Permission.NOTIFICATION_VIEW,
//        Permission.WALLET_VIEW_SELF,
//        Permission.WALLET_DEPOSIT,
//        Permission.WALLET_WITHDRAW,
//        Permission.WALLET_TRANSACTION_VIEW,
//        Permission.PROJECT_VIEW_ACTIVE,
//        Permission.REQUEST_CREATE,
//        Permission.REQUEST_VIEW_SELF,
//        Permission.PAYROLL_VIEW_SELF,
//        Permission.PAYROLL_DOWNLOAD,
//        // Manager-specific permissions
//        Permission.PROJECT_CREATE,
//        Permission.PROJECT_UPDATE,
//        Permission.PROJECT_PHASE_MANAGE,
//        Permission.PROJECT_MEMBER_MANAGE,
//        Permission.PROJECT_STATUS_MANAGE,
//        Permission.REQUEST_VIEW_DEPT,
//        Permission.REQUEST_APPROVE_TIER1,
//        Permission.REQUEST_REJECT,
//        Permission.DEPT_VIEW_DASHBOARD));
//
//    // 3. ACCOUNTANT Role (Inherits Employee + Accountant permissions)
//    createRoleIfNotExists("ACCOUNTANT", "Kế toán - Quản lý quỹ, chi lương, giải ngân", Set.of(
//        // Employee permissions
//        Permission.USER_PROFILE_VIEW,
//        Permission.USER_PROFILE_UPDATE,
//        Permission.USER_PIN_UPDATE,
//        Permission.NOTIFICATION_VIEW,
//        Permission.WALLET_VIEW_SELF,
//        Permission.WALLET_DEPOSIT,
//        Permission.WALLET_WITHDRAW,
//        Permission.WALLET_TRANSACTION_VIEW,
//        Permission.PROJECT_VIEW_ACTIVE,
//        Permission.REQUEST_CREATE,
//        Permission.REQUEST_VIEW_SELF,
//        Permission.PAYROLL_VIEW_SELF,
//        Permission.PAYROLL_DOWNLOAD,
//        // Accountant-specific permissions
//        Permission.PROJECT_VIEW_ALL,
//        Permission.REQUEST_VIEW_APPROVED,
//        Permission.REQUEST_PAYOUT,
//        Permission.TRANSACTION_APPROVE_WITHDRAW,
//        Permission.PAYROLL_MANAGE,
//        Permission.PAYROLL_EXECUTE,
//        Permission.SYSTEM_FUND_VIEW,
//        Permission.SYSTEM_FUND_TOPUP));
//
//    // 4. ADMIN Role (Full permissions)
//    createRoleIfNotExists("ADMIN", "Quản trị viên - Toàn quyền hệ thống", Set.of(Permission.values()));
//  }
//
//  /**
//   * Initialize 4 default users (one for each role)
//   */
//  private void initUsers() {
//    Role employeeRole = roleRepository.findByName("EMPLOYEE").orElseThrow();
//    Role managerRole = roleRepository.findByName("MANAGER").orElseThrow();
//    Role accountantRole = roleRepository.findByName("ACCOUNTANT").orElseThrow();
//    Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();
//
//    // Create users
//    User employee = createUserIfNotExists("employee@ifms.com", "Nguyễn Văn A", employeeRole);
//    User manager = createUserIfNotExists("manager@ifms.com", "Trần Thị B", managerRole);
//    User accountant = createUserIfNotExists("accountant@ifms.com", "Lê Văn C", accountantRole);
//    User admin = createUserIfNotExists("admin@ifms.com", "Phạm Thị D", adminRole);
//
//    // Create wallets for each user
//    if (employee != null)
//      createWalletIfNotExists(employee);
//    if (manager != null)
//      createWalletIfNotExists(manager);
//    if (accountant != null)
//      createWalletIfNotExists(accountant);
//    if (admin != null)
//      createWalletIfNotExists(admin);
//  }
//
//  /**
//   * Initialize System Fund with 10 billion VND
//   */
//  private void initSystemFund() {
//    if (systemFundRepository.count() == 0) {
//      SystemFund fund = SystemFund.builder()
//          .totalBalance(INITIAL_SYSTEM_FUND)
//          .bankName("Vietcombank")
//          .bankAccount("0011004567890")
//          .build();
//      systemFundRepository.save(fund);
//      log.info("💰 Created SystemFund with initial balance: {} VND", INITIAL_SYSTEM_FUND);
//    } else {
//      log.info("💰 SystemFund already exists, skipping...");
//    }
//  }
//
//  // ============ HELPER METHODS ============
//
//  private void createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
//    if (roleRepository.findByName(name).isEmpty()) {
//      Role role = Role.builder()
//          .name(name)
//          .description(description)
//          .permissions(permissions)
//          .build();
//      roleRepository.save(role);
//      log.info("🔐 Created role: {} with {} permissions", name, permissions.size());
//    } else {
//      log.info("🔐 Role {} already exists, skipping...", name);
//    }
//  }
//
//  private User createUserIfNotExists(String email, String fullName, Role role) {
//    if (userRepository.findByEmail(email).isEmpty()) {
//      User user = User.builder()
//          .email(email)
//          .password(passwordEncoder.encode(DEFAULT_PASSWORD))
//          .fullName(fullName)
//          .role(role)
//          .status(UserStatus.ACTIVE)
//          .isFirstLogin(false) // Set false for demo accounts
//          .enabled(true)
//          .build();
//      userRepository.save(user);
//      log.info("👤 Created user: {} ({})", fullName, email);
//      return user;
//    } else {
//      log.info("👤 User {} already exists, skipping...", email);
//      return null;
//    }
//  }
//
//  private void createWalletIfNotExists(User user) {
//    if (!walletRepository.existsByUserId(user.getId())) {
//      Wallet wallet = Wallet.builder()
//          .user(user)
//          .balance(BigDecimal.ZERO)
//          .pendingBalance(BigDecimal.ZERO)
//          .debtBalance(BigDecimal.ZERO)
//          .build();
//      walletRepository.save(wallet);
//      log.info("💳 Created wallet for user: {}", user.getEmail());
//    }
//  }
//}