package com.biolab.user.repository;

import com.biolab.user.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

/** Repository for {@code sec_schema.role_permissions}. */
@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleId(UUID roleId);
    boolean existsByRoleIdAndPermissionId(UUID roleId, UUID permissionId);
    void deleteByRoleIdAndPermissionId(UUID roleId, UUID permissionId);

    @Query("SELECT rp.permission.name FROM RolePermission rp WHERE rp.role.id = :roleId")
    List<String> findPermissionNamesByRoleId(UUID roleId);
}
