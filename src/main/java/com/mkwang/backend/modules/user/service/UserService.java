package com.mkwang.backend.modules.user.service;

import com.mkwang.backend.modules.user.dto.request.OnboardUserRequest;
import com.mkwang.backend.modules.user.dto.response.OnboardUserResponse;
import com.mkwang.backend.modules.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    OnboardUserResponse onboardUser(OnboardUserRequest request);

    User getUserById(Long userId);

    Page<User> getDepartmentMembers(Long departmentId, Long excludedUserId, String search, Pageable pageable);

    User getDepartmentUserById(Long departmentId, Long userId);

    List<User> getActiveTeamLeadersByDepartmentId(Long departmentId);
}
