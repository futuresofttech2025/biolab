package com.biolab.common.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Base64;

/**
 * JPA {@link AttributeConverter} for transparent AES-256-GCM column encryption.
 *
 * <h3>Migration-safe decryption (plain-text passthrough)</h3>
 * <p>Existing rows written before encryption was enabled contain raw plain text,
 * not Base64-encoded ciphertext. Attempting to decrypt these causes:</p>
 * <pre>NegativeArraySizeException: -8  (combined.length - IV_LENGTH &lt; 0)</pre>
 *
 * <p>This converter detects unencrypted values by checking whether the stored
 * string is valid Base64 <em>and</em> long enough to contain at least one byte
 * of ciphertext after the 12-byte IV prefix (minimum decoded length = 13 bytes,
 * minimum Base64 length = ceil(13/3)*4 = 20 chars). Values that fail this check
 * are returned as-is (plain text) and a WARN is logged so operators know which
 * rows still need migration.</p>
 *
 * <p><strong>Remove this passthrough after running the data migration.</strong>
 * See {@code V15__encrypt_existing_pii.sql} and {@code PiiEncryptionMigrator}.</p>
 *
 * @author BioLab Engineering Team
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);

    /** Minimum Base64 length for a value that has been encrypted:
     *  12-byte IV + 1-byte ciphertext minimum + 16-byte GCM tag = 29 bytes → Base64 = 40 chars. */
    private static final int MIN_ENCRYPTED_BASE64_LENGTH = 40;

    private final AesEncryptionService encryptionService;

    public EncryptedStringConverter(AesEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    /**
     * Decrypts the stored value. If the value is plain text (not yet migrated),
     * returns it as-is and logs a warning.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;

        if (!looksEncrypted(dbData)) {
            log.warn("PII column contains plain text — row not yet migrated. " +
                     "Run PiiEncryptionMigrator to encrypt existing data. " +
                     "Value length: {}", dbData.length());
            return dbData;
        }

        try {
            return encryptionService.decrypt(dbData);
        } catch (Exception e) {
            // Fallback: if decryption still fails (e.g. wrong key), return plain text
            // rather than crashing the entire login. Log at ERROR so it's visible.
            log.error("Decryption failed for stored value (length={}). " +
                      "Returning raw value. Check ENCRYPTION_KEY is correct. Error: {}",
                      dbData.length(), e.getMessage());
            return dbData;
        }
    }

    /**
     * Heuristic: a value looks encrypted if it is valid Base64 and long enough
     * to contain IV (12 bytes) + GCM tag (16 bytes) = at least 28 bytes decoded,
     * which is at least 40 Base64 chars. Plain-text names/phones are always shorter
     * and/or contain non-Base64 characters (spaces, +, etc. aside from Base64 set).
     */
    private boolean looksEncrypted(String value) {
        if (value.length() < MIN_ENCRYPTED_BASE64_LENGTH) return false;
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length >= 29; // 12 IV + 1 cipher + 16 GCM tag minimum
        } catch (IllegalArgumentException e) {
            return false; // not valid Base64 → definitely plain text
        }
    }
}
