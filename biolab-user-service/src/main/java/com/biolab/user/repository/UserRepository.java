package com.biolab.user.repository;

import com.biolab.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@code sec_schema.users} — profile CRUD + admin search.
 *
 * @author BioLab Engineering Team
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    /** Admin search — filter by name/email keyword and active status. */
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(String search, Boolean isActive, Pageable pageable);
}
