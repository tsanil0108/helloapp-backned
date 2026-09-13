package com.packersmovers.marketplace.controller;

import com.packersmovers.marketplace.common.response.ApiResponse;
import com.packersmovers.marketplace.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Server-to-server confirmation path for Razorpay events (payment.captured / payment.failed).
 * This is deliberately public (see SecurityConfig PUBLIC_ENDPOINTS) since Razorpay itself calls it -
 * authenticity is instead guaranteed by the X-Razorpay-Signature HMAC check inside PaymentService.
 */
@RestController
@RequestMapping("/api/payments/webhook")
@RequiredArgsConstructor
@Tag(name = "Payment Webhook", description = "Razorpay server-to-server payment confirmation")
public class PaymentWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay")
    public ApiResponse<Void> razorpayWebhook(HttpServletRequest request) throws IOException {
        String rawBody = new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String signature = request.getHeader("X-Razorpay-Signature");
        paymentService.handleWebhook(rawBody, signature);
        return ApiResponse.success("Webhook processed", null);
    }
}
