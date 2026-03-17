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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AES-256-GCM encryption service for PII column-level encryption.
 *
 * <h3>Sprint 2 — GAP-18: Key versioning for rotation</h3>
 * <p>Ciphertext is now prefixed with a 2-byte version tag: {@code v1:}, {@code v2:}, etc.
 * This allows multiple key versions to coexist during rotation.
 * Old ciphertexts encrypted with key-version {@code N} remain decryptable
 * while new writes use key-version {@code N+1}.
 * The {@link PiiEncryptionMigrator} re-encrypts old records to the current key version.</p>
 *
 * <h3>Ciphertext format (v1+)</h3>
 * <pre>
 *   v{version}:{Base64(IV + ciphertext + GCM-tag)}
 *   e.g.  v1:dGVzdGRhdGE...
 * </pre>
 *
 * <h3>Backward compatibility</h3>
 * <p>Values without a version prefix are treated as legacy plaintext or
 * single-key ciphertext from before versioning was introduced.
 * {@link #decrypt(String)} falls back gracefully for these cases.</p>
 *
 * <h3>Sprint 2 — GAP-12: Email deterministic encryption</h3>
 * <p>{@link #encryptDeterministic(String)} uses HMAC-SHA256 as a pseudorandom
 * function so that equal plaintexts always produce equal ciphertexts — enabling
 * indexed equality lookups on encrypted email columns without exposing plaintext.
 * The output is NOT secret; it is used purely for lookups. The display value
 * (firstName, lastName, phone) uses randomised IV as before.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Service
public class AesEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Primary encryption key (current version).
     * Format: {@code <version>:<base64-key>}
     * Example: {@code 1:gIl1mzWKEv1t6ZKwxma5d8n/E0u9AxJWs13lRlWFWEg=}
     */
    @Value("${app.encryption.key:#{null}}")
    private String configuredKey;

    /**
     * Previous key versions for decryption during rotation.
     * Format: comma-separated {@code <version>:<base64-key>} pairs.
     * Example: {@code 0:oldKeyBase64==}
     * Only needed during the transition period.
     */
    @Value("${app.encryption.legacy-keys:}")
    private String legacyKeys;

    /** Map of version → SecretKey for all known key versions. */
    private final Map<Integer, SecretKey> keyMap = new ConcurrentHashMap<>();

    /** Current (highest) key version — used for all new encryptions. */
    private int currentVersion = 1;

    /** Key used for HMAC-based deterministic encryption (email lookup). */
    private SecretKey hmacKey;

    @PostConstruct
    public void init() {
        try {
            if (configuredKey != null && !configuredKey.isBlank()) {
                parseAndLoadKey(configuredKey.trim());
                log.info("AES-256 encryption initialised with configured key (version {})",
                        currentVersion);
            } else {
                // Dev mode — generate ephemeral key at version 1
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(SecurityConstants.ENCRYPTION_KEY_SIZE);
                SecretKey generated = keyGen.generateKey();
                keyMap.put(1, generated);
                currentVersion = 1;
                hmacKey = generated; // use same key for HMAC in dev
                String b64 = Base64.getEncoder().encodeToString(generated.getEncoded());
                log.warn("AES-256 encryption initialised with GENERATED key (dev only). "
                        + "Set app.encryption.key=1:{} for production", b64);
            }

            // Load legacy keys for decryption-only use during rotation
            if (legacyKeys != null && !legacyKeys.isBlank()) {
                for (String entry : legacyKeys.split(",")) {
                    entry = entry.trim();
                    if (!entry.isEmpty()) {
                        parseAndLoadKey(entry);
                    }
                }
                log.info("Loaded {} key version(s) total (current=v{})",
                        keyMap.size(), currentVersion);
            }

        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialise AES encryption", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Randomised encryption (firstName, lastName, phone)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Encrypts plaintext using AES-256-GCM with a random IV.
     * Output format: {@code v{version}:{Base64(IV+ciphertext+tag)}}
     */
    public String encrypt(String plaintext) {
        if (plaintext == null) return null;
        try {
            byte[] iv = new byte[SecurityConstants.GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(SecurityConstants.ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keyMap.get(currentVersion),
                    new GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return "v" + currentVersion + ":" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a versioned ciphertext.
     * Supports legacy (unversioned) values for backward compatibility.
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null) return null;
        try {
            int version = currentVersion;
            String payload = ciphertext;

            if (ciphertext.matches("^v\\d+:.*")) {
                int colon = ciphertext.indexOf(':');
                version = Integer.parseInt(ciphertext.substring(1, colon));
                payload = ciphertext.substring(colon + 1);
            }

            SecretKey key = keyMap.get(version);
            if (key == null) {
                log.error("No key found for version {}, known versions: {}",
                        version, keyMap.keySet());
                throw new RuntimeException("Unknown encryption key version: " + version);
            }

            byte[] combined = Base64.getDecoder().decode(payload);
            byte[] iv       = new byte[SecurityConstants.GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - iv.length];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance(SecurityConstants.ENCRYPTION_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key,
                    new GCMParameterSpec(SecurityConstants.GCM_TAG_LENGTH, iv));

            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Deterministic encryption (email lookup — GAP-12)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Produces a deterministic, versioned hash of the input using HMAC-SHA256.
     * Equal inputs always produce equal outputs — suitable for indexed lookups.
     * Output: {@code dv{version}:{Base64-HMAC-SHA256}}
     *
     * <p><b>Security note:</b> This is NOT encryption — the output leaks
     * equality. Use only for lookup keys, never for display. The plaintext
     * display value must be separately encrypted with {@link #encrypt(String)}.
     */
    public String encryptDeterministic(String plaintext) {
        if (plaintext == null) return null;
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey.getEncoded(), "HmacSHA256"));
            byte[] hmac = mac.doFinal(
                    plaintext.toLowerCase().trim()
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "dv" + currentVersion + ":" + Base64.getEncoder().encodeToString(hmac);
        } catch (Exception e) {
            throw new RuntimeException("Deterministic encryption failed", e);
        }
    }

    /**
     * Returns true if the stored value is already encrypted with the current key version.
     * Used by {@link PiiEncryptionMigrator} to skip already-migrated records.
     */
    public boolean isCurrentVersion(String value) {
        if (value == null) return false;
        return value.startsWith("v" + currentVersion + ":") ||
                value.startsWith("dv" + currentVersion + ":");
    }

    /** Returns the current key version number. */
    public int getCurrentVersion() { return currentVersion; }

    // ─────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────

    private void parseAndLoadKey(String entry) {
        if (entry.contains(":")) {
            int colon = entry.indexOf(':');
            int version = Integer.parseInt(entry.substring(0, colon).trim());
            byte[] keyBytes = Base64.getDecoder().decode(entry.substring(colon + 1).trim());
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            keyMap.put(version, key);
            if (version > currentVersion) {
                currentVersion = version;
                hmacKey = key;  // use highest version key for HMAC
            } else if (!keyMap.containsKey(currentVersion)) {
                hmacKey = key;
            }
        } else {
            // Legacy format: bare Base64 key — treat as version 1
            byte[] keyBytes = Base64.getDecoder().decode(entry);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            keyMap.put(1, key);
            currentVersion = 1;
            hmacKey = key;
        }
    }
}