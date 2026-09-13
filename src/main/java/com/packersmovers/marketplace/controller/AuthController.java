package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.auth.JwtAuthResponse;
import com.packersmovers.marketplace.dto.auth.LoginRequest;
import com.packersmovers.marketplace.dto.auth.RefreshTokenRequest;
import com.packersmovers.marketplace.dto.auth.RegisterProviderRequest;
import com.packersmovers.marketplace.dto.auth.ForgotPasswordRequest;
import com.packersmovers.marketplace.dto.auth.ResetPasswordRequest;
import com.packersmovers.marketplace.dto.auth.ChangePasswordRequest;
import com.packersmovers.marketplace.service.AuthService;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Login-capable identities only (Provider / Admin / Super Admin).
 * Customers never authenticate here - see PublicLeadController for the frictionless, no-OTP lead form.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Login, provider self-registration, token refresh")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register-provider")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JwtAuthResponse> registerProvider(@Valid @RequestBody RegisterProviderRequest request) {
        return ApiResponse.success("Registration received. Your account is under Super Admin review.",
                authService.registerProvider(request));
    }

    @PostMapping("/login")
    public ApiResponse<JwtAuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<JwtAuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(authService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ApiResponse.success("Logged out successfully", null);
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(org.springframework.security.core.Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        authService.logoutAll(principal.getUserId());
        return ApiResponse.success("All sessions revoked successfully", null);
    }
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ApiResponse.success("If an account exists, password reset instructions have been sent.", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.success("Password reset successfully. Please sign in again.", null);
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
                                             org.springframework.security.core.Authentication authentication) {
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        authService.changePassword(principal.getUserId(), request);
        return ApiResponse.success("Password changed successfully. Please sign in again on other devices.", null);
    }

}
