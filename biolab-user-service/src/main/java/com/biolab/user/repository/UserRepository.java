package com.biolab.user.repository;

import com.biolab.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@code sec_schema.users} — profile CRUD + admin search.
 *
 * <h3>Sprint 2/3: Email encryption compatibility</h3>
 * <p>Mirrors the changes in {@code biolab-auth-service} UserRepository.
 * Email lookups work via the deterministic converter (hash comparison).
 * Admin keyword search now targets firstName/lastName only for partial
 * matching; full-email admin lookup uses the exact hash path.</p>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    default Optional<User> findByEmailIgnoreCase(String email) {
        return findByEmail(email);
    }

    boolean existsByEmail(String email);

    default boolean existsByEmailIgnoreCase(String email) {
        return existsByEmail(email);
    }

    /**
     * Admin search — firstName/lastName partial match only.
     * Full email lookup uses findByEmail (exact hash match).
     */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL " +
           " OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           " OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(@Param("search") String search,
                           @Param("isActive") Boolean isActive,
                           Pageable pageable);
}
