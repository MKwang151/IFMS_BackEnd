package com.mkwang.backend.modules.auth.service;

import com.mkwang.backend.modules.auth.dto.request.ChangePasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.ForgotPasswordRequest;
import com.mkwang.backend.modules.auth.dto.request.LoginRequest;
import com.mkwang.backend.modules.auth.dto.request.ResetPasswordRequest;
import com.mkwang.backend.modules.auth.dto.response.AuthenticationResponse;
import com.mkwang.backend.modules.auth.dto.response.UserInfoResponse;

public interface AuthService {

    AuthenticationResponse login(LoginRequest request);

    AuthenticationResponse refreshToken(String refreshToken);

    void logout(String accessToken);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    void changePassword(ChangePasswordRequest request, String username);

    UserInfoResponse getCurrentUser(String username);
}
