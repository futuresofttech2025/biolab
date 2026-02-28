package com.biolab.user.service.impl;

import com.biolab.user.dto.request.OrganizationCreateRequest;
import com.biolab.user.dto.request.OrganizationUpdateRequest;
import com.biolab.user.dto.response.OrganizationResponse;
import com.biolab.user.dto.response.PageResponse;
import com.biolab.user.entity.Organization;
import com.biolab.user.entity.UserOrganization;
import com.biolab.user.entity.enums.OrganizationType;
import com.biolab.user.exception.ResourceNotFoundException;
import com.biolab.user.repository.OrganizationRepository;
import com.biolab.user.repository.UserOrganizationRepository;
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
@DisplayName("OrganizationServiceImpl Unit Tests")
class OrganizationServiceImplTest {

    @InjectMocks private OrganizationServiceImpl service;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private UserOrganizationRepository userOrgRepository;

    private Organization supplierOrg;
    private final UUID orgId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        supplierOrg = Organization.builder()
            .name("BioLab Genomics").type(OrganizationType.SUPPLIER)
            .address("123 Lab St").phone("+1-555-0100")
            .website("https://genomics.biolab.com").isActive(true)
            .build();
        supplierOrg.setId(orgId);
        supplierOrg.setCreatedAt(Instant.now());
    }

    // ══════════════════════════════════════════════════════════════════
    // CREATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("[TC-USR-001] ✅ Should create supplier organization")
        void create_Supplier() {
            OrganizationCreateRequest req = OrganizationCreateRequest.builder()
                .name("New Supplier Lab").type("SUPPLIER").address("456 Research Ave")
                .phone("+1-555-0200").website("https://newlab.com")
                .build();

            when(organizationRepository.save(any())).thenReturn(supplierOrg);

            OrganizationResponse result = service.create(req);

            assertThat(result.getId()).isEqualTo(orgId);
            assertThat(result.getName()).isEqualTo("BioLab Genomics");
            assertThat(result.getType()).isEqualTo("SUPPLIER");
            verify(organizationRepository).save(any(Organization.class));
        }

        @Test
        @DisplayName("[TC-USR-002] ✅ Should create buyer organization")
        void create_Buyer() {
            Organization buyerOrg = Organization.builder()
                .name("PharmaCo").type(OrganizationType.BUYER).isActive(true).build();
            buyerOrg.setId(UUID.randomUUID());
            buyerOrg.setCreatedAt(Instant.now());

            OrganizationCreateRequest req = OrganizationCreateRequest.builder()
                .name("PharmaCo").type("BUYER").build();

            when(organizationRepository.save(any())).thenReturn(buyerOrg);

            OrganizationResponse result = service.create(req);

            assertThat(result.getType()).isEqualTo("BUYER");
        }

        @Test
        @DisplayName("[TC-USR-003] ✅ Should trim name on creation")
        void create_TrimsName() {
            OrganizationCreateRequest req = OrganizationCreateRequest.builder()
                .name("  Padded Name  ").type("SUPPLIER").build();

            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            when(organizationRepository.save(captor.capture())).thenReturn(supplierOrg);

            service.create(req);

            assertThat(captor.getValue().getName()).isEqualTo("Padded Name");
        }

        @Test
        @DisplayName("[TC-USR-004] ❌ Should throw on invalid organization type")
        void create_InvalidType() {
            OrganizationCreateRequest req = OrganizationCreateRequest.builder()
                .name("Bad Corp").type("INVALID").build();

            assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("[TC-USR-005] ✅ Should default isActive to true")
        void create_DefaultsActive() {
            OrganizationCreateRequest req = OrganizationCreateRequest.builder()
                .name("Active Lab").type("SUPPLIER").build();

            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            when(organizationRepository.save(captor.capture())).thenReturn(supplierOrg);

            service.create(req);

            assertThat(captor.getValue().getIsActive()).isTrue();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GET BY ID
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getById")
    class GetByIdTests {

        @Test
        @DisplayName("[TC-USR-006] ✅ Should return organization with member count")
        void getById_Success() {
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(
                List.of(new UserOrganization(), new UserOrganization(), new UserOrganization())
            );

            OrganizationResponse result = service.getById(orgId);

            assertThat(result.getName()).isEqualTo("BioLab Genomics");
            assertThat(result.getMemberCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("[TC-USR-007] ❌ Should throw when not found")
        void getById_NotFound() {
            when(organizationRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-008] ✅ Should return zero member count for empty org")
        void getById_NoMembers() {
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(List.of());

            OrganizationResponse result = service.getById(orgId);

            assertThat(result.getMemberCount()).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SEARCH
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("search")
    class SearchTests {

        @Test
        @DisplayName("[TC-USR-009] ✅ Should search with all filters")
        void search_AllFilters() {
            Page<Organization> page = new PageImpl<>(List.of(supplierOrg), PageRequest.of(0, 10), 1);
            when(organizationRepository.searchOrganizations("bio", OrganizationType.SUPPLIER, true, PageRequest.of(0, 10)))
                .thenReturn(page);

            PageResponse<OrganizationResponse> result = service.search("bio", "SUPPLIER", true, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("[TC-USR-010] ✅ Should search with null type")
        void search_NullType() {
            when(organizationRepository.searchOrganizations("bio", null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(supplierOrg)));

            PageResponse<OrganizationResponse> result = service.search("bio", null, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("[TC-USR-011] ✅ Should return empty results")
        void search_Empty() {
            when(organizationRepository.searchOrganizations(any(), any(), any(), any())).thenReturn(Page.empty());

            PageResponse<OrganizationResponse> result = service.search("xyz", null, null, PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("[TC-USR-012] ❌ Should throw on invalid type filter")
        void search_InvalidType() {
            assertThatThrownBy(() -> service.search("bio", "INVALID", null, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("update")
    class UpdateTests {

        @Test
        @DisplayName("[TC-USR-013] ✅ Should update all provided fields")
        void update_AllFields() {
            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("Updated Name");
            req.setAddress("New Address");
            req.setPhone("+9999");
            req.setWebsite("https://updated.com");
            req.setIsActive(false);

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));
            when(organizationRepository.save(any())).thenReturn(supplierOrg);
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(List.of());

            OrganizationResponse result = service.update(orgId, req);

            assertThat(result).isNotNull();
            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Updated Name");
            assertThat(captor.getValue().getAddress()).isEqualTo("New Address");
            assertThat(captor.getValue().getIsActive()).isFalse();
        }

        @Test
        @DisplayName("[TC-USR-014] ✅ Should only update non-null fields")
        void update_PartialFields() {
            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("Only Name Changed");

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));
            when(organizationRepository.save(any())).thenReturn(supplierOrg);
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(List.of());

            service.update(orgId, req);

            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Only Name Changed");
            assertThat(captor.getValue().getAddress()).isEqualTo("123 Lab St"); // Unchanged
        }

        @Test
        @DisplayName("[TC-USR-015] ❌ Should throw when org not found")
        void update_NotFound() {
            when(organizationRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(UUID.randomUUID(), new OrganizationUpdateRequest()))
                .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-USR-016] ✅ Should trim name on update")
        void update_TrimsName() {
            OrganizationUpdateRequest req = new OrganizationUpdateRequest();
            req.setName("  Trimmed  ");

            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));
            when(organizationRepository.save(any())).thenReturn(supplierOrg);
            when(userOrgRepository.findByOrganizationId(orgId)).thenReturn(List.of());

            service.update(orgId, req);

            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            verify(organizationRepository).save(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Trimmed");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DEACTIVATE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("deactivate")
    class DeactivateTests {

        @Test
        @DisplayName("[TC-USR-017] ✅ Should set isActive to false")
        void deactivate_Success() {
            when(organizationRepository.findById(orgId)).thenReturn(Optional.of(supplierOrg));

            ArgumentCaptor<Organization> captor = ArgumentCaptor.forClass(Organization.class);
            when(organizationRepository.save(captor.capture())).thenReturn(supplierOrg);

            service.deactivate(orgId);

            assertThat(captor.getValue().getIsActive()).isFalse();
            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("[TC-USR-018] ❌ Should throw when org not found")
        void deactivate_NotFound() {
            when(organizationRepository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deactivate(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
