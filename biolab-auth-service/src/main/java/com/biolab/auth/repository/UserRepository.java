package com.biolab.auth.repository;

import com.biolab.auth.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.users} — CRUD + custom queries. */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);

    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%',:search,'%')) " +
           "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%',:search,'%'))) " +
           "AND (:isActive IS NULL OR u.isActive = :isActive)")
    Page<User> searchUsers(String search, Boolean isActive, Pageable pageable);
}
