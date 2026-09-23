package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.enums.KycDocType;
import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.dto.common.PageResponse;
import com.packersmovers.marketplace.dto.payment.PaymentVerifyRequest;
import com.packersmovers.marketplace.dto.payment.RazorpayOrderResponse;
import com.packersmovers.marketplace.dto.provider.KycUploadRequest;
import com.packersmovers.marketplace.dto.provider.ProviderProfileResponse;
import com.packersmovers.marketplace.dto.wallet.AddMoneyRequest;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;
import com.packersmovers.marketplace.dto.wallet.WalletTransactionResponse;
import com.packersmovers.marketplace.security.CustomUserPrincipal;
import com.packersmovers.marketplace.service.PaymentService;
import com.packersmovers.marketplace.service.ProviderService;
import com.packersmovers.marketplace.service.WalletService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Provider's own account:
 *
 * - Profile
 * - KYC document upload
 * - Wallet
 * - Razorpay top-up
 */
@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@Tag(
        name = "Provider Account",
        description = "Profile, KYC and wallet for the logged-in provider"
)
public class ProviderController {

    private final ProviderService providerService;
    private final WalletService walletService;
    private final PaymentService paymentService;


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

        /*
         * Convert the document type received from the mobile app
         * into the backend KycDocType enum.
         *
         * Example:
         * "GST_CERTIFICATE"
         * "PAN_CARD"
         * "AADHAAR_CARD"
         */
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


        /*
         * Build the DTO expected by ProviderService.
         */
        KycUploadRequest request = new KycUploadRequest();

        request.setDocType(kycDocType);


        /*
         * Save the uploaded file and KYC record.
         */
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
}