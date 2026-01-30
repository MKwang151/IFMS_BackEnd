package com.mkwang.backend.modules.auth.service;

import com.mkwang.backend.modules.auth.dto.response.AuthenticationResponse;
import com.mkwang.backend.modules.auth.dto.request.LoginRequest;
import com.mkwang.backend.modules.auth.dto.request.RegisterRequest;

public interface AuthService {

    AuthenticationResponse register(RegisterRequest request);

    AuthenticationResponse login(LoginRequest request);

    AuthenticationResponse refreshToken(String refreshToken);
}
