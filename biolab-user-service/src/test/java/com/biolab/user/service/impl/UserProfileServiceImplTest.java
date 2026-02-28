package com.biolab.user.service.impl;

import com.biolab.user.dto.request.UserUpdateRequest;
import com.biolab.user.dto.response.*;
import com.biolab.user.entity.*;
import com.biolab.user.entity.enums.OrganizationType;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl Unit Tests")
class UserProfileServiceImplTest {

    @InjectMocks private UserProfileServiceImpl service;
    @Mock private UserRepository userRepository;
    @Mock private UserRoleRepository userRoleRepository;
    @Mock private UserOrganizationRepository userOrgRepository;

    private User testUser;
    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .email("jane@biolab.com").firstName("Jane").lastName("Doe")
            .phone("+1234567890").isActive(true).isEmailVerified(true).isLocked(false)
            .build();
        testUser.setId(userId);
        testUser.setCreatedAt(Instant.now());
    }

    private Organization makeOrg(UUID id, String name, OrganizationType type) {
        Organization org = Organization.builder().name(name).type(type).isActive(true).build();
        org.setId(id);
        return org;
    }

    private UserOrganization makeUserOrg(Organization org, String roleInOrg) {
        UserOrganization uo = UserOrganization.builder()
            .userId(userId).organization(org).roleInOrg(roleInOrg).isPrimary(true)
            .build();
        uo.setId(UUID.randomUUID());
        return uo;
    }

    // ══════════════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("[TC-USR-034] ✅ Should return full profile with roles and orgs")
        void getById_Success() {
            Organization org = makeOrg(orgId, "BioLab Alpha", OrganizationType.SUPPLIER);
            UserOrganization uo = makeUserOrg(org, "MANAGER");

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("SUPPLIER"));
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of(uo));

            UserProfileResponse result = service.getById(userId);

            assertThat(result.getId()).isEqualTo(userId);
            assertThat(result.getEmail()).isEqualTo("jane@biolab.com");
            assertThat(result.getFirstName()).isEqualTo("Jane");
            assertThat(result.getRoles()).containsExactly("SUPPLIER");
            assertThat(result.getOrganizations()).hasSize(1);
            assertThat(result.getOrganizations().get(0).getOrganizationName()).isEqualTo("BioLab Alpha");
            assertThat(result.getOrganizations().get(0).getRoleInOrg()).isEqualTo("MANAGER");
        }

        @Test
        @DisplayName("[TC-USR-035] ❌ Should throw ResourceNotFoundException for unknown ID")
        void getById_NotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-036] ✅ Should return profile with empty roles and orgs")
        void getById_NoRolesNoOrgs() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.getById(userId);

            assertThat(result.getRoles()).isEmpty();
            assertThat(result.getOrganizations()).isEmpty();
        }

        @Test
        @DisplayName("[TC-USR-037] ✅ Should return multiple roles")
        void getById_MultipleRoles() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("ADMIN", "SUPPLIER"));
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.getById(userId);

            assertThat(result.getRoles()).containsExactly("ADMIN", "SUPPLIER");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GET BY EMAIL
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getByEmail")
    class GetByEmailTests {

        @Test
        @DisplayName("[TC-USR-038] ✅ Should return profile by email")
        void getByEmail_Success() {
            when(userRepository.findByEmailIgnoreCase("jane@biolab.com")).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("BUYER"));
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.getByEmail("jane@biolab.com");

            assertThat(result.getEmail()).isEqualTo("jane@biolab.com");
        }

        @Test
        @DisplayName("[TC-USR-039] ❌ Should throw when email not found")
        void getByEmail_NotFound() {
            when(userRepository.findByEmailIgnoreCase("nobody@biolab.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByEmail("nobody@biolab.com"))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-040] ✅ Should be case-insensitive")
        void getByEmail_CaseInsensitive() {
            when(userRepository.findByEmailIgnoreCase("JANE@BIOLAB.COM")).thenReturn(Optional.of(testUser));
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.getByEmail("JANE@BIOLAB.COM");

            assertThat(result.getId()).isEqualTo(userId);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("[TC-USR-041] ✅ Should return paginated search results")
        void search_Success() {
            Page<User> page = new PageImpl<>(List.of(testUser), PageRequest.of(0, 10), 1);
            when(userRepository.searchUsers("jane", true, PageRequest.of(0, 10))).thenReturn(page);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("BUYER"));
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            PageResponse<UserProfileResponse> result = service.search("jane", true, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo("jane@biolab.com");
        }

        @Test
        @DisplayName("[TC-USR-042] ✅ Should return empty results when no matches")
        void search_NoMatch() {
            when(userRepository.searchUsers("zzz", null, PageRequest.of(0, 10))).thenReturn(Page.empty());

            PageResponse<UserProfileResponse> result = service.search("zzz", null, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC-USR-043] ✅ Should filter by isActive")
        void search_FilterByActive() {
            when(userRepository.searchUsers(null, false, PageRequest.of(0, 10))).thenReturn(Page.empty());

            PageResponse<UserProfileResponse> result = service.search(null, false, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
            verify(userRepository).searchUsers(null, false, PageRequest.of(0, 10));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("[TC-USR-044] ✅ Should update all provided fields")
        void update_AllFields() {
            UserUpdateRequest req = UserUpdateRequest.builder()
                .firstName("Updated").lastName("Name").phone("+9999").avatarUrl("https://new-avatar.png")
                .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.update(userId, req);

            assertThat(result).isNotNull();
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("Updated");
            assertThat(captor.getValue().getLastName()).isEqualTo("Name");
            assertThat(captor.getValue().getPhone()).isEqualTo("+9999");
            assertThat(captor.getValue().getAvatarUrl()).isEqualTo("https://new-avatar.png");
        }

        @Test
        @DisplayName("[TC-USR-045] ✅ Should only update non-null fields (partial update)")
        void update_PartialFields() {
            UserUpdateRequest req = UserUpdateRequest.builder().firstName("NewFirst").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            service.update(userId, req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("NewFirst");
            assertThat(captor.getValue().getLastName()).isEqualTo("Doe"); // Unchanged
            assertThat(captor.getValue().getPhone()).isEqualTo("+1234567890"); // Unchanged
        }

        @Test
        @DisplayName("[TC-USR-046] ❌ Should throw when user not found")
        void update_NotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(UUID.randomUUID(), new UserUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-047] ✅ Should trim firstName and lastName")
        void update_TrimsWhitespace() {
            UserUpdateRequest req = UserUpdateRequest.builder()
                .firstName("  Trimmed  ").lastName("  Name  ").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            service.update(userId, req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getFirstName()).isEqualTo("Trimmed");
            assertThat(captor.getValue().getLastName()).isEqualTo("Name");
        }

        @Test
        @DisplayName("[TC-USR-048] ✅ Should update updatedAt timestamp")
        void update_SetsUpdatedAt() {
            UserUpdateRequest req = UserUpdateRequest.builder().firstName("X").build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            service.update(userId, req);

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DEACTIVATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deactivate")
    class DeactivateTests {

        @Test
        @DisplayName("[TC-USR-049] ✅ Should set isActive to false")
        void deactivate_Success() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenReturn(testUser);

            service.deactivate(userId);

            assertThat(captor.getValue().getIsActive()).isFalse();
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("[TC-USR-050] ❌ Should throw when user not found")
        void deactivate_NotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // REACTIVATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reactivate")
    class ReactivateTests {

        @Test
        @DisplayName("[TC-USR-051] ✅ Should set isActive to true and return profile")
        void reactivate_Success() {
            testUser.setIsActive(false);

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of("SUPPLIER"));
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.reactivate(userId);

            assertThat(result).isNotNull();
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            assertThat(captor.getValue().getIsActive()).isTrue();
        }

        @Test
        @DisplayName("[TC-USR-052] ❌ Should throw when user not found")
        void reactivate_NotFound() {
            when(userRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reactivate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-053] ✅ Should be idempotent when already active")
        void reactivate_AlreadyActive() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any())).thenReturn(testUser);
            when(userRoleRepository.findRoleNamesByUserId(userId)).thenReturn(List.of());
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            UserProfileResponse result = service.reactivate(userId);

            assertThat(result).isNotNull();
        }
    }
}
