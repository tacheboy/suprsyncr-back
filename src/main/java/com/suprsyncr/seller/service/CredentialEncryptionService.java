package com.suprsyncr.seller.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service for encrypting and decrypting platform credentials using AES-256.
 */
@Service
public class CredentialEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private final SecretKey secretKey;
    
    public CredentialEncryptionService(@Value("${app.encryption.key}") String encryptionKey) {
        // Ensure the key is 32 bytes for AES-256
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Encryption key must be 32 bytes for AES-256");
        }
        this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
    }
    
    /**
     * Encrypts plaintext credentials.
     * 
     * @param plaintext the plaintext to encrypt
     * @return Base64-encoded encrypted string
     */
    public String encrypt(String plaintext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt credentials", e);
        }
    }
    
    /**
     * Decrypts encrypted credentials.
     * 
     * @param ciphertext the Base64-encoded encrypted string
     * @return decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(ciphertext);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt credentials", e);
        }
    }
}

