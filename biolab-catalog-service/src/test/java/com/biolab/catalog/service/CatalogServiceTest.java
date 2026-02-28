package com.biolab.catalog.service;

import com.biolab.catalog.dto.*;
import com.biolab.catalog.entity.*;
import com.biolab.catalog.entity.Service;
import com.biolab.catalog.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CatalogService Unit Tests")
class CatalogServiceTest {

    @InjectMocks private CatalogService service;
    @Mock private ServiceRepository serviceRepo;
    @Mock private ServiceCategoryRepository categoryRepo;
    @Mock private ServiceRequestRepository requestRepo;

    private ServiceCategory category;
    private Service sampleSvc;
    private final UUID catId = UUID.randomUUID(), svcId = UUID.randomUUID(), suppOrgId = UUID.randomUUID();

    @BeforeEach void setUp() {
        category = new ServiceCategory(); category.setId(catId); category.setName("Biochemical"); category.setSlug("biochemical"); category.setIsActive(true);
        sampleSvc = new Service(); sampleSvc.setId(svcId); sampleSvc.setName("HPLC"); sampleSvc.setSlug("hplc"); sampleSvc.setCategory(category);
        sampleSvc.setSupplierOrgId(suppOrgId); sampleSvc.setPriceFrom(BigDecimal.valueOf(2600)); sampleSvc.setRating(BigDecimal.valueOf(4.8)); sampleSvc.setReviewCount(41); sampleSvc.setIsActive(true);
    }

