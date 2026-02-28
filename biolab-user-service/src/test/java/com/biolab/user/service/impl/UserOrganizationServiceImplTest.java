package com.biolab.user.service.impl;

import com.biolab.user.dto.request.UserOrganizationAssignRequest;
import com.biolab.user.dto.response.UserOrganizationResponse;
import com.biolab.user.entity.Organization;
import com.biolab.user.entity.UserOrganization;
import com.biolab.user.entity.enums.OrganizationType;
import com.biolab.user.exception.DuplicateResourceException;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.OrganizationRepository;
import com.biolab.user.repository.UserOrganizationRepository;
import com.biolab.user.repository.UserRepository;
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
@DisplayName("UserOrganizationServiceImpl Unit Tests")
class UserOrganizationServiceImplTest {

    @InjectMocks private UserOrganizationServiceImpl service;
    @Mock private UserOrganizationRepository userOrgRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;

    private final UUID userId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();
    private Organization org;
    private UserOrganization userOrg;

    @BeforeEach
    void setUp() {
        org = Organization.builder()
            .name("BioLab Alpha").type(OrganizationType.SUPPLIER).isActive(true).build();
        org.setId(orgId);
        org.setCreatedAt(Instant.now());

        userOrg = UserOrganization.builder()
            .userId(userId).organization(org).roleInOrg("MANAGER").isPrimary(true)
            .build();
        userOrg.setId(UUID.randomUUID());
    }

    // ══════════════════════════════════════════════════════════════════
    // ADD MEMBER
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addMember")
    class AddMemberTests {

        @Test
        @DisplayName("[TC-USR-019] ✅ Should add user to organization successfully")
        void addMember_Success() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(orgId).roleInOrg("MANAGER").isPrimary(true).build();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(false);
            when(userOrgRepository.save(any())).thenReturn(userOrg);

