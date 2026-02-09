package com.mkwang.backend.modules.user.entity;

import com.mkwang.backend.modules.file.entity.FileStorage;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.NumberFormat;

import java.time.LocalDate;

/**
 * UserProfile entity - Contains personal and bank information.
 * One-to-One relationship with User (Shares primary key).
 */
@Entity
@Table(name = "user_profiles")
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

  @Column(name = "phone_number", length = 10, unique = true)
  private String phoneNumber;

  @Column(name = "date_of_birth")
  private LocalDate dateOfBirth;

  @Column(name = "citizen_id", length = 20)
  private String citizenId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "avatar_file_id")
  private FileStorage avatarFile;

  // Bank information
  @Column(name = "bank_name", length = 100)
  private String bankName;

  @Column(name = "bank_account_num", length = 30)
  private String bankAccountNum;

  @Column(name = "bank_account_owner", length = 100)
  private String bankAccountOwner;
}
