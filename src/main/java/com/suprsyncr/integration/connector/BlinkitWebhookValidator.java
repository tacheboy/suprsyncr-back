package com.suprsyncr.integration.connector;

import com.suprsyncr.seller.entity.PlatformType;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Blinkit webhook signature validator.
 * Validates webhook authenticity using HMAC-SHA256 signature verification.
 */
@Component
public class BlinkitWebhookValidator implements WebhookValidator {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    
    @Override
    public PlatformType getPlatformType() {
        return PlatformType.BLINKIT;
    }
    
    @Override
    public boolean validateSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }
        
        try {
            // Compute HMAC-SHA256 of the payload
            String computedSignature = computeHmacSha256(payload, secret);
            
            // Use constant-time comparison to prevent timing attacks
            return constantTimeEquals(computedSignature, signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Computes HMAC-SHA256 signature of the payload using the secret key.
     * Returns Base64-encoded signature.
     */
    private String computeHmacSha256(String payload, String secret) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
        mac.init(secretKeySpec);
        
        byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(hmacBytes);
    }
    
    /**
     * Performs constant-time string comparison to prevent timing attacks.
     * Compares two strings byte-by-byte without short-circuiting.
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        
        // If lengths differ, still compare to prevent timing attacks
        if (aBytes.length != bBytes.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        
        return result == 0;
    }
}
