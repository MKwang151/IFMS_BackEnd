package com.mkwang.backend.modules.profile.service;

import com.mkwang.backend.common.exception.BadRequestException;
import com.mkwang.backend.common.exception.ResourceNotFoundException;
import com.mkwang.backend.modules.profile.dto.request.UpdateMyProfileRequest;
import com.mkwang.backend.modules.profile.dto.response.MyProfileResponse;
import com.mkwang.backend.modules.profile.entity.UserProfile;
import com.mkwang.backend.modules.profile.entity.UserSecuritySettings;
import com.mkwang.backend.modules.profile.mapper.ProfileMapper;
import com.mkwang.backend.modules.profile.repository.UserProfileRepository;
import com.mkwang.backend.modules.profile.repository.UserSecuritySettingsRepository;
import com.mkwang.backend.modules.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository userProfileRepository;
    private final UserSecuritySettingsRepository userSecuritySettingsRepository;
    private final ProfileMapper profileMapper;

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

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(Long userId) {
        UserProfile profile = getProfileByUserId(userId);
        return profileMapper.toMyProfileResponse(profile);
    }

    @Override
    @Transactional
    public MyProfileResponse updateMyProfile(Long userId, UpdateMyProfileRequest request) {
        UserProfile profile = getProfileByUserId(userId);
        String normalizedPhoneNumber = request.getPhoneNumber().trim();

        if (userProfileRepository.existsByPhoneNumberAndUserIdNot(normalizedPhoneNumber, userId)) {
            throw new BadRequestException("Phone number already exists: " + normalizedPhoneNumber);
        }

        profile.getUser().setFullName(request.getFullName().trim());
        profile.setPhoneNumber(normalizedPhoneNumber);
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setCitizenId(normalizeOptionalField(request.getCitizenId()));
        profile.setAddress(normalizeOptionalField(request.getAddress()));

        UserProfile savedProfile = userProfileRepository.save(profile);
        return profileMapper.toMyProfileResponse(savedProfile);
    }

    private String normalizeOptionalField(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
