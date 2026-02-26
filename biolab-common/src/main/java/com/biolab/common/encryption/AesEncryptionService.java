package com.biolab.common.encryption;

import com.biolab.common.security.SecurityConstants;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption service for PII column-level encryption.
 *
 * <h3>Compliance (Slide 9 &amp; 10):</h3>
 * <ul>
 *   <li>HIPAA: PHI encryption at rest (AES-256)</li>
 *   <li>GDPR: Privacy by design — encrypted PII columns</li>
 *   <li>Key rotation support: configurable via {@code app.encryption.key}</li>
 * </ul>
 *
 * <h3>Encrypted Fields (per schema):</h3>
 * <p>Phone numbers, addresses, and other PII stored in User/Organization
 * entities use this service via {@link EncryptedStringConverter}.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
public class AesEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.encryption.key:#{null}}")
    private String configuredKey;

    private SecretKey secretKey;

    /**
     * Initializes the encryption key. Uses configured key if provided,
     * otherwise generates a new one (development mode).
     */
    @PostConstruct
    public void init() {
        try {
            if (configuredKey != null && !configuredKey.isBlank()) {
                byte[] keyBytes = Base64.getDecoder().decode(configuredKey);
                this.secretKey = new SecretKeySpec(keyBytes, "AES");
                log.info("AES-256 encryption initialized with configured key");
            } else {
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(SecurityConstants.ENCRYPTION_KEY_SIZE);
                this.secretKey = keyGen.generateKey();
                String encoded = Base64.getEncoder().encodeToString(secretKey.getEncoded());
                log.warn("AES-256 encryption initialized with GENERATED key (dev only). "
                       + "Set app.encryption.key={} for production", encoded);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize AES encryption", e);
        }
    }

    /**
     * Encrypts plaintext using AES-256-GCM.
     *
     * @param plaintext the string to encrypt
     * @return Base64-encoded ciphertext (IV prepended)
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[SecurityConstants.GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(SecurityConstants.ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

            // Prepend IV to ciphertext
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts AES-256-GCM ciphertext.
     *
     * @param ciphertext Base64-encoded ciphertext (IV prepended)
     * @return decrypted plaintext
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            byte[] combined = Base64.getDecoder().decode(ciphertext);
            byte[] iv = new byte[SecurityConstants.GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - iv.length];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(SecurityConstants.ENCRYPTION_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
