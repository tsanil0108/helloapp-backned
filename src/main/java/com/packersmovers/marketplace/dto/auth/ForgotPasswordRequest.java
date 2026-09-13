package com.packersmovers.marketplace.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank(message = "Email or mobile is required") String identifier) {}
