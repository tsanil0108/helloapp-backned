package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.coupon.CouponResponse;
import com.packersmovers.marketplace.dto.coupon.CreateCouponRequest;
import com.packersmovers.marketplace.dto.coupon.UpdateCouponRequest;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class CouponController {

    private final CouponService couponService;


    // ============================================================
    // LIST COUPONS
    // ============================================================

    @GetMapping
    public ApiResponse<List<CouponResponse>> listCoupons() {

        return ApiResponse.success(
                couponService.listCoupons()
        );
    }


    // ============================================================
    // GET COUPON
    // ============================================================

    @GetMapping("/{couponId}")
    public ApiResponse<CouponResponse> getCoupon(
            @PathVariable Long couponId
    ) {

        return ApiResponse.success(
                couponService.getCoupon(couponId)
        );
    }


    // ============================================================
    // CREATE COUPON
    // ============================================================

    @PostMapping
    public ApiResponse<CouponResponse> createCoupon(
            @Valid @RequestBody CreateCouponRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        if (principal == null
                || principal.getUserId() == null) {

            throw new BadRequestException(
                    "Authenticated admin is required"
            );
        }

        return ApiResponse.success(
                couponService.createCoupon(
                        request,
                        principal.getUserId()
                )
        );
    }


    // ============================================================
    // UPDATE COUPON
    // ============================================================

    @PutMapping("/{couponId}")
    public ApiResponse<CouponResponse> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody UpdateCouponRequest request
    ) {

        return ApiResponse.success(
                couponService.updateCoupon(
                        couponId,
                        request
                )
        );
    }


    // ============================================================
    // ACTIVATE
    // ============================================================

    @PostMapping("/{couponId}/activate")
    public ApiResponse<CouponResponse> activateCoupon(
            @PathVariable Long couponId
    ) {

        return ApiResponse.success(
                couponService.activateCoupon(
                        couponId
                )
        );
    }


    // ============================================================
    // DEACTIVATE
    // ============================================================

    @PostMapping("/{couponId}/deactivate")
    public ApiResponse<CouponResponse> deactivateCoupon(
            @PathVariable Long couponId
    ) {

        return ApiResponse.success(
                couponService.deactivateCoupon(
                        couponId
                )
        );
    }
}