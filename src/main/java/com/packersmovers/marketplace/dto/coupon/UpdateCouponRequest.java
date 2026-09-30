package com.packersmovers.marketplace.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class UpdateCouponRequest {

    @Size(
            max = 60,
            message = "Coupon code must not exceed 60 characters"
    )
    private String code;


    @DecimalMin(
            value = "0.01",
            inclusive = true,
            message = "Coupon amount must be greater than zero"
    )
    private BigDecimal amount;


    private Boolean welcomeCoupon;


    private Boolean active;


    private Instant expiresAt;


    private Integer maxUses;
}