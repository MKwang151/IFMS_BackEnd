package com.mkwang.backend.modules.user.repository;

import com.mkwang.backend.modules.user.entity.UserSecuritySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSecuritySettingsRepository extends JpaRepository<UserSecuritySettings, Long> {
}