            UserOrganizationResponse result = service.addMember(userId, req);

            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getOrganizationName()).isEqualTo("BioLab Alpha");
            assertThat(result.getRoleInOrg()).isEqualTo("MANAGER");
            assertThat(result.getIsPrimary()).isTrue();
            verify(userOrgRepository).save(any(UserOrganization.class));
        }

        @Test
        @DisplayName("[TC-USR-020] ✅ Should default roleInOrg to MEMBER when null")
        void addMember_DefaultRole() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(orgId).build(); // null roleInOrg

            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(false);

            ArgumentCaptor<UserOrganization> captor = ArgumentCaptor.forClass(UserOrganization.class);
            when(userOrgRepository.save(captor.capture())).thenReturn(userOrg);

            service.addMember(userId, req);

            assertThat(captor.getValue().getRoleInOrg()).isEqualTo("MEMBER");
        }

        @Test
        @DisplayName("[TC-USR-021] ✅ Should default isPrimary to false when null")
        void addMember_DefaultNotPrimary() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(orgId).roleInOrg("MEMBER").build(); // null isPrimary

            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(false);

            ArgumentCaptor<UserOrganization> captor = ArgumentCaptor.forClass(UserOrganization.class);
            when(userOrgRepository.save(captor.capture())).thenReturn(userOrg);

            service.addMember(userId, req);

            assertThat(captor.getValue().getIsPrimary()).isFalse();
        }

        @Test
        @DisplayName("[TC-USR-022] ❌ Should throw when user does not exist")
        void addMember_UserNotFound() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(orgId).build();

            when(userRepository.existsById(userId)).thenReturn(false);

            assertThatThrownBy(() -> service.addMember(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(userOrgRepository, never()).save(any());
        }

        @Test
        @DisplayName("[TC-USR-023] ❌ Should throw when organization does not exist")
        void addMember_OrgNotFound() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(UUID.randomUUID()).build();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMember(userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(userOrgRepository, never()).save(any());
        }

        @Test
        @DisplayName("[TC-USR-024] ❌ Should throw on duplicate membership")
        void addMember_Duplicate() {
            UserOrganizationAssignRequest req = UserOrganizationAssignRequest.builder()
                .organizationId(orgId).build();

            when(userRepository.existsById(userId)).thenReturn(true);
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(org));
            when(userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(true);

            assertThatThrownBy(() -> service.addMember(userId, req))
                .isInstanceOf(DuplicateResourceException.class);
            verify(userOrgRepository, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GET BY USER ID
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getByUserId")
    class GetByUserIdTests {

        @Test
        @DisplayName("[TC-USR-025] ✅ Should return user's organizations")
        void getByUserId_Success() {
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of(userOrg));

            List<UserOrganizationResponse> result = service.getByUserId(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOrganizationName()).isEqualTo("BioLab Alpha");
            assertThat(result.get(0).getOrganizationType()).isEqualTo("SUPPLIER");
        }

        @Test
        @DisplayName("[TC-USR-026] ✅ Should return empty list when user has no orgs")
        void getByUserId_Empty() {
            when(userOrgRepository.findByUserId(userId)).thenReturn(List.of());

            assertThat(service.getByUserId(userId)).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GET BY ORGANIZATION ID
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getByOrganizationId")
    class GetByOrganizationIdTests {

        @Test
        @DisplayName("[TC-USR-027] ✅ Should return org members")
        void getByOrgId_Success() {
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(List.of(userOrg));

            List<UserOrganizationResponse> result = service.getByOrganizationId(orgId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("[TC-USR-028] ✅ Should return empty for org with no members")
        void getByOrgId_Empty() {
            when(userOrgRepository.findByOrganizationId(any())).thenReturn(List.of());

            assertThat(service.getByOrganizationId(UUID.randomUUID())).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE MEMBERSHIP
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateMembership")
    class UpdateMembershipTests {

        @Test
        @DisplayName("[TC-USR-029] ✅ Should update role and primary flag")
        void update_AllFields() {
            when(userOrgRepository.findByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(Optional.of(userOrg));
            when(userOrgRepository.save(any())).thenReturn(userOrg);

            UserOrganizationResponse result = service.updateMembership(userId, orgId, "ADMIN", false);

            assertThat(result).isNotNull();
            ArgumentCaptor<UserOrganization> captor = ArgumentCaptor.forClass(UserOrganization.class);
            verify(userOrgRepository).save(captor.capture());
            assertThat(captor.getValue().getRoleInOrg()).isEqualTo("ADMIN");
            assertThat(captor.getValue().getIsPrimary()).isFalse();
        }

        @Test
        @DisplayName("[TC-USR-030] ✅ Should update only role when isPrimary is null")
        void update_OnlyRole() {
            when(userOrgRepository.findByUserIdAndOrganizationId(userId, orgId))
                .thenReturn(Optional.of(userOrg));
            when(userOrgRepository.save(any())).thenReturn(userOrg);

            service.updateMembership(userId, orgId, "VIEWER", null);

            ArgumentCaptor<UserOrganization> captor = ArgumentCaptor.forClass(UserOrganization.class);
            verify(userOrgRepository).save(captor.capture());
            assertThat(captor.getValue().getRoleInOrg()).isEqualTo("VIEWER");
            assertThat(captor.getValue().getIsPrimary()).isTrue(); // unchanged from setUp
        }

        @Test
        @DisplayName("[TC-USR-031] ❌ Should throw when membership not found")
        void update_NotFound() {
            when(userOrgRepository.findByUserIdAndOrganizationId(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMembership(userId, orgId, "ADMIN", null))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // REMOVE MEMBER
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("removeMember")
    class RemoveMemberTests {

        @Test
        @DisplayName("[TC-USR-032] ✅ Should remove member successfully")
        void remove_Success() {
            when(userOrgRepository.existsByUserIdAndOrganizationId(userId, orgId)).thenReturn(true);

            service.removeMember(userId, orgId);

            verify(userOrgRepository).deleteByUserIdAndOrganizationId(userId, orgId);
        }

        @Test
        @DisplayName("[TC-USR-033] ❌ Should throw when membership not found")
        void remove_NotFound() {
            when(userOrgRepository.existsByUserIdAndOrganizationId(any(), any())).thenReturn(false);

            assertThatThrownBy(() -> service.removeMember(userId, orgId))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(userOrgRepository, never()).deleteByUserIdAndOrganizationId(any(), any());
        }
    }
}
