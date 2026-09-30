package com.packersmovers.marketplace.dto.coupon;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(
            max = 60,
            message = "Coupon code must not exceed 60 characters"
    )
    private String code;
}