package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.*;
import com.biolab.auth.dto.response.*;
import com.biolab.auth.entity.*;
import com.biolab.auth.entity.enums.*;
import com.biolab.auth.exception.*;
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

/**
 * Comprehensive unit tests for auth-service RBAC, Consent, and Session services.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Service — RBAC, Consent & Session Tests")
class AuthServiceRbacConsentSessionTest {

    // ══════════════════════════════════════════════════════════════════
    // ROLE SERVICE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("RoleServiceImpl")
    class RoleServiceTests {
        @InjectMocks private RoleServiceImpl service;
        @Mock private RoleRepository repo;

        private Role sampleRole;
        private final UUID roleId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            sampleRole = Role.builder().name("DATA_ANALYST").displayName("Data Analyst")
                    .description("Custom analytics role").isSystemRole(false).build();
            sampleRole.setId(roleId);
            sampleRole.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("[TC-AUTH-029] ✅ Create role successfully")
        void create_Success() {
            RoleCreateRequest req = new RoleCreateRequest();
            req.setName("data_analyst"); req.setDisplayName("Data Analyst"); req.setDescription("Desc");
            when(repo.existsByName("DATA_ANALYST")).thenReturn(false);
            when(repo.save(any())).thenReturn(sampleRole);

            RoleResponse resp = service.create(req);
            assertThat(resp.getName()).isEqualTo("DATA_ANALYST");
        }

        @Test
        @DisplayName("[TC-AUTH-030] ❌ Create role with duplicate name")
        void create_Duplicate() {
            RoleCreateRequest req = new RoleCreateRequest();
            req.setName("ADMIN"); req.setDisplayName("Admin");
            when(repo.existsByName("ADMIN")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-031] ✅ Get all roles returns list")
        void getAll_Success() {
            when(repo.findAll()).thenReturn(List.of(sampleRole));
            assertThat(service.getAll()).hasSize(1);
        }

        @Test
        @DisplayName("[TC-AUTH-032] ✅ Get role by ID")
        void getById_Success() {
            when(repo.findById(roleId)).thenReturn(Optional.of(sampleRole));
            assertThat(service.getById(roleId).getName()).isEqualTo("DATA_ANALYST");
        }

        @Test
        @DisplayName("[TC-AUTH-033] ❌ Get role by non-existent ID")
        void getById_NotFound() {
            when(repo.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getById(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-034] ✅ Get role by name")
        void getByName_Success() {
            when(repo.findByName("DATA_ANALYST")).thenReturn(Optional.of(sampleRole));
            assertThat(service.getByName("DATA_ANALYST").getId()).isEqualTo(roleId);
        }

        @Test
        @DisplayName("[TC-AUTH-035] ❌ Get role by non-existent name")
        void getByName_NotFound() {
            when(repo.findByName("GHOST")).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.getByName("GHOST")).isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-036] ✅ Update role partial fields")
        void update_Success() {
            RoleUpdateRequest req = new RoleUpdateRequest();
            req.setDescription("Updated desc");
            when(repo.findById(roleId)).thenReturn(Optional.of(sampleRole));
            when(repo.save(any())).thenReturn(sampleRole);

            RoleResponse resp = service.update(roleId, req);
            assertThat(resp).isNotNull();
            verify(repo).save(any());
        }

        @Test
        @DisplayName("[TC-AUTH-037] ❌ Update non-existent role")
        void update_NotFound() {
            when(repo.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.update(UUID.randomUUID(), new RoleUpdateRequest()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-038] ✅ Delete role")
        void delete_Success() {
            when(repo.existsById(roleId)).thenReturn(true);
            service.delete(roleId);
            verify(repo).deleteById(roleId);
        }

        @Test
        @DisplayName("[TC-AUTH-039] ❌ Delete non-existent role")
        void delete_NotFound() {
            when(repo.existsById(any())).thenReturn(false);
            assertThatThrownBy(() -> service.delete(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // PERMISSION SERVICE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("PermissionServiceImpl")
    class PermissionServiceTests {
        @InjectMocks private PermissionServiceImpl service;
        @Mock private PermissionRepository repo;

        private Permission perm;
        private final UUID permId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            perm = Permission.builder().name("REPORT_EXPORT").module("REPORT").action("EXPORT")
                    .description("Export reports").build();
            perm.setId(permId); perm.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("[TC-AUTH-040] ✅ Create permission successfully")
        void create_Success() {
            PermissionCreateRequest req = new PermissionCreateRequest();
            req.setName("REPORT_EXPORT"); req.setModule("REPORT"); req.setAction("EXPORT");
            when(repo.existsByName("REPORT_EXPORT")).thenReturn(false);
            when(repo.save(any())).thenReturn(perm);

            assertThat(service.create(req).getName()).isEqualTo("REPORT_EXPORT");
        }

        @Test
        @DisplayName("[TC-AUTH-041] ❌ Create permission with duplicate name")
        void create_Duplicate() {
            PermissionCreateRequest req = new PermissionCreateRequest();
            req.setName("USER_CREATE"); req.setModule("USER"); req.setAction("CREATE");
            when(repo.existsByName("USER_CREATE")).thenReturn(true);

            assertThatThrownBy(() -> service.create(req)).isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-042] ✅ Get permissions by module")
        void getByModule_Success() {
            when(repo.findByModule("REPORT")).thenReturn(List.of(perm));
            assertThat(service.getByModule("REPORT")).hasSize(1);
        }

        @Test
        @DisplayName("[TC-AUTH-043] ✅ Get permissions by module returns empty when none exist")
        void getByModule_Empty() {
            when(repo.findByModule("NONEXISTENT")).thenReturn(List.of());
            assertThat(service.getByModule("NONEXISTENT")).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUTH-044] ❌ Delete non-existent permission")
        void delete_NotFound() {
            when(repo.existsById(any())).thenReturn(false);
            assertThatThrownBy(() -> service.delete(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // USER ROLE SERVICE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("UserRoleServiceImpl")
    class UserRoleServiceTests {
        @InjectMocks private UserRoleServiceImpl service;
        @Mock private UserRoleRepository repo;
        @Mock private UserRepository userRepo;
        @Mock private RoleRepository roleRepo;

        private User user;
        private Role role;
        private final UUID userId = UUID.randomUUID();
        private final UUID roleId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            user = User.builder().email("u@b.com").passwordHash("h").firstName("A").lastName("B").build();
            user.setId(userId);
            role = Role.builder().name("SUPPLIER").displayName("Supplier").build();
            role.setId(roleId); role.setCreatedAt(Instant.now());
        }

        @Test
        @DisplayName("[TC-AUTH-045] ✅ Assign role to user")
        void assign_Success() {
            UserRoleAssignRequest req = new UserRoleAssignRequest();
            req.setRoleId(roleId);
            UUID adminId = UUID.randomUUID();

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByUserIdAndRoleId(userId, roleId)).thenReturn(false);

            UserRole saved = UserRole.builder().user(user).role(role).assignedBy(adminId).build();
            saved.setId(UUID.randomUUID());
            when(repo.save(any())).thenReturn(saved);

            UserRoleResponse resp = service.assign(userId, req, adminId);
            assertThat(resp.getUserId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("[TC-AUTH-046] ❌ Assign duplicate role")
        void assign_Duplicate() {
            UserRoleAssignRequest req = new UserRoleAssignRequest();
            req.setRoleId(roleId);

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepo.findById(roleId)).thenReturn(Optional.of(role));
            when(repo.existsByUserIdAndRoleId(userId, roleId)).thenReturn(true);

            assertThatThrownBy(() -> service.assign(userId, req, UUID.randomUUID()))
                    .isInstanceOf(DuplicateResourceException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-047] ❌ Assign role to non-existent user")
        void assign_UserNotFound() {
            UserRoleAssignRequest req = new UserRoleAssignRequest();
            req.setRoleId(roleId);
            when(userRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assign(UUID.randomUUID(), req, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-048] ❌ Assign non-existent role to user")
        void assign_RoleNotFound() {
            UserRoleAssignRequest req = new UserRoleAssignRequest();
            req.setRoleId(UUID.randomUUID());
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(roleRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.assign(userId, req, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-049] ✅ Revoke role from user")
        void revoke_Success() {
            when(repo.existsByUserIdAndRoleId(userId, roleId)).thenReturn(true);
            service.revoke(userId, roleId);
            verify(repo).deleteByUserIdAndRoleId(userId, roleId);
        }

        @Test
        @DisplayName("[TC-AUTH-050] ❌ Revoke non-existent role assignment")
        void revoke_NotFound() {
            when(repo.existsByUserIdAndRoleId(any(), any())).thenReturn(false);
            assertThatThrownBy(() -> service.revoke(userId, roleId)).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // CONSENT SERVICE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ConsentServiceImpl")
    class ConsentServiceTests {
        @InjectMocks private ConsentServiceImpl service;
        @Mock private ConsentRecordRepository repo;
        @Mock private UserRepository userRepo;

        private User user;
        private final UUID userId = UUID.randomUUID();

        @BeforeEach
        void setUp() {
            user = User.builder().email("u@b.com").passwordHash("h").firstName("A").lastName("B").build();
            user.setId(userId);
        }

        @Test
        @DisplayName("[TC-AUTH-051] ✅ Grant GDPR consent")
        void grant_Success() {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("GDPR"); req.setVersion("2.0");

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(repo.findByUserIdAndConsentType(userId, ConsentType.GDPR)).thenReturn(Optional.empty());

            ConsentRecord cr = ConsentRecord.builder().user(user).consentType(ConsentType.GDPR)
                    .ipAddress("127.0.0.1").version("2.0").build();
            cr.setId(UUID.randomUUID());
            when(repo.save(any())).thenReturn(cr);

            ConsentRecordResponse resp = service.grant(userId, req, "127.0.0.1");
            assertThat(resp.getConsentType()).isEqualTo("GDPR");
            assertThat(resp.getVersion()).isEqualTo("2.0");
        }

        @Test
        @DisplayName("[TC-AUTH-052] ✅ Re-grant previously revoked consent")
        void grant_ReGrant() {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("HIPAA");

            ConsentRecord existing = ConsentRecord.builder().user(user).consentType(ConsentType.HIPAA)
                    .ipAddress("old-ip").version("1.0").revokedAt(Instant.now()).build();
            existing.setId(UUID.randomUUID());

            when(userRepo.findById(userId)).thenReturn(Optional.of(user));
            when(repo.findByUserIdAndConsentType(userId, ConsentType.HIPAA)).thenReturn(Optional.of(existing));
            when(repo.save(any())).thenReturn(existing);

            ConsentRecordResponse resp = service.grant(userId, req, "new-ip");
            assertThat(resp).isNotNull();
        }

        @Test
        @DisplayName("[TC-AUTH-053] ❌ Grant consent for non-existent user")
        void grant_UserNotFound() {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("GDPR");
            when(userRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.grant(UUID.randomUUID(), req, "127.0.0.1"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-054] ❌ Grant consent with invalid type")
        void grant_InvalidType() {
            ConsentRequest req = new ConsentRequest();
            req.setConsentType("INVALID");
            when(userRepo.findById(userId)).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> service.grant(userId, req, "127.0.0.1"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-055] ✅ Revoke consent")
        void revoke_Success() {
            ConsentRecord cr = ConsentRecord.builder().user(user).consentType(ConsentType.MARKETING)
                    .ipAddress("ip").version("1.0").build();
            cr.setId(UUID.randomUUID());
            when(repo.findByUserIdAndConsentType(userId, ConsentType.MARKETING)).thenReturn(Optional.of(cr));
            when(repo.save(any())).thenReturn(cr);

            ConsentRecordResponse resp = service.revoke(userId, "MARKETING");
            assertThat(resp).isNotNull();
            verify(repo).save(any());
        }

        @Test
        @DisplayName("[TC-AUTH-056] ❌ Revoke non-existent consent")
        void revoke_NotFound() {
            when(repo.findByUserIdAndConsentType(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.revoke(userId, "TOS"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-057] ✅ Get all consent records for user")
        void getByUserId_Success() {
            ConsentRecord cr = ConsentRecord.builder().user(user).consentType(ConsentType.GDPR)
                    .ipAddress("ip").version("1.0").build();
            cr.setId(UUID.randomUUID());
            when(repo.findByUserId(userId)).thenReturn(List.of(cr));

            List<ConsentRecordResponse> result = service.getByUserId(userId);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[TC-AUTH-058] ✅ Get consent records returns empty for user with no consents")
        void getByUserId_Empty() {
            when(repo.findByUserId(userId)).thenReturn(List.of());
            assertThat(service.getByUserId(userId)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SESSION SERVICE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("SessionServiceImpl")
    class SessionServiceTests {
        @InjectMocks private SessionServiceImpl service;
        @Mock private UserSessionRepository sessionRepo;
        @Mock private RefreshTokenRepository refreshTokenRepo;

        private final UUID userId = UUID.randomUUID();
        private final UUID sessionId = UUID.randomUUID();

        @Test
        @DisplayName("[TC-AUTH-059] ✅ Get active sessions for user")
        void getActiveSessions_Success() {
            UserSession session = UserSession.builder()
                    .ipAddress("10.0.0.1").userAgent("Chrome").isActive(true)
                    .expiresAt(Instant.now().plusSeconds(3600)).lastAccessedAt(Instant.now()).build();
            session.setId(sessionId); session.setCreatedAt(Instant.now());

            when(sessionRepo.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of(session));

            List<UserSessionResponse> result = service.getActiveSessions(userId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getIpAddress()).isEqualTo("10.0.0.1");
        }

        @Test
        @DisplayName("[TC-AUTH-060] ✅ Get active sessions returns empty when no sessions")
        void getActiveSessions_Empty() {
            when(sessionRepo.findByUserIdAndIsActiveTrue(userId)).thenReturn(List.of());
            assertThat(service.getActiveSessions(userId)).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUTH-061] ✅ Terminate specific session")
        void terminateSession_Success() {
            UserSession session = UserSession.builder().isActive(true).build();
            session.setId(sessionId);
            when(sessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

            service.terminateSession(sessionId);

            ArgumentCaptor<UserSession> captor = ArgumentCaptor.forClass(UserSession.class);
            verify(sessionRepo).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("[TC-AUTH-062] ❌ Terminate non-existent session")
        void terminateSession_NotFound() {
            when(sessionRepo.findById(any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.terminateSession(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-063] ✅ Force logout revokes tokens and sessions")
        void forceLogout_Success() {
            when(refreshTokenRepo.revokeAllByUserIdWithReason(eq(userId), any(RevokedReason.class), any(Instant.class))).thenReturn(3);
            when(sessionRepo.deactivateAllUserSessions(userId)).thenReturn(2);

            int total = service.forceLogoutUser(userId);
            assertThat(total).isEqualTo(5);
        }

        @Test
        @DisplayName("[TC-AUTH-064] ✅ Get session stats returns counts")
        void getSessionStats_Success() {
            when(sessionRepo.countByIsActiveTrue()).thenReturn(42L);
            when(refreshTokenRepo.countActiveTokens(any())).thenReturn(100L);
            when(sessionRepo.countDistinctUsersByIsActiveTrue()).thenReturn(25L);
            when(sessionRepo.countByCreatedAtAfter(any())).thenReturn(10L);

            Map<String, Object> stats = service.getSessionStats();
            assertThat(stats).containsEntry("activeSessions", 42L);
            assertThat(stats).containsEntry("activeRefreshTokens", 100L);
            assertThat(stats).containsEntry("uniqueUsersWithSessions", 25L);
        }

        @Test
        @DisplayName("[TC-AUTH-065] ✅ Terminate all sessions for user")
        void terminateAllSessions_Success() {
            service.terminateAllSessions(userId);
            verify(sessionRepo).deactivateAllUserSessions(userId);
        }
    }
}
