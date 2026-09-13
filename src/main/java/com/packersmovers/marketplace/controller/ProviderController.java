package com.packersmovers.marketplace.controller;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Provider's own account: profile, KYC submission and the wallet / Razorpay top-up flow. */
@RestController
@RequestMapping("/api/provider")
@RequiredArgsConstructor
@Tag(name = "Provider Account", description = "Profile, KYC and wallet for the logged-in provider")
public class ProviderController {

    private final ProviderService providerService;
    private final WalletService walletService;
    private final PaymentService paymentService;

    @GetMapping("/me")
    public ApiResponse<ProviderProfileResponse> me(@AuthenticationPrincipal CustomUserPrincipal principal) {
        return ApiResponse.success(providerService.getMyProfile(principal.getUserId()));
    }

    @PutMapping("/kyc")
    public ApiResponse<Void> uploadKyc(@AuthenticationPrincipal CustomUserPrincipal principal,
                                        @Valid @RequestBody KycUploadRequest request) {
        providerService.uploadKycDocument(principal.getUserId(), request);
        return ApiResponse.success("Document submitted for verification", null);
    }

    @GetMapping("/wallet")
    public ApiResponse<WalletResponse> wallet(@AuthenticationPrincipal CustomUserPrincipal principal) {
        Long providerId = providerService.resolveProviderId(principal.getUserId());
        return ApiResponse.success(walletService.getWallet(providerId));
    }

    @GetMapping("/wallet/transactions")
    public ApiResponse<PageResponse<WalletTransactionResponse>> walletTransactions(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long providerId = providerService.resolveProviderId(principal.getUserId());
        return ApiResponse.success(walletService.getTransactions(providerId, page, size));
    }

    @PostMapping("/wallet/topup/order")
    public ApiResponse<RazorpayOrderResponse> createTopupOrder(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                                 @Valid @RequestBody AddMoneyRequest request) {
        Long providerId = providerService.resolveProviderId(principal.getUserId());
        return ApiResponse.success(paymentService.createTopupOrder(providerId, request.getAmount()));
    }

    @PostMapping("/wallet/topup/verify")
    public ApiResponse<WalletResponse> verifyTopup(@AuthenticationPrincipal CustomUserPrincipal principal,
                                                    @Valid @RequestBody PaymentVerifyRequest request) {
        Long providerId = providerService.resolveProviderId(principal.getUserId());
        return ApiResponse.success("Wallet credited", paymentService.verifyAndCreditTopup(providerId, request));
    }
}
