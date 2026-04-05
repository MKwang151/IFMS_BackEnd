package com.mkwang.backend.modules.profile.service;

import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.modules.profile.entity.UserProfile;
import com.mkwang.backend.modules.profile.entity.UserSecuritySettings;
import com.mkwang.backend.modules.profile.repository.UserProfileRepository;
import com.mkwang.backend.modules.profile.repository.UserSecuritySettingsRepository;
import com.mkwang.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserSecuritySettingsRepository userSecuritySettingsRepository;

    @Override
    @Transactional
    public UserProfile createProfile(User user, String employeeCode, String jobTitle, String phoneNumber) {
        UserProfile profile = UserProfile.builder()
                .user(user)
                .employeeCode(employeeCode)
                .jobTitle(jobTitle)
                .phoneNumber(phoneNumber)
                .build();
        return userProfileRepository.save(profile);
    }

    @Override
    @Transactional
    public UserSecuritySettings createSecuritySettings(User user, String encodedPin) {
        UserSecuritySettings settings = userSecuritySettingsRepository
                .findById(user.getId())
                .orElse(UserSecuritySettings.builder().user(user).build());
        settings.setTransactionPin(encodedPin);
        return userSecuritySettingsRepository.save(settings);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile getProfileByUserId(Long userId) {
        return userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserProfile", "userId", userId));
    }
}
