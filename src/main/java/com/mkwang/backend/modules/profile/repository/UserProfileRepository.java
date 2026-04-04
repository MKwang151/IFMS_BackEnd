package com.mkwang.backend.modules.profile.repository;

import com.mkwang.backend.modules.profile.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByPhoneNumber(String phoneNumber);
}
