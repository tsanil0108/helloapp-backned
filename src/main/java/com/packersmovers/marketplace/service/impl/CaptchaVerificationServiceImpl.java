package com.packersmovers.marketplace.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.packersmovers.marketplace.common.exception.BadRequestException;
import com.packersmovers.marketplace.service.CaptchaVerificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class CaptchaVerificationServiceImpl implements CaptchaVerificationService {
    private final RestClient.Builder restClientBuilder;

    @Value("${app.captcha.enabled:false}")
    private boolean enabled;

    @Value("${app.captcha.secret:}")
    private String secret;

    @Value("${app.captcha.verify-url:https://www.google.com/recaptcha/api/siteverify}")
    private String verifyUrl;

    @Override
    public void verify(String token, String clientIp) {
        if (!StringUtils.hasText(token)) throw new BadRequestException("CAPTCHA verification failed. Please retry.");
        if (!enabled) return; // Development mode: require token presence only.
        if (!StringUtils.hasText(secret)) throw new IllegalStateException("CAPTCHA is enabled but secret is not configured");

        CaptchaResponse result = restClientBuilder.build().post()
                .uri(verifyUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("secret=" + encode(secret) + "&response=" + encode(token)
                        + (StringUtils.hasText(clientIp) ? "&remoteip=" + encode(clientIp) : ""))
                .retrieve()
                .body(CaptchaResponse.class);

        if (result == null || !result.isSuccess()) {
            throw new BadRequestException("CAPTCHA verification failed. Please retry.");
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Data
    public static class CaptchaResponse {
        private boolean success;
        @JsonProperty("challenge_ts") private String challengeTs;
        private String hostname;
    }
}
