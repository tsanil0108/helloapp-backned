package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.auth.CreateAdminRequest;
import com.packersmovers.marketplace.dto.auth.ForgotPasswordRequest;
import com.packersmovers.marketplace.dto.auth.ResetPasswordRequest;
import com.packersmovers.marketplace.dto.auth.ChangePasswordRequest;
import com.packersmovers.marketplace.dto.auth.JwtAuthResponse;
import com.packersmovers.marketplace.dto.auth.LoginRequest;
import com.packersmovers.marketplace.dto.auth.RegisterProviderRequest;

public interface AuthService {
    JwtAuthResponse login(LoginRequest request);
    JwtAuthResponse refresh(String refreshToken);
    JwtAuthResponse registerProvider(RegisterProviderRequest request);
    void createAdmin(CreateAdminRequest request);
    void logout(String refreshToken);
    void logoutAll(Long userId);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void changePassword(Long userId, ChangePasswordRequest request);
}
