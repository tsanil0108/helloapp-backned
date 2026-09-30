package com.packersmovers.marketplace.repository;

import com.packersmovers.marketplace.entity.CouponRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRedemptionRepository
        extends JpaRepository<CouponRedemption, Long> {

    // ============================================================
    // SAME COUPON + SAME PROVIDER
    // ============================================================
    //
    // Prevents the same provider from redeeming
    // the exact same coupon more than once.
    //
    boolean existsByCouponIdAndProviderId(
            Long couponId,
            Long providerId
    );


    // ============================================================
    // WELCOME COUPON CHECK
    // ============================================================
    //
    // Checks whether this provider has already redeemed
    // any coupon that was marked as a Welcome Coupon.
    //
    @Query("""
           select case
                    when count(cr) > 0 then true
                    else false
                  end
           from CouponRedemption cr
           where cr.provider.id = :providerId
             and cr.coupon.welcomeCoupon = true
           """)
    boolean existsWelcomeCouponRedemption(
            @Param("providerId") Long providerId
    );


    // ============================================================
    // FIND SAME PROVIDER + SAME COUPON
    // ============================================================

    Optional<CouponRedemption>
    findByCouponIdAndProviderId(
            Long couponId,
            Long providerId
    );


    // ============================================================
    // COUNT COUPON REDEMPTIONS
    // ============================================================

    long countByCouponId(
            Long couponId
    );
}