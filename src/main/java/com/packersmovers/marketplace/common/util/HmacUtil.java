package com.packersmovers.marketplace.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** HMAC-SHA256 helper used to verify Razorpay payment signatures and webhook signatures. */
public final class HmacUtil {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacUtil() {
    }

    public static String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            throw new IllegalStateException("Unable to compute HMAC signature", e);
        }
    }

    public static boolean matches(String data, String secret, String expectedSignatureHex) {
        String computed = hmacSha256Hex(data, secret);
        return java.security.MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                expectedSignatureHex.getBytes(StandardCharsets.UTF_8));
    }
}
