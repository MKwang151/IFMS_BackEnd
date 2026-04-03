package com.mkwang.backend.modules.user.service;

import com.mkwang.backend.modules.user.dto.request.OnboardUserRequest;
import com.mkwang.backend.modules.user.dto.response.OnboardUserResponse;

public interface UserService {

    OnboardUserResponse onboardUser(OnboardUserRequest request);
}
