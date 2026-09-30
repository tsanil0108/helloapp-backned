package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.enums.KycDocType;
import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.lead.LeadAssignmentHistoryResponse;
import com.packersmovers.marketplace.dto.lead.LeadAssignmentStatusRequest;
import com.packersmovers.marketplace.dto.lead.ProviderLeadStatsResponse;
import com.packersmovers.marketplace.dto.payment.PaymentVerifyRequest;
import com.packersmovers.marketplace.dto.payment.RazorpayOrderResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import com.packersmovers.marketplace.dto.wallet.AddMoneyRequest;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;
import com.packersmovers.marketplace.dto.wallet.WalletTransactionResponse;
import com.packersmovers.marketplace.entity.Coupon;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.CouponService;
import com.packersmovers.marketplace.service.PaymentService;
import com.packersmovers.marketplace.service.ProviderService;
import com.packersmovers.marketplace.service.WalletService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Provider's own account:
 *
 * - Profile
 * - KYC document upload
 * - Wallet
 * - Wallet transactions
 * - Coupon redemption
 * - Razorpay top-up
 * - Lead lifecycle
 * - Lead statistics
 * - Lead history
 */
@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@Tag(
        name = "Provider Account",
        description = "Profile, KYC, wallet and lead management for the logged-in provider"
)
public class ProviderController {

    private final ProviderService providerService;
    private final WalletService walletService;
    private final PaymentService paymentService;
    private final CouponService couponService;


    // ============================================================
    // PROFILE
    // ============================================================

    @GetMapping("/me")
    public ApiResponse<ProviderProfileResponse> me(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        return ApiResponse.success(
                providerService.getMyProfile(
                        principal.getUserId()
                )
        );
    }


    // ============================================================
    // KYC DOCUMENT UPLOAD
    // ============================================================

    @PutMapping(
            value = "/kyc",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<Void> uploadKyc(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestPart("docType")
            String docType,

            @RequestPart("file")
            MultipartFile file
    ) {

        KycDocType kycDocType;

        try {

            kycDocType = KycDocType.valueOf(
                    docType.trim().toUpperCase()
            );

        } catch (IllegalArgumentException | NullPointerException ex) {

            throw new IllegalArgumentException(
                    "Invalid KYC document type: " + docType
            );
        }


        KycUploadRequest request =
                new KycUploadRequest();

        request.setDocType(
                kycDocType
        );


        providerService.uploadKycDocument(
                principal.getUserId(),
                request,
                file
        );


        return ApiResponse.success(
                "Document uploaded and submitted for verification",
                null
        );
    }


    // ============================================================
    // WALLET
    // ============================================================

    @GetMapping("/wallet")
    public ApiResponse<WalletResponse> wallet(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        Long providerId =
                providerService.resolveProviderId(
                        principal.getUserId()
                );


        return ApiResponse.success(
                walletService.getWallet(
                        providerId
                )
        );
    }


    // ============================================================
    // WALLET TRANSACTIONS
    // ============================================================

    @GetMapping("/wallet/transactions")
    public ApiResponse<PageResponse<WalletTransactionResponse>>
    walletTransactions(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam(
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    defaultValue = "20"
            )
            int size
    ) {

        Long providerId =
                providerService.resolveProviderId(
                        principal.getUserId()
                );


        return ApiResponse.success(
                walletService.getTransactions(
                        providerId,
                        page,
                        size
                )
        );
    }


    // ============================================================
    // COUPON REDEEM
    // ============================================================

    /**
     * Redeem coupon for the currently authenticated provider.
     *
     * Endpoint:
     *
     * POST /api/provider/wallet/coupon/redeem
     *
     * Request:
     *
     * {
     *     "code": "HELLO200"
     * }
     *
     * Provider ID is NOT accepted from frontend.
     * It is resolved from the authenticated JWT.
     *
     * CouponService handles:
     *
     * - Coupon validation
     * - Active check
     * - Expiry check
     * - Maximum usage check
     * - One coupon redemption per provider
     * - Wallet credit
     * - Wallet ledger
     * - Coupon usage increment
     * - Audit log
     */
    @PostMapping("/wallet/coupon/redeem")
    public ApiResponse<Coupon> redeemCoupon(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestBody
            Map<String, String> request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Coupon request is required"
            );
        }


