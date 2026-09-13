package com.packersmovers.marketplace.service;

import com.packersmovers.marketplace.dto.payment.PaymentVerifyRequest;
import com.packersmovers.marketplace.dto.payment.RazorpayOrderResponse;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;

import java.math.BigDecimal;

public interface PaymentService {

    /** Creates a Razorpay order for a provider's wallet top-up and records a PENDING Payment row. */
    RazorpayOrderResponse createTopupOrder(Long providerId, BigDecimal amount);

    /** Verifies the client-side Razorpay checkout signature, then credits the provider's wallet. */
    WalletResponse verifyAndCreditTopup(Long providerId, PaymentVerifyRequest request);

    /** Handles Razorpay server-to-server webhook events (payment.captured / payment.failed / refund.processed). */
    void handleWebhook(String rawBody, String signatureHeader);
}
