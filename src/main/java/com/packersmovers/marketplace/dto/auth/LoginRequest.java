package com.packersmovers.marketplace.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Email or mobile is required")
    private String identifier; // email or mobile

    @NotBlank(message = "Password is required")
    private String password;
}
