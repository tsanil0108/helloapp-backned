package com.packersmovers.marketplace.dto.coupon;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(
            max = 60,
            message = "Coupon code cannot exceed 60 characters"
    )
    private String code;


    @DecimalMin(
            value = "0.01",
            inclusive = true,
            message = "Coupon amount must be greater than 0"
    )
    private BigDecimal amount;


    /**
     * true  = Welcome / registration coupon
     * false = Normal promotional coupon
     */
    private boolean welcomeCoupon;


    private Instant expiresAt;


    private Integer maxUses;


    private Boolean active;
}