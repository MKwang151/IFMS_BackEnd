package com.mkwang.backend.modules.user.entity;

import com.mkwang.backend.modules.file.entity.FileStorage;
import com.mkwang.backend.modules.user.entity.User; // Import User
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * UserProfile entity - Contains personal, HR info and bank information.
 * One-to-One relationship with User (Shares primary key).
 */
@Entity
@Table(name = "user_profiles", indexes = {
    @Index(name = "idx_user_profiles_employee_code", columnList = "employee_code"),
    @Index(name = "idx_user_profiles_phone_number", columnList = "phone_number")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

  @Id
  @Column(name = "user_id")
  private Long userId;

  @OneToOne(fetch = FetchType.LAZY)
  @MapsId
  @JoinColumn(name = "user_id")
  private User user;

  // =================================================================
  // 1. THÔNG TIN ĐỊNH DANH (HR Identity) - THÊM MỚI
  // =================================================================

  // Mã nhân viên (VD: MK001, MK002). Cực kỳ quan trọng để quản lý nội bộ.
  @Column(name = "employee_code", length = 20, unique = true)
  private String employeeCode;

  // Chức danh công việc thực tế (VD: Senior Backend Developer, Marketing Lead)
  @Column(name = "job_title", length = 100)
  private String jobTitle;

  // =================================================================
  // 2. THÔNG TIN CÁ NHÂN (Personal Info)
  // =================================================================

  @Column(name = "phone_number", length = 15, unique = true) // Sửa length lên 15 đề phòng mã vùng
  private String phoneNumber;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "citizen_id", length = 20)
  private String citizenId; // Căn cước công dân

  @Column(name = "address", length = 255) // Nên thêm địa chỉ
  private String address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "avatar_file_id")
  private FileStorage avatarFile;

  // =================================================================
  // 3. THÔNG TIN TÀI CHÍNH (Bank Info for Salary)
  // =================================================================

  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Column(name = "bank_account_num", length = 30)
  private String bankAccountNum;

  @Column(name = "bank_account_owner", length = 100)
  private String bankAccountOwner;
}