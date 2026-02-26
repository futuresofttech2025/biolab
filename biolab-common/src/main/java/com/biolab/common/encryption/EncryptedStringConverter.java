package com.biolab.common.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

/**
 * JPA {@link AttributeConverter} for transparent AES-256 column encryption.
 *
 * <p>Apply to PII fields in entities for HIPAA-compliant storage:</p>
 * <pre>
 *   &#64;Convert(converter = EncryptedStringConverter.class)
 *   &#64;Column(name = "phone")
 *   private String phone;
 * </pre>
 *
 * <p>Data is encrypted before writing to the database and decrypted
 * when reading. The database column stores Base64-encoded ciphertext.</p>
 *
 * @author BioLab Engineering Team
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesEncryptionService encryptionService;

    public EncryptedStringConverter(AesEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return encryptionService.decrypt(dbData);
    }
}
