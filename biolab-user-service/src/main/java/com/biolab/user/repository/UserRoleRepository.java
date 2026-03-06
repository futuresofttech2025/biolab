package com.biolab.user.repository;

import com.biolab.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.user_roles}. */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByRoleId(UUID roleId);
    Optional<UserRole> findByUser_IdAndRole_Id(UUID userId, UUID roleId);
    boolean existsByUser_IdAndRole_Id(UUID userId, UUID roleId);
    void deleteByUser_IdAndRole_Id(UUID userId, UUID roleId);

    @Query("SELECT ur.role.name FROM UserRole ur WHERE ur.user.id = :userId")
    List<String> findRoleNamesByUserId(UUID userId);
}