package com.biolab.auth.migration;

import com.biolab.auth.entity.User;
import com.biolab.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * One-time PII encryption migrator.
 *
 * <h3>What it does</h3>
 * <p>Runs on startup when the {@code migrate-pii} Spring profile is active.
 * Scans all User rows and re-saves any whose {@code firstName}, {@code lastName},
 * or {@code phone} columns still contain plain text. The JPA
 * {@link com.biolab.common.encryption.EncryptedStringConverter} transparently
 * encrypts the values on {@code save()}, so no manual encryption is needed here.</p>
 *
 * <h3>How to run</h3>
 * <pre>
 * # One-time — add the profile and restart the service, then remove it
 * SPRING_PROFILES_ACTIVE=migrate-pii java -jar biolab-auth-service.jar
 * </pre>
 *
 * <p>After migration completes, remove the {@code migrate-pii} profile and
 * optionally remove the plain-text passthrough guard in
 * {@link com.biolab.common.encryption.EncryptedStringConverter}.</p>
 *
 * @author BioLab Engineering Team
 */
@Component
@Profile("migrate-pii")
public class PiiEncryptionMigrator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PiiEncryptionMigrator.class);

    /** Minimum decoded byte length to consider a value already encrypted.
     *  12 (IV) + 1 (min cipher) + 16 (GCM tag) = 29. */
    private static final int MIN_ENCRYPTED_BYTES = 29;

    private final UserRepository userRepository;

    public PiiEncryptionMigrator(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("=== PII Encryption Migration starting ===");

        List<User> users = userRepository.findAll();
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger skipped  = new AtomicInteger(0);

        for (User user : users) {
            boolean needsMigration =
                    needsEncryption(user.getFirstName()) ||
                    needsEncryption(user.getLastName())  ||
                    needsEncryption(user.getPhone());

            if (needsMigration) {
                // EncryptedStringConverter.convertToDatabaseColumn() encrypts on save
                userRepository.save(user);
                migrated.incrementAndGet();
                log.info("Migrated user id={} email={}", user.getId(), maskEmail(user.getEmail()));
            } else {
                skipped.incrementAndGet();
            }
        }

        log.info("=== PII Encryption Migration complete: {} migrated, {} already encrypted ===",
                 migrated.get(), skipped.get());
    }

    /**
     * Returns true if the value is plain text (not yet encrypted).
     * Mirrors the heuristic in EncryptedStringConverter.
     */
    private boolean needsEncryption(String value) {
        if (value == null) return false;
        if (value.length() < 40) return true; // too short to be encrypted
        try {
            byte[] decoded = Base64.getDecoder().decode(value);
            return decoded.length < MIN_ENCRYPTED_BYTES;
        } catch (IllegalArgumentException e) {
            return true; // not Base64 → plain text
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return (at > 1 ? email.charAt(0) + "***" : "***") + email.substring(at);
    }
}
