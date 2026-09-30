package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.coupon.CouponValidationResponse;
import com.packersmovers.marketplace.dto.coupon.ValidateCouponRequest;
import com.packersmovers.marketplace.service.CouponService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/coupon")
@RequiredArgsConstructor
@Tag(
        name = "Coupon",
        description = "Public coupon validation for provider registration"
)
public class AuthCouponController {

    private final CouponService couponService;


    // ============================================================
    // VALIDATE COUPON
    // ============================================================

    @PostMapping("/validate")
    public ApiResponse<CouponValidationResponse> validateCoupon(
            @Valid @RequestBody ValidateCouponRequest request
    ) {

        return ApiResponse.success(
                couponService.validateCoupon(
                        request.getCode()
                )
        );
    }
}