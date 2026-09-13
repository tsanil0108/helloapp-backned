package com.packersmovers.marketplace.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterProviderRequest {

    @NotBlank
    private String companyName;

    @NotBlank
    private String ownerName;

    @NotBlank @Email
    private String email;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String mobile;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    private String gstNumber;
    private String panNumber;

    @NotEmpty(message = "Select at least one service area")
    private List<Long> serviceAreaIds;

    @NotEmpty(message = "Select at least one service category")
    private List<Long> serviceCategoryIds;
}
