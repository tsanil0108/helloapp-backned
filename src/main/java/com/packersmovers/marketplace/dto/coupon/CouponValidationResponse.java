package com.packersmovers.marketplace.dto.coupon;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CouponValidationResponse {

    private boolean valid;

    private String code;

    private BigDecimal amount;

    private boolean welcomeCoupon;

    private String message;
}