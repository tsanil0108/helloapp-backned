package com.packersmovers.marketplace.service;

public interface CaptchaVerificationService {
    void verify(String token, String clientIp);
}
