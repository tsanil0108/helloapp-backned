package com.packersmovers.marketplace.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.packersmovers.marketplace.common.enums.PaymentPurpose;
import com.packersmovers.marketplace.common.enums.PaymentStatus;
import com.packersmovers.marketplace.common.enums.TransactionReferenceType;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.common.exception.ResourceNotFoundException;
import com.packersmovers.marketplace.config.RazorpayProperties;
import com.packersmovers.marketplace.common.util.HmacUtil;
import com.packersmovers.marketplace.dto.payment.PaymentVerifyRequest;
import com.packersmovers.marketplace.dto.payment.RazorpayOrderResponse;
import com.packersmovers.marketplace.dto.wallet.WalletResponse;
import com.packersmovers.marketplace.entity.Payment;
import com.packersmovers.marketplace.entity.Provider;
import com.packersmovers.marketplace.repository.PaymentRepository;
import com.packersmovers.marketplace.repository.ProviderRepository;
import com.packersmovers.marketplace.service.AuditService;
import com.packersmovers.marketplace.service.PaymentService;
import com.packersmovers.marketplace.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Razorpay integration: order creation via REST (Orders API), signature verification via HMAC-SHA256,
 * and webhook handling as a second, authoritative confirmation path (payments can be verified either
 * by the client-side checkout callback OR the server-to-server webhook - whichever arrives).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private static final String RAZORPAY_ORDERS_URL = "https://api.razorpay.com/v1/orders";

    private final RazorpayProperties razorpayProperties;
    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;
    private final ProviderRepository providerRepository;
    private final WalletService walletService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public RazorpayOrderResponse createTopupOrder(Long providerId, BigDecimal amount) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> ResourceNotFoundException.of("Provider", providerId));

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // paise
        body.put("currency", "INR");
        body.put("receipt", "wallet-topup-" + providerId + "-" + Instant.now().toEpochMilli());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(razorpayProperties.keyId(), razorpayProperties.keySecret());

        String razorpayOrderId;
        try {
            var response = restTemplate.exchange(RAZORPAY_ORDERS_URL, HttpMethod.POST,
                    new HttpEntity<>(body, headers), JsonNode.class);
            razorpayOrderId = response.getBody() != null ? response.getBody().path("id").asText() : null;
            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                throw new BadRequestException("Razorpay did not return an order id");
            }
        } catch (Exception e) {
            log.error("Razorpay order creation failed", e);
            throw new BadRequestException("Unable to initiate payment right now. Please try again.");
        }

        Payment payment = Payment.builder()
                .provider(provider)
                .razorpayOrderId(razorpayOrderId)
                .amount(amount)
                .purpose(PaymentPurpose.WALLET_TOPUP)
                .status(PaymentStatus.CREATED)
                .build();
        payment = paymentRepository.save(payment);

        return RazorpayOrderResponse.builder()
                .paymentId(payment.getId())
                .razorpayOrderId(razorpayOrderId)
                .amount(amount)
                .currency("INR")
                .razorpayKeyId(razorpayProperties.keyId())
                .build();
    }

    @Override
    @Transactional
    public WalletResponse verifyAndCreditTopup(Long providerId, PaymentVerifyRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderIdForUpdate(request.getRazorpayOrderId())
                .orElseThrow(() -> new BadRequestException("Unknown payment order"));

        if (!payment.getProvider().getId().equals(providerId)) {
            throw new BadRequestException("This payment does not belong to the current provider");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return walletService.getWallet(providerId); // already processed - idempotent
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        boolean valid = HmacUtil.matches(payload, razorpayProperties.keySecret(), request.getRazorpaySignature());
        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            auditService.log("PAYMENT_SIGNATURE_INVALID", "Payment", payment.getId(), null, "FAILED", null);
            throw new BadRequestException("Payment verification failed. If money was deducted, it will be refunded automatically.");
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setVerifiedAt(Instant.now());
        paymentRepository.save(payment);

        walletService.credit(providerId, payment.getAmount(), TransactionReferenceType.RAZORPAY_TOPUP,
                payment.getId(), "Wallet top-up via Razorpay order " + payment.getRazorpayOrderId());

        auditService.log("PAYMENT_VERIFIED", "Payment", payment.getId(), null, "SUCCESS", null);
        return walletService.getWallet(providerId);
    }

    @Override
    @Transactional
    public void handleWebhook(String rawBody, String signatureHeader) {
        boolean valid = HmacUtil.matches(rawBody, razorpayProperties.webhookSecret(), signatureHeader == null ? "" : signatureHeader);
        if (!valid) {
            log.warn("Rejected Razorpay webhook: signature mismatch");
            throw new BadRequestException("Invalid webhook signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
            String razorpayOrderId = paymentEntity.path("order_id").asText(null);

            if (razorpayOrderId == null) {
                log.info("Webhook event {} without order_id - ignoring", event);
                return;
            }

            paymentRepository.findByRazorpayOrderIdForUpdate(razorpayOrderId).ifPresent(payment -> {
                if ("payment.captured".equals(event) && payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setRazorpayPaymentId(paymentEntity.path("id").asText(null));
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setVerifiedAt(Instant.now());
                    paymentRepository.save(payment);

                    walletService.credit(payment.getProvider().getId(), payment.getAmount(),
                            TransactionReferenceType.RAZORPAY_TOPUP, payment.getId(),
                            "Wallet top-up confirmed via webhook " + razorpayOrderId);

                    auditService.log("PAYMENT_WEBHOOK_CAPTURED", "Payment", payment.getId(), null, "SUCCESS", null);
                } else if ("payment.failed".equals(event)) {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(paymentEntity.path("error_description").asText(null));
                    paymentRepository.save(payment);
                    auditService.log("PAYMENT_WEBHOOK_FAILED", "Payment", payment.getId(), null, "FAILED", null);
                }
            });
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            throw new BadRequestException("Unable to process webhook payload");
        }
    }
}
