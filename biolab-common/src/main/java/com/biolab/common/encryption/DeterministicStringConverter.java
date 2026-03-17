package com.biolab.common.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} for deterministic HMAC-SHA256 column hashing.
 *
 * <h3>Sprint 2 — GAP-12: Email encrypted for lookups</h3>
 * <p>Apply to the {@code email} column on {@code User} entities to enable
 * encrypted equality-based lookups (e.g. {@code findByEmailIgnoreCase})
 * without storing plaintext email addresses in the database.</p>
 *
 * <p>The output is a versioned HMAC-SHA256 digest:
 * {@code dv1:Base64(HMAC-SHA256(lower(email)))}.
 * Equal email addresses always produce equal stored values, so the database
 * index and unique constraint continue to work correctly.</p>
 *
 * <h3>Usage on entity</h3>
 * <pre>
 *   &#64;Convert(converter = DeterministicStringConverter.class)
 *   &#64;Column(name = "email", nullable = false, unique = true, length = 100)
 *   private String email;
 * </pre>
 *
 * <h3>Security trade-off</h3>
 * <p>The deterministic output leaks equality — an attacker with DB access
 * can tell if two users share an email, but cannot recover the plaintext.
 * A separate display copy or the decryption path is needed to show email
 * values to the user; for emails this is acceptable since the email is
 * already known to the authenticated user.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Converter
@Component
public class DeterministicStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptionService encryptionService;

    public DeterministicStringConverter(AesEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    /** Converts email to deterministic HMAC-SHA256 hash before storing. */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encryptDeterministic(attribute);
    }

    /**
     * Email columns cannot be decrypted (HMAC is one-way).
     * The value returned here is the raw stored hash — used only for lookups.
     * To display the user's email, keep it separately in the session/JWT claim.
     *
     * <p>This method returns the stored hash as-is. Application code that needs
     * to show the user their email should read it from the JWT {@code email}
     * claim or a separate plaintext display column, NOT from this entity field.</p>
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        // Return the hash value — callers must not display this to users.
        // JWT email claim is the source of truth for display.
        return dbData;
    }
}