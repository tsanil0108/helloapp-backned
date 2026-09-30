package com.packersmovers.marketplace.dto.coupon;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
public class CouponResponse {

    private Long id;

    private String code;

    private BigDecimal amount;

    private boolean welcomeCoupon;

    private boolean active;

    private Instant expiresAt;

    private Integer maxUses;

    private int usedCount;

    private boolean expired;

    private boolean usageLimitReached;

    private Instant createdAt;

    private Instant updatedAt;
}