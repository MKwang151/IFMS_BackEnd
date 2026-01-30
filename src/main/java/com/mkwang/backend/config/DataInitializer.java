package com.mkwang.backend.config;

import com.mkwang.backend.modules.user.entity.Permission;
import com.mkwang.backend.modules.user.entity.Role;
import com.mkwang.backend.modules.user.entity.User;
import com.mkwang.backend.modules.user.repository.RoleRepository;
import com.mkwang.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Initialize default roles and users on application startup
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

  private final RoleRepository roleRepository;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(String... args) {
    initializeRoles();
    initializeUsers();
  }

  private void initializeRoles() {
    // Check if roles already exist
    if (roleRepository.count() > 0) {
      log.info("Roles already initialized, skipping...");
      return;
    }

    // Create ADMIN role with all permissions
    Role adminRole = Role.builder()
        .name("ADMIN")
        .description("Administrator with full access")
        .permissions(Set.of(
            Permission.ADMIN_READ,
            Permission.ADMIN_WRITE,
            Permission.ADMIN_DELETE,
            Permission.USER_READ,
            Permission.USER_WRITE))
        .build();

    // Create USER role with basic permissions
    Role userRole = Role.builder()
        .name("USER")
        .description("Default user role with basic permissions")
        .permissions(Set.of(
            Permission.USER_READ,
            Permission.USER_WRITE))
        .build();

    roleRepository.save(adminRole);
    roleRepository.save(userRole);

    log.info("✅ Initialized default roles: ADMIN, USER");
  }

  private void initializeUsers() {
    // Check if users already exist
    if (userRepository.count() > 0) {
      log.info("Users already initialized, skipping...");
      return;
    }

    Role adminRole = roleRepository.findByName("ADMIN")
        .orElseThrow(() -> new RuntimeException("ADMIN role not found"));
    Role userRole = roleRepository.findByName("USER")
        .orElseThrow(() -> new RuntimeException("USER role not found"));

    // Create admin user
    User admin = User.builder()
        .email("admin@example.com")
        .password(passwordEncoder.encode("admin123"))
        .firstName("Admin")
        .lastName("User")
        .role(adminRole)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();

    // Create regular user
    User user = User.builder()
        .email("user@example.com")
        .password(passwordEncoder.encode("user123"))
        .firstName("Regular")
        .lastName("User")
        .role(userRole)
        .enabled(true)
        .accountNonExpired(true)
        .accountNonLocked(true)
        .credentialsNonExpired(true)
        .build();

    userRepository.save(admin);
    userRepository.save(user);

    log.info("✅ Initialized default users:");
    log.info("   - admin@example.com / admin123 (ADMIN role)");
    log.info("   - user@example.com / user123 (USER role)");
  }
}
