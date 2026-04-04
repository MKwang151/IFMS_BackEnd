package com.mkwang.backend.modules.user.service;

import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeGenerator;
import com.mkwang.backend.common.utils.businesscodegenerator.BusinessCodeType;
import com.mkwang.backend.modules.mail.publisher.MailPublisher;
import com.mkwang.backend.modules.mail.publisher.MailType;
import com.mkwang.backend.modules.organization.entity.Department;
import com.mkwang.backend.modules.organization.repository.DepartmentRepository;
import com.mkwang.backend.modules.profile.service.ProfileService;
import com.mkwang.backend.modules.user.dto.request.OnboardUserRequest;
import com.mkwang.backend.modules.user.dto.response.OnboardUserResponse;
import com.mkwang.backend.modules.user.entity.*;
import com.mkwang.backend.modules.user.repository.RoleRepository;
import com.mkwang.backend.modules.user.repository.UserRepository;
import com.mkwang.backend.modules.wallet.entity.WalletOwnerType;
import com.mkwang.backend.modules.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository           userRepository;
    private final RoleRepository           roleRepository;
    private final DepartmentRepository     departmentRepository;
    private final ProfileService           profileService;
    private final WalletService            walletService;
    private final PasswordEncoder          passwordEncoder;
    private final BusinessCodeGenerator    codeGenerator;
    private final SpringTemplateEngine     templateEngine;
    private final MailPublisher            mailPublisher;
    private final SecureRandom             secureRandom;

    // ── POST /users/onboard ──────────────────────────────────────

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public OnboardUserResponse onboardUser(OnboardUserRequest request) {
        // 1. Kiểm tra email trùng
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists: " + request.getEmail());
        }

        // 2. Tìm role
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.getRoleName()));

        // 3. Tìm department
        Department department = departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found: " + request.getDepartmentId()));

        // 4. Sinh mã nhân viên và mật khẩu tạm
        String employeeCode    = codeGenerator.generate(BusinessCodeType.EMPLOYEE);
        String tempPassword    = generateTemporaryPassword();

        // 5. Tạo User
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFullName())
                .password(passwordEncoder.encode(tempPassword))
                .role(role)
                .department(department)
                .status(UserStatus.ACTIVE)
                .isFirstLogin(true)
                .build();
        user = userRepository.save(user);

        // 6. Tạo UserProfile
        profileService.createProfile(user, employeeCode, request.getJobTitle(), request.getPhoneNumber());

        // 7. Tạo Wallet
        walletService.createWallet(WalletOwnerType.USER, user.getId());

        // 8. Gửi onboard email
        sendOnboardEmail(user.getEmail(), user.getFullName(), employeeCode,
                tempPassword, role.getName(), department.getName());

        log.info("User onboarded: {} | {} | dept={}", employeeCode, request.getEmail(), department.getCode());

        return OnboardUserResponse.builder()
                .id(user.getId())
                .employeeCode(employeeCode)
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(role.getName())
                .departmentName(department.getName())
                .temporaryPassword(tempPassword)
                .build();
    }

    // ── Private helpers ──────────────────────────────────────────

    private void sendOnboardEmail(String email, String fullName, String employeeCode,
                                   String temporaryPassword, String role, String departmentName) {
        Context ctx = new Context();
        ctx.setVariable("fullName",          fullName);
        ctx.setVariable("email",             email);
        ctx.setVariable("employeeCode",      employeeCode);
        ctx.setVariable("temporaryPassword", temporaryPassword);
        ctx.setVariable("role",              role);
        ctx.setVariable("departmentName",    departmentName);

        String html = templateEngine.process("email/onboard-email", ctx);
        mailPublisher.publish(MailType.ONBOARD, email, "Chào mừng bạn đến với IFMS — Thông tin tài khoản", html);
    }

    /**
     * Sinh mật khẩu tạm: Ifms@ + 4 chữ số ngẫu nhiên (VD: Ifms@7832).
     * Đủ điều kiện uppercase, special char, digit — pass hầu hết password policy.
     */
    private String generateTemporaryPassword() {
        int digits = 1000 + secureRandom.nextInt(9000); // 1000–9999
        return "Ifms@" + digits;
    }
}
