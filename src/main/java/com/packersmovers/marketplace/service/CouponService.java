package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.coupon.CouponResponse;
import com.packersmovers.marketplace.dto.coupon.CouponValidationResponse;
import com.packersmovers.marketplace.dto.coupon.CreateCouponRequest;
import com.packersmovers.marketplace.dto.coupon.UpdateCouponRequest;
import com.packersmovers.marketplace.entity.Coupon;

import java.util.List;

public interface CouponService {

    CouponResponse createCoupon(
            CreateCouponRequest request,
            Long adminUserId
    );

    CouponResponse updateCoupon(
            Long couponId,
            UpdateCouponRequest request
    );

    List<CouponResponse> listCoupons();

    CouponResponse getCoupon(Long couponId);

    CouponResponse activateCoupon(Long couponId);

    CouponResponse deactivateCoupon(Long couponId);

    CouponValidationResponse validateCoupon(String code);

    Coupon redeemCoupon(
            String code,
            Long providerId
    );
}