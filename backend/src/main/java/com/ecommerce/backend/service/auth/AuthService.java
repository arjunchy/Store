package com.ecommerce.backend.service.auth;

import com.ecommerce.backend.dto.request.AuthRequest;
import com.ecommerce.backend.dto.response.AuthResponse;
import com.ecommerce.backend.dto.request.RefreshTokenRequest;

public interface AuthService {

    AuthResponse login(AuthRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String refreshToken);
}