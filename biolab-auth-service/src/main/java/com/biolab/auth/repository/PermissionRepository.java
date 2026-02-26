package com.biolab.auth.repository;

import com.biolab.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repository for {@code sec_schema.permissions} — CRUD + module queries. */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByName(String name);
    List<Permission> findByModule(String module);
    boolean existsByName(String name);
}