    @Test @DisplayName("[TC-CAT-001] ✅ List active categories") void listCategories_Ok() { when(categoryRepo.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of(category)); assertThat(service.listCategories()).hasSize(1); }
    @Test @DisplayName("[TC-CAT-002] ✅ Categories empty") void listCategories_Empty() { when(categoryRepo.findByIsActiveTrueOrderBySortOrder()).thenReturn(List.of()); assertThat(service.listCategories()).isEmpty(); }
    @Test @DisplayName("[TC-CAT-003] ✅ List services no filters") void listServices_All() { when(serviceRepo.findByIsActiveTrue(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(sampleSvc))); assertThat(service.listServices(null, null, PageRequest.of(0,10)).getTotalElements()).isEqualTo(1); }
    @Test @DisplayName("[TC-CAT-004] ✅ List services by category") void listServices_ByCat() { when(serviceRepo.findByCategoryIdAndIsActiveTrue(eq(catId), any())).thenReturn(new PageImpl<>(List.of(sampleSvc))); assertThat(service.listServices(catId, null, PageRequest.of(0,10)).getContent()).hasSize(1); }
    @Test @DisplayName("[TC-CAT-005] ✅ Search services by keyword") void listServices_Search() { when(serviceRepo.search(eq("HPLC"), any())).thenReturn(new PageImpl<>(List.of(sampleSvc))); assertThat(service.listServices(null, "HPLC", PageRequest.of(0,10)).getContent()).hasSize(1); }
    @Test @DisplayName("[TC-CAT-006] ✅ Search no results") void listServices_NoMatch() { when(serviceRepo.search(eq("zzz"), any())).thenReturn(Page.empty()); assertThat(service.listServices(null, "zzz", PageRequest.of(0,10)).getContent()).isEmpty(); }
    @Test @DisplayName("[TC-CAT-007] ✅ Get service by ID") void getService_Ok() { when(serviceRepo.findById(svcId)).thenReturn(Optional.of(sampleSvc)); assertThat(service.getService(svcId).name()).isEqualTo("HPLC"); }
    @Test @DisplayName("[TC-CAT-008] ❌ Get service not found") void getService_404() { when(serviceRepo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.getService(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class); }

    @Test @DisplayName("[TC-CAT-009] ✅ Create service") void createService_Ok() {
        CreateServiceRequest req = new CreateServiceRequest("New", catId, "Desc", "Method", BigDecimal.TEN, "1d");
        when(categoryRepo.findById(catId)).thenReturn(Optional.of(category)); when(serviceRepo.save(any())).thenReturn(sampleSvc);
        assertThat(service.createService(req, suppOrgId)).isNotNull(); verify(serviceRepo).save(any());
    }
    @Test @DisplayName("[TC-CAT-010] ❌ Create service bad category") void createService_BadCat() {
        when(categoryRepo.findById(any())).thenReturn(Optional.empty());
        assertThatThrownBy(()->service.createService(new CreateServiceRequest("x", UUID.randomUUID(), null, null, null, null), suppOrgId)).isInstanceOf(ResourceNotFoundException.class);
    }
    @Test @DisplayName("[TC-CAT-011] ✅ Update service") void updateService_Ok() { when(serviceRepo.findById(svcId)).thenReturn(Optional.of(sampleSvc)); when(serviceRepo.save(any())).thenReturn(sampleSvc); assertThat(service.updateService(svcId, new CreateServiceRequest("Upd", catId, "D", null, null, null))).isNotNull(); }
    @Test @DisplayName("[TC-CAT-012] ❌ Update service not found") void updateService_404() { when(serviceRepo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.updateService(UUID.randomUUID(), new CreateServiceRequest("x", catId, null, null, null, null))).isInstanceOf(ResourceNotFoundException.class); }
    @Test @DisplayName("[TC-CAT-013] ✅ Toggle service") void toggle_Ok() { when(serviceRepo.findById(svcId)).thenReturn(Optional.of(sampleSvc)); service.toggleService(svcId, false); ArgumentCaptor<Service> c=ArgumentCaptor.forClass(Service.class); verify(serviceRepo).save(c.capture()); assertThat(c.getValue().getIsActive()).isFalse(); }
    @Test @DisplayName("[TC-CAT-014] ❌ Toggle not found") void toggle_404() { when(serviceRepo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.toggleService(UUID.randomUUID(), true)).isInstanceOf(ResourceNotFoundException.class); }
    @Test @DisplayName("[TC-CAT-015] ✅ List by supplier") void listBySupplier_Ok() { when(serviceRepo.findBySupplierOrgId(eq(suppOrgId), any())).thenReturn(new PageImpl<>(List.of(sampleSvc))); assertThat(service.listBySupplier(suppOrgId, PageRequest.of(0,10)).getTotalElements()).isEqualTo(1); }

    // Service Requests
    @Test @DisplayName("[TC-CAT-016] ✅ Create request") void createReq_Ok() {
        ServiceRequest sr = new ServiceRequest(); sr.setId(UUID.randomUUID()); sr.setService(sampleSvc); sr.setBuyerId(UUID.randomUUID()); sr.setStatus("PENDING");
        when(serviceRepo.findById(svcId)).thenReturn(Optional.of(sampleSvc)); when(requestRepo.save(any())).thenReturn(sr);
        assertThat(service.createRequest(new CreateServiceRequestRequest(svcId, "Blood", null, "Req", null), UUID.randomUUID(), UUID.randomUUID()).status()).isEqualTo("PENDING");
    }
    @Test @DisplayName("[TC-CAT-017] ❌ Create request bad service") void createReq_BadSvc() { when(serviceRepo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.createRequest(new CreateServiceRequestRequest(UUID.randomUUID(), "x", null, null, null), UUID.randomUUID(), UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class); }
    @Test @DisplayName("[TC-CAT-018] ✅ Accept request") void acceptReq_Ok() { ServiceRequest sr = new ServiceRequest(); sr.setId(UUID.randomUUID()); sr.setService(sampleSvc); sr.setBuyerId(UUID.randomUUID()); sr.setStatus("PENDING"); when(requestRepo.findById(any())).thenReturn(Optional.of(sr)); when(requestRepo.save(any())).thenReturn(sr); service.updateRequestStatus(sr.getId(), "ACCEPTED"); verify(requestRepo).save(any()); }
    @Test @DisplayName("[TC-CAT-019] ❌ Update request not found") void updateReq_404() { when(requestRepo.findById(any())).thenReturn(Optional.empty()); assertThatThrownBy(()->service.updateRequestStatus(UUID.randomUUID(), "ACCEPTED")).isInstanceOf(ResourceNotFoundException.class); }
    @Test @DisplayName("[TC-CAT-020] ✅ Supplier stats") void stats_Ok() { when(serviceRepo.countBySupplierOrgId(suppOrgId)).thenReturn(5L); when(requestRepo.countByServiceSupplierOrgIdAndStatus(suppOrgId, "PENDING")).thenReturn(3L); Map<String,Object> s = service.supplierStats(suppOrgId); assertThat(s).containsEntry("totalServices", 5L).containsEntry("pendingRequests", 3L); }
    @Test @DisplayName("[TC-CAT-021] ✅ Default timeline and priority on request") void createReq_Defaults() { when(serviceRepo.findById(svcId)).thenReturn(Optional.of(sampleSvc)); ArgumentCaptor<ServiceRequest> c = ArgumentCaptor.forClass(ServiceRequest.class); ServiceRequest sr = new ServiceRequest(); sr.setId(UUID.randomUUID()); sr.setService(sampleSvc); sr.setBuyerId(UUID.randomUUID()); when(requestRepo.save(c.capture())).thenReturn(sr); service.createRequest(new CreateServiceRequestRequest(svcId, "S", null, "R", null), UUID.randomUUID(), UUID.randomUUID()); assertThat(c.getValue().getTimeline()).isEqualTo("STANDARD"); assertThat(c.getValue().getPriority()).isEqualTo("MEDIUM"); }
}
