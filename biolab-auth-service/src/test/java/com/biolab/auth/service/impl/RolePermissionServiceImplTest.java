package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.RolePermissionAssignRequest;
import com.biolab.auth.dto.response.RolePermissionResponse;
import com.biolab.auth.entity.*;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RolePermissionServiceImpl Unit Tests")
class RolePermissionServiceImplTest {

    @InjectMocks private RolePermissionServiceImpl service;
    @Mock private RolePermissionRepository repo;
    @Mock private RoleRepository roleRepo;
    @Mock private PermissionRepository permRepo;

    private Role role;
    private Permission perm;
    private final UUID roleId = UUID.randomUUID();
    private final UUID permId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        role = Role.builder().name("SUPPLIER").displayName("Supplier").isSystemRole(true).build();
        role.setId(roleId); role.setCreatedAt(Instant.now());

        perm = Permission.builder().name("SERVICE_CREATE").module("SERVICE").action("CREATE").description("Create service").build();
        perm.setId(permId); perm.setCreatedAt(Instant.now());
    }

    @Nested @DisplayName("assignPermissions")
    class AssignTests {
        @Test @DisplayName("[TC-AUTH-100] ✅ Should assign new permissions to role")
        void assign_Success() {
            RolePermissionAssignRequest req = new RolePermissionAssignRequest();
            req.setPermissionIds(List.of(permId));

            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByRoleIdAndPermissionId(roleId, permId)).thenReturn(false);
            when(permRepo.findById(permId)).thenReturn(Optional.of(perm));
            when(repo.save(any())).thenReturn(RolePermission.builder().role(role).permission(perm).build());

            RolePermission rp = RolePermission.builder().role(role).permission(perm).build();
            when(repo.findByRoleId(roleId)).thenReturn(List.of(rp));

            RolePermissionResponse result = service.assignPermissions(roleId, req);
            assertThat(result.getRoleName()).isEqualTo("SUPPLIER");
            assertThat(result.getPermissions()).hasSize(1);
            assertThat(result.getPermissions().get(0).getName()).isEqualTo("SERVICE_CREATE");
        }

        @Test @DisplayName("[TC-AUTH-101] ✅ Should skip already-assigned permissions (idempotent)")
        void assign_SkipDuplicate() {
            RolePermissionAssignRequest req = new RolePermissionAssignRequest();
            req.setPermissionIds(List.of(permId));

            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByRoleIdAndPermissionId(roleId, permId)).thenReturn(true);
            when(repo.findByRoleId(roleId)).thenReturn(List.of());

            service.assignPermissions(roleId, req);
            verify(repo, never()).save(any());
        }

        @Test @DisplayName("[TC-AUTH-102] ❌ Should throw when role not found")
        void assign_RoleNotFound() {
            RolePermissionAssignRequest req = new RolePermissionAssignRequest();
            req.setPermissionIds(List.of(permId));
            when(roleRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignPermissions(UUID.randomUUID(), req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-AUTH-103] ❌ Should throw when permission not found")
        void assign_PermNotFound() {
            RolePermissionAssignRequest req = new RolePermissionAssignRequest();
            req.setPermissionIds(List.of(UUID.randomUUID()));

            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByRoleIdAndPermissionId(eq(roleId), any())).thenReturn(false);
            when(permRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assignPermissions(roleId, req))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-AUTH-104] ✅ Should assign multiple permissions at once")
        void assign_Multiple() {
            UUID permId2 = UUID.randomUUID();
            Permission perm2 = Permission.builder().name("SERVICE_EDIT").module("SERVICE").action("EDIT").build();
            perm2.setId(permId2); perm2.setCreatedAt(Instant.now());

            RolePermissionAssignRequest req = new RolePermissionAssignRequest();
            req.setPermissionIds(List.of(permId, permId2));

            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByRoleIdAndPermissionId(roleId, permId)).thenReturn(false);
            when(repo.existsByRoleIdAndPermissionId(roleId, permId2)).thenReturn(false);
            when(permRepo.findById(permId)).thenReturn(Optional.of(perm));
            when(permRepo.findById(permId2)).thenReturn(Optional.of(perm2));
            when(repo.save(any())).thenReturn(RolePermission.builder().role(role).permission(perm).build());
            when(repo.findByRoleId(roleId)).thenReturn(List.of());

            service.assignPermissions(roleId, req);
            verify(repo, times(2)).save(any());
        }
    }

    @Nested @DisplayName("getByRoleId")
    class GetByRoleIdTests {
        @Test @DisplayName("[TC-AUTH-105] ✅ Should return role with its permissions")
        void getByRoleId_Success() {
            RolePermission rp = RolePermission.builder().role(role).permission(perm).build();
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.findByRoleId(roleId)).thenReturn(List.of(rp));

            RolePermissionResponse result = service.getByRoleId(roleId);
            assertThat(result.getRoleId()).isEqualTo(roleId);
            assertThat(result.getRoleName()).isEqualTo("SUPPLIER");
            assertThat(result.getPermissions()).hasSize(1);
        }

        @Test @DisplayName("[TC-AUTH-106] ❌ Should throw when role not found")
        void getByRoleId_NotFound() {
            when(roleRepo.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getByRoleId(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-AUTH-107] ✅ Should return role with empty permissions list")
        void getByRoleId_NoPermissions() {
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.findByRoleId(roleId)).thenReturn(List.of());
            assertThat(service.getByRoleId(roleId).getPermissions()).isEmpty();
        }
    }

    @Nested @DisplayName("revokePermission")
    class RevokeTests {
        @Test @DisplayName("[TC-AUTH-108] ✅ Should revoke permission from role")
        void revoke_Success() {
            when(repo.existsByRoleIdAndPermissionId(roleId, permId)).thenReturn(true);
            service.revokePermission(roleId, permId);
            verify(repo).deleteByRoleIdAndPermissionId(roleId, permId);
        }

        @Test @DisplayName("[TC-AUTH-109] ❌ Should throw when role-permission mapping not found")
        void revoke_NotFound() {
            when(repo.existsByRoleIdAndPermissionId(any(), any())).thenReturn(false);
            assertThatThrownBy(() -> service.revokePermission(UUID.randomUUID(), UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
