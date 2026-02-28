package com.biolab.user.service.impl;

import com.biolab.user.dto.response.RoleResponse;
import com.biolab.user.dto.response.UserRoleResponse;
import com.biolab.user.entity.*;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.*;
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
@DisplayName("UserRoleQueryServiceImpl Unit Tests")
class UserRoleQueryServiceImplTest {

    @InjectMocks private UserRoleQueryServiceImpl service;
    @Mock private UserRoleRepository userRoleRepo;
    @Mock private RoleRepository roleRepo;
    @Mock private RolePermissionRepository rolePermRepo;

    private final UUID userId = UUID.randomUUID();
    private final UUID roleId = UUID.randomUUID();

    private Role makeRole(String name) {
        Role r = Role.builder().name(name).displayName(name + " Display").description("Desc").isSystemRole(true).build();
        r.setId(roleId); r.setCreatedAt(Instant.now());
        return r;
    }

    private UserRole makeUserRole(String roleName) {
        User u = User.builder().email("u@b.com").passwordHash("h").firstName("A").lastName("B").build();
        u.setId(userId);
        Role r = makeRole(roleName);
        UserRole ur = UserRole.builder().user(u).role(r).assignedBy(UUID.randomUUID()).build();
        ur.setId(UUID.randomUUID());
        ur.setAssignedAt(Instant.now());
        return ur;
    }

    @Nested @DisplayName("getRolesByUserId")
    class GetRolesByUserIdTests {
        @Test @DisplayName("[TC-USR-054] ✅ Should return user's roles with all fields")
        void getRoles_Success() {
            when(userRoleRepo.findByUserId(userId)).thenReturn(List.of(makeUserRole("SUPPLIER")));
            List<UserRoleResponse> result = service.getRolesByUserId(userId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUserId()).isEqualTo(userId);
            assertThat(result.get(0).getRoleName()).isEqualTo("SUPPLIER");
            assertThat(result.get(0).getRoleDisplayName()).isEqualTo("SUPPLIER Display");
        }

        @Test @DisplayName("[TC-USR-055] ✅ Should return empty list when user has no roles")
        void getRoles_Empty() {
            when(userRoleRepo.findByUserId(userId)).thenReturn(List.of());
            assertThat(service.getRolesByUserId(userId)).isEmpty();
        }
    }

    @Nested @DisplayName("getRoleNamesByUserId")
    class GetRoleNamesTests {
        @Test @DisplayName("[TC-USR-056] ✅ Should return role name strings")
        void getRoleNames_Success() {
            when(userRoleRepo.findRoleNamesByUserId(userId)).thenReturn(List.of("ADMIN", "SUPPLIER"));
            assertThat(service.getRoleNamesByUserId(userId)).containsExactly("ADMIN", "SUPPLIER");
        }

        @Test @DisplayName("[TC-USR-057] ✅ Should return empty for user with no roles")
        void getRoleNames_Empty() {
            when(userRoleRepo.findRoleNamesByUserId(userId)).thenReturn(List.of());
            assertThat(service.getRoleNamesByUserId(userId)).isEmpty();
        }
    }

    @Nested @DisplayName("getRoleById")
    class GetRoleByIdTests {
        @Test @DisplayName("[TC-USR-058] ✅ Should return role with permissions")
        void getRoleById_Success() {
            Role role = makeRole("ADMIN");
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(rolePermRepo.findPermissionNamesByRoleId(roleId)).thenReturn(List.of("USER_CREATE", "USER_VIEW"));

            RoleResponse result = service.getRoleById(roleId);
            assertThat(result.getName()).isEqualTo("ADMIN");
            assertThat(result.getPermissions()).containsExactly("USER_CREATE", "USER_VIEW");
            assertThat(result.getIsSystemRole()).isTrue();
        }

        @Test @DisplayName("[TC-USR-059] ❌ Should throw when role not found")
        void getRoleById_NotFound() {
            when(roleRepo.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getRoleById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-USR-060] ✅ Should return role with empty permissions")
        void getRoleById_NoPermissions() {
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(makeRole("BUYER")));
            when(rolePermRepo.findPermissionNamesByRoleId(roleId)).thenReturn(List.of());
            assertThat(service.getRoleById(roleId).getPermissions()).isEmpty();
        }
    }

    @Nested @DisplayName("getAllRoles")
    class GetAllRolesTests {
        @Test @DisplayName("[TC-USR-061] ✅ Should return all roles with permissions")
        void getAll_Success() {
            Role r = makeRole("ADMIN");
            when(roleRepo.findAll()).thenReturn(List.of(r));
            when(rolePermRepo.findPermissionNamesByRoleId(roleId)).thenReturn(List.of("USER_CREATE"));
            List<RoleResponse> result = service.getAllRoles();
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getPermissions()).containsExactly("USER_CREATE");
        }

        @Test @DisplayName("[TC-USR-062] ✅ Should return empty when no roles exist")
        void getAll_Empty() {
            when(roleRepo.findAll()).thenReturn(List.of());
            assertThat(service.getAllRoles()).isEmpty();
        }
    }

    @Nested @DisplayName("hasRole")
    class HasRoleTests {
        @Test @DisplayName("[TC-USR-063] ✅ Should return true when user has the role")
        void hasRole_True() {
            when(userRoleRepo.findRoleNamesByUserId(userId)).thenReturn(List.of("ADMIN", "SUPPLIER"));
            assertThat(service.hasRole(userId, "ADMIN")).isTrue();
        }

        @Test @DisplayName("[TC-USR-064] ✅ Should return false when user does not have the role")
        void hasRole_False() {
            when(userRoleRepo.findRoleNamesByUserId(userId)).thenReturn(List.of("BUYER"));
            assertThat(service.hasRole(userId, "SUPER_ADMIN")).isFalse();
        }

        @Test @DisplayName("[TC-USR-065] ✅ Should return false when user has no roles")
        void hasRole_NoRoles() {
            when(userRoleRepo.findRoleNamesByUserId(userId)).thenReturn(List.of());
            assertThat(service.hasRole(userId, "BUYER")).isFalse();
        }
    }
}
