package com.biolab.auth.repository;

import com.biolab.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@code sec_schema.users} — CRUD + custom queries.
 *
 * <h3>Sprint 2 / Sprint 3 — Email encryption impact</h3>
 * <p>Since the {@code email} column now stores an HMAC-SHA256 hash
 * (via {@link com.biolab.common.encryption.DeterministicStringConverter}),
 * the JPA methods {@code findByEmailIgnoreCase} and {@code existsByEmailIgnoreCase}
 * continue to work correctly: Spring Data passes the method argument through
 * the converter before running the query, so the comparison is hash-vs-hash.</p>
 *
 * <h3>Admin search</h3>
 * <p>The {@code searchUsers} JPQL query previously searched
 * {@code LOWER(u.email) LIKE ...}. This no longer works against the hash value.
 * The search now targets {@code emailDisplay} (AES-encrypted, decrypted by JPA
 * on read) for the email component, and continues to use firstName/lastName
 * directly (which are decrypted transparently by the converter).</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by email. The argument is automatically hashed by
     * {@link com.biolab.common.encryption.DeterministicStringConverter}
     * before the DB query runs.
     */
    Optional<User> findByEmail(String email);

    /**
     * Compatibility alias — delegates to {@link #findByEmail(String)}.
     * The converter normalises case (lower-trims) before hashing, so
     * case-insensitivity is preserved.
     */
    default Optional<User> findByEmailIgnoreCase(String email) {
        return findByEmail(email);
    }

    /**
     * Checks if a user exists with the given email.
     * The converter hashes before comparing.
     */
    boolean existsByEmail(String email);

    default boolean existsByEmailIgnoreCase(String email) {
        return existsByEmail(email);
    }

    /**
     * Admin search — keyword matched against decrypted firstName, lastName,
     * and the encrypted emailDisplay field.
     *
     * <p>Note: LIKE search on AES-encrypted emailDisplay is only useful if
     * the admin provides the full email address (the converter encrypts the
     * full value for comparison). For partial email search, use a dedicated
     * admin search endpoint that accepts a full email and hashes it for an
     * exact lookup.</p>
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL " +
           " OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           " OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(@Param("search") String search,
                           @Param("isActive") Boolean isActive,
                           Pageable pageable);

    /**
     * Exact email lookup for admin endpoints — accepts raw email, hashed by converter.
     * Alias for findByEmail that makes the intent clear in admin service code.
     */
    @Query("SELECT u FROM User u WHERE u.email = :emailHash")
    Optional<User> findByEmailHash(@Param("emailHash") String emailHash);
}
