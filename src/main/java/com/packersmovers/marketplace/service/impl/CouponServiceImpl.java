package com.packersmovers.marketplace.service.impl;

import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.dto.coupon.CouponResponse;
import com.packersmovers.marketplace.dto.coupon.CouponValidationResponse;
import com.packersmovers.marketplace.dto.coupon.CreateCouponRequest;
import com.packersmovers.marketplace.dto.coupon.UpdateCouponRequest;
import com.packersmovers.marketplace.entity.Coupon;
import com.packersmovers.marketplace.entity.CouponRedemption;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.entity.User;
import com.packersmovers.marketplace.repository.CouponRedemptionRepository;
import com.packersmovers.marketplace.repository.CouponRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.repository.UserRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.CouponService;
import com.packersmovers.marketplace.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    private final CouponRedemptionRepository couponRedemptionRepository;

    private final ProviderRepository providerRepository;

    private final UserRepository userRepository;

    private final WalletService walletService;

    private final AuditService auditService;


    // ============================================================
    // CREATE COUPON
    // ============================================================

    @Override
    @Transactional
    public CouponResponse createCoupon(
            CreateCouponRequest request,
            Long adminUserId
    ) {

        if (request == null) {
            throw new BadRequestException(
                    "Coupon request is required"
            );
        }

        if (adminUserId == null) {
            throw new BadRequestException(
                    "Admin user ID is required"
            );
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of(
                                "User",
                                adminUserId
                        )
                );

        String code = normalizeCode(
                request.getCode()
        );

        if (code.isBlank()) {
            throw new BadRequestException(
                    "Coupon code is required"
            );
        }

        if (request.getAmount() == null
                || request.getAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new BadRequestException(
                    "Coupon amount must be greater than zero"
            );
        }

        if (request.getMaxUses() != null
                && request.getMaxUses() <= 0) {

            throw new BadRequestException(
                    "Maximum uses must be greater than zero"
            );
        }

        if (request.getExpiresAt() != null
                && !request.getExpiresAt()
                .isAfter(Instant.now())) {

            throw new BadRequestException(
                    "Coupon expiry must be in the future"
            );
        }

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new BadRequestException(
                    "Coupon code already exists"
            );
        }


        Coupon coupon = Coupon.builder()
                .code(code)
                .amount(request.getAmount())
                .welcomeCoupon(request.isWelcomeCoupon())
                .active(
                        request.getActive() == null
                                || request.getActive()
                )
                .expiresAt(request.getExpiresAt())
                .maxUses(request.getMaxUses())
                .usedCount(0)
                .createdBy(admin)
                .build();


        coupon = couponRepository.save(coupon);


        auditService.log(
                "COUPON_CREATED",
                "Coupon",
                coupon.getId(),
                null,
                coupon.getCode(),
                "Coupon created with amount ₹"
                        + coupon.getAmount()
                        + ", welcomeCoupon="
                        + coupon.isWelcomeCoupon()
                        + " by admin user "
                        + adminUserId
        );


        return toResponse(coupon);
    }


    // ============================================================
    // UPDATE COUPON
    // ============================================================

    @Override
    @Transactional
    public CouponResponse updateCoupon(
            Long couponId,
            UpdateCouponRequest request
    ) {

        if (couponId == null) {
            throw new BadRequestException(
                    "Coupon ID is required"
            );
        }

        if (request == null) {
            throw new BadRequestException(
                    "Coupon update request is required"
            );
        }


        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of(
                                "Coupon",
                                couponId
                        )
                );


        String oldCode = coupon.getCode();

        BigDecimal oldAmount = coupon.getAmount();

        boolean oldWelcomeCoupon =
                coupon.isWelcomeCoupon();


        // --------------------------------------------------------
        // CODE
        // --------------------------------------------------------

        if (request.getCode() != null
                && !request.getCode().isBlank()) {

            String newCode =
                    normalizeCode(
                            request.getCode()
                    );


            if (!newCode.equalsIgnoreCase(
                    coupon.getCode()
            )
                    && couponRepository
                    .existsByCodeIgnoreCase(newCode)) {

                throw new BadRequestException(
                        "Coupon code already exists"
                );
            }


            coupon.setCode(newCode);
        }


        // --------------------------------------------------------
        // AMOUNT
        // --------------------------------------------------------

        if (request.getAmount() != null) {

            if (request.getAmount()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new BadRequestException(
                        "Coupon amount must be greater than zero"
                );
            }


            coupon.setAmount(
                    request.getAmount()
            );
        }


        // --------------------------------------------------------
        // WELCOME COUPON
        // --------------------------------------------------------

        if (request.getWelcomeCoupon() != null) {

            /*
             * Do not allow changing an already-used coupon
             * from normal -> welcome or welcome -> normal.
             *
             * This keeps redemption history consistent.
             */
            if (request.getWelcomeCoupon()
                    != coupon.isWelcomeCoupon()
                    && coupon.getUsedCount() > 0) {

                throw new BadRequestException(
                        "Cannot change welcome coupon type after the coupon has been redeemed"
                );
            }


            coupon.setWelcomeCoupon(
                    request.getWelcomeCoupon()
            );
        }


        // --------------------------------------------------------
        // ACTIVE
        // --------------------------------------------------------

        if (request.getActive() != null) {

            coupon.setActive(
                    request.getActive()
            );
        }


        // --------------------------------------------------------
        // EXPIRY
        // --------------------------------------------------------

        if (request.getExpiresAt() != null) {

            if (!request.getExpiresAt()
                    .isAfter(Instant.now())) {

                throw new BadRequestException(
                        "Coupon expiry must be in the future"
                );
            }


            coupon.setExpiresAt(
                    request.getExpiresAt()
            );
        }


        // --------------------------------------------------------
        // MAX USES
        // --------------------------------------------------------

        if (request.getMaxUses() != null) {

            if (request.getMaxUses() <= 0) {

                throw new BadRequestException(
                        "Maximum uses must be greater than zero"
                );
            }


            if (request.getMaxUses()
                    < coupon.getUsedCount()) {

                throw new BadRequestException(
                        "Maximum uses cannot be lower than current usage"
                );
            }


            coupon.setMaxUses(
                    request.getMaxUses()
            );
        }


        coupon = couponRepository.save(coupon);


        auditService.log(
                "COUPON_UPDATED",
                "Coupon",
                coupon.getId(),
                oldCode
                        + " / "
                        + oldAmount
                        + " / welcome="
                        + oldWelcomeCoupon,
                coupon.getCode()
                        + " / "
                        + coupon.getAmount()
                        + " / welcome="
                        + coupon.isWelcomeCoupon(),
                "Coupon updated"
        );


        return toResponse(coupon);
    }


    // ============================================================
    // LIST COUPONS
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<CouponResponse> listCoupons() {

        return couponRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }


    // ============================================================
    // GET COUPON
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CouponResponse getCoupon(
            Long couponId
    ) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of(
                                "Coupon",
                                couponId
                        )
                );


        return toResponse(coupon);
    }


    // ============================================================
    // ACTIVATE
    // ============================================================

    @Override
    @Transactional
    public CouponResponse activateCoupon(
            Long couponId
    ) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of(
                                "Coupon",
                                couponId
                        )
                );


        if (coupon.getExpiresAt() != null
                && !coupon.getExpiresAt()
                .isAfter(Instant.now())) {

            throw new BadRequestException(
                    "Cannot activate an expired coupon"
            );
        }


        if (coupon.getMaxUses() != null
                && coupon.getUsedCount()
                >= coupon.getMaxUses()) {

            throw new BadRequestException(
                    "Cannot activate a coupon that has reached its usage limit"
            );
        }


        coupon.setActive(true);

        coupon = couponRepository.save(coupon);


        auditService.log(
                "COUPON_ACTIVATED",
                "Coupon",
                coupon.getId(),
                "false",
                "true",
                "Coupon activated"
        );


        return toResponse(coupon);
    }


    // ============================================================
    // DEACTIVATE
    // ============================================================

    @Override
    @Transactional
    public CouponResponse deactivateCoupon(
            Long couponId
    ) {

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() ->
                        ResourceNotFoundException.of(
                                "Coupon",
                                couponId
                        )
                );


        coupon.setActive(false);

        coupon = couponRepository.save(coupon);


        auditService.log(
                "COUPON_DEACTIVATED",
                "Coupon",
                coupon.getId(),
                "true",
                "false",
                "Coupon deactivated"
        );


        return toResponse(coupon);
    }


    // ============================================================
    // VALIDATE COUPON
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public CouponValidationResponse validateCoupon(
            String code
    ) {

        String normalizedCode =
                normalizeCode(code);


        if (normalizedCode.isBlank()) {

            return invalidResponse(
                    normalizedCode,
                    "Coupon code is required"
            );
        }


        Coupon coupon =
                couponRepository
                        .findByCodeIgnoreCase(
                                normalizedCode
                        )
                        .orElse(null);


        if (coupon == null) {

            return invalidResponse(
                    normalizedCode,
                    "Invalid coupon code"
            );
        }


        Instant now = Instant.now();


        if (!coupon.isActive()) {

            return invalidResponse(
                    coupon.getCode(),
                    "Coupon is inactive"
            );
        }


        if (coupon.getExpiresAt() != null
                && !coupon.getExpiresAt()
                .isAfter(now)) {

            return invalidResponse(
                    coupon.getCode(),
                    "Coupon has expired"
            );
        }


        if (coupon.getMaxUses() != null
                && coupon.getUsedCount()
                >= coupon.getMaxUses()) {

            return invalidResponse(
                    coupon.getCode(),
                    "Coupon usage limit has been reached"
            );
        }


        return CouponValidationResponse.builder()
                .valid(true)
                .code(coupon.getCode())
                .amount(coupon.getAmount())
                .welcomeCoupon(coupon.isWelcomeCoupon())
                .message(
                        "Coupon is valid"
                )
                .build();
    }


    // ============================================================
    // REDEEM COUPON
    // ============================================================

    @Override
    @Transactional
    public Coupon redeemCoupon(
            String code,
            Long providerId
    ) {

        if (providerId == null) {

            throw new BadRequestException(
                    "Provider ID is required"
            );
        }


        String normalizedCode =
                normalizeCode(code);


        if (normalizedCode.isBlank()) {

            throw new BadRequestException(
                    "Coupon code is required"
            );
        }


        /*
         * Lock coupon row.
         *
         * This prevents concurrent requests from consuming
         * the same remaining coupon slot.
         */
        Coupon coupon =
                couponRepository
                        .findByCodeForUpdate(
                                normalizedCode
                        )
                        .orElseThrow(() ->
                                new BadRequestException(
                                        "Invalid coupon code"
                                )
                        );


        Instant now = Instant.now();


        // --------------------------------------------------------
        // ACTIVE
        // --------------------------------------------------------

        if (!coupon.isActive()) {

            throw new BadRequestException(
                    "Coupon is inactive"
            );
        }


        // --------------------------------------------------------
        // EXPIRY
        // --------------------------------------------------------

        if (coupon.getExpiresAt() != null
                && !coupon.getExpiresAt()
                .isAfter(now)) {

            throw new BadRequestException(
                    "Coupon has expired"
            );
        }


        // --------------------------------------------------------
        // MAX USES
        // --------------------------------------------------------

        if (coupon.getMaxUses() != null
                && coupon.getUsedCount()
                >= coupon.getMaxUses()) {

            throw new BadRequestException(
                    "Coupon usage limit has been reached"
            );
        }


        // --------------------------------------------------------
        // SAME COUPON + SAME PROVIDER
        // --------------------------------------------------------
        //
        // A provider cannot redeem the exact same coupon
        // more than once.
        //

        if (couponRedemptionRepository
                .existsByCouponIdAndProviderId(
                        coupon.getId(),
                        providerId
                )) {

            throw new BadRequestException(
                    "This coupon has already been redeemed"
            );
        }


        // --------------------------------------------------------
        // WELCOME COUPON RULE
        // --------------------------------------------------------
        //
        // A provider can redeem only ONE welcome coupon
        // during the lifetime of the provider account.
        //
        // Normal coupons are NOT affected by this check.
        //

        if (coupon.isWelcomeCoupon()
                && couponRedemptionRepository
                .existsWelcomeCouponRedemption(
                        providerId
                )) {

            throw new BadRequestException(
                    "This provider has already redeemed a welcome coupon"
            );
        }


        // --------------------------------------------------------
        // PROVIDER
        // --------------------------------------------------------

        Provider provider =
                providerRepository.findById(providerId)
                        .orElseThrow(() ->
                                ResourceNotFoundException.of(
                                        "Provider",
                                        providerId
                                )
                        );


        // --------------------------------------------------------
        // WALLET CREDIT
        // --------------------------------------------------------
        //
        // Existing atomic WalletService is used.
        //

        walletService.credit(
                providerId,
                coupon.getAmount(),
                TransactionReferenceType.COUPON,
                coupon.getId(),
                (
                        coupon.isWelcomeCoupon()
                                ? "Welcome coupon "
                                : "Coupon "
                )
                        + coupon.getCode()
                        + " credited ₹"
                        + coupon.getAmount()
        );


        // --------------------------------------------------------
        // SAVE REDEMPTION
        // --------------------------------------------------------

        CouponRedemption redemption =
                CouponRedemption.builder()
                        .coupon(coupon)
                        .provider(provider)
                        .amount(coupon.getAmount())
                        .redeemedAt(now)
                        .build();


        couponRedemptionRepository.save(
                redemption
        );


        // --------------------------------------------------------
        // INCREMENT USAGE
        // --------------------------------------------------------

        coupon.setUsedCount(
                coupon.getUsedCount() + 1
        );


        couponRepository.save(coupon);


        // --------------------------------------------------------
        // AUDIT
        // --------------------------------------------------------

        auditService.log(
                "COUPON_REDEEMED",
                "Coupon",
                coupon.getId(),
                null,
                providerId.toString(),
                "Coupon "
                        + coupon.getCode()
                        + " redeemed by provider "
                        + providerId
                        + " for ₹"
                        + coupon.getAmount()
                        + ", welcomeCoupon="
                        + coupon.isWelcomeCoupon()
        );


        return coupon;
    }


    // ============================================================
    // RESPONSE MAPPER
    // ============================================================

    private CouponResponse toResponse(
            Coupon coupon
    ) {

        Instant now = Instant.now();


        boolean expired =
                coupon.getExpiresAt() != null
                        && !coupon.getExpiresAt()
                        .isAfter(now);


        boolean usageLimitReached =
                coupon.getMaxUses() != null
                        && coupon.getUsedCount()
                        >= coupon.getMaxUses();


        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .amount(coupon.getAmount())
                .welcomeCoupon(
                        coupon.isWelcomeCoupon()
                )
                .active(coupon.isActive())
                .expiresAt(coupon.getExpiresAt())
                .maxUses(coupon.getMaxUses())
                .usedCount(coupon.getUsedCount())
                .expired(expired)
                .usageLimitReached(
                        usageLimitReached
                )
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }


    // ============================================================
    // NORMALIZE CODE
    // ============================================================

    private String normalizeCode(
            String code
    ) {

        if (code == null) {
            return "";
        }


        return code
                .trim()
                .toUpperCase();
    }


    // ============================================================
    // INVALID RESPONSE
    // ============================================================

    private CouponValidationResponse invalidResponse(
            String code,
            String message
    ) {

        return CouponValidationResponse.builder()
                .valid(false)
                .code(code)
                .amount(null)
                .welcomeCoupon(false)
                .message(message)
                .build();
    }
}