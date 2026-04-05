package com.mkwang.backend.modules.profile.service;

import com.mkwang.backend.modules.profile.entity.UserProfile;
import com.mkwang.backend.modules.profile.entity.UserSecuritySettings;
import com.mkwang.backend.modules.user.entity.User;

public interface ProfileService {

    /**
     * Tạo UserProfile cho user mới trong quá trình onboarding.
     *
     * @param user           user đã được persisted
     * @param employeeCode   mã nhân viên (VD: MK011)
     * @param jobTitle       chức danh
     * @param phoneNumber    số điện thoại
     * @return UserProfile đã được lưu
     */
    UserProfile createProfile(User user, String employeeCode, String jobTitle, String phoneNumber);

    /**
     * Tạo hoặc cập nhật UserSecuritySettings với transaction PIN đã hash.
     * Được gọi khi user hoàn tất first-login setup.
     *
     * @param user       user cần thiết lập PIN
     * @param encodedPin PIN đã được hash (PasswordEncoder.encode)
     * @return UserSecuritySettings đã được lưu
     */
    UserSecuritySettings createSecuritySettings(User user, String encodedPin);

    /**
     * Lấy UserProfile theo userId.
     * Dùng để đọc thông tin ngân hàng (bankAccountNum, bankAccountOwner, bankName)
     * khi user tạo WithdrawRequest.
     *
     * @param userId ID of the user
     * @return UserProfile
     * @throws com.mkwang.backend.common.exception.ResourceNotFoundException nếu không tồn tại
     */
    UserProfile getProfileByUserId(Long userId);
}