        String code =
                request.get("code");


        if (code == null || code.isBlank()) {

            throw new IllegalArgumentException(
                    "Coupon code is required"
            );
        }


        Long providerId =
                providerService.resolveProviderId(
                        principal.getUserId()
                );


        Coupon coupon =
                couponService.redeemCoupon(
                        code,
                        providerId
                );


        return ApiResponse.success(
                "Coupon redeemed and wallet credited successfully",
                coupon
        );
    }


    // ============================================================
    // RAZORPAY CREATE ORDER
    // ============================================================

    @PostMapping("/wallet/topup/order")
    public ApiResponse<RazorpayOrderResponse>
    createTopupOrder(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @Valid
            @RequestBody
            AddMoneyRequest request
    ) {

        Long providerId =
                providerService.resolveProviderId(
                        principal.getUserId()
                );


        return ApiResponse.success(
                paymentService.createTopupOrder(
                        providerId,
                        request.getAmount()
                )
        );
    }


    // ============================================================
    // RAZORPAY VERIFY
    // ============================================================

    @PostMapping("/wallet/topup/verify")
    public ApiResponse<WalletResponse>
    verifyTopup(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @Valid
            @RequestBody
            PaymentVerifyRequest request
    ) {

        Long providerId =
                providerService.resolveProviderId(
                        principal.getUserId()
                );


        return ApiResponse.success(
                "Wallet credited",
                paymentService.verifyAndCreditTopup(
                        providerId,
                        request
                )
        );
    }


    // ============================================================
    // LEAD STATUS UPDATE
    // ============================================================

    /**
     * Provider updates the lifecycle of an unlocked lead.
     *
     * Examples:
     *
     * CONTACTED
     * QUOTE_SENT
     * NEGOTIATION
     * BOOKED
     * SERVICE_IN_PROGRESS
     * COMPLETED
     * LOST
     */
    @PostMapping(
            "/leads/{assignmentId}/status"
    )
    public ApiResponse<Void> updateLeadStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @PathVariable
            Long assignmentId,

            @Valid
            @RequestBody
            LeadAssignmentStatusRequest request
    ) {

        providerService.updateLeadAssignmentStatus(
                principal.getUserId(),
                assignmentId,
                request
        );


        return ApiResponse.success(
                "Lead status updated successfully",
                null
        );
    }


    // ============================================================
    // LEAD STATISTICS
    // ============================================================

    /**
     * Provider performance summary.
     *
     * Shows:
     *
     * - Assigned
     * - Viewed
     * - Unlocked
     * - Contacted
     * - Quote sent
     * - Negotiation
     * - Booked
     * - Service in progress
     * - Completed
     * - Lost
     * - Expired
     * - Cancelled
     */
    @GetMapping("/leads/stats")
    public ApiResponse<ProviderLeadStatsResponse> getLeadStats(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        return ApiResponse.success(
                "Lead statistics fetched successfully",
                providerService.getMyLeadStats(
                        principal.getUserId()
                )
        );
    }


    // ============================================================
    // COMPLETE LEAD HISTORY
    // ============================================================

    /**
     * Returns the provider's complete lead assignment history.
     *
     * This is intentionally provider-scoped.
     * Provider ID is resolved from the JWT.
     */
    @GetMapping("/leads/history")
    public ApiResponse<List<LeadAssignmentHistoryResponse>>
    getLeadHistory(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        return ApiResponse.success(
                "Lead history fetched successfully",
                providerService.listMyLeadHistory(
                        principal.getUserId()
                )
        );
    }
}