package com.biolab.audit.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    @InjectMocks
    private AuditService service;


    @Mock private com.biolab.audit.repository.AuditEventRepository eventRepo;
    @Mock private com.biolab.audit.repository.ComplianceAuditRepository compRepo;
    @Mock private com.biolab.audit.repository.PolicyDocumentRepository policyRepo;
    @Mock private com.biolab.audit.repository.PlatformSettingRepository settingRepo;

    @Test
    @DisplayName("listEvents returns paginated audit events")
    void listEvents_returnsPaginated() {
        when(eventRepo.findByOrderByCreatedAtDesc(any(Pageable.class)))
            .thenReturn(Page.empty());
        Page<?> result = service.listEvents(PageRequest.of(0, 10));
        assertNotNull(result);
    }

    @Test
    @DisplayName("dashboardStats returns counts")
    void dashboardStats_returns() {
        when(compRepo.count()).thenReturn(12L);
        when(eventRepo.count()).thenReturn(500L);
        when(policyRepo.count()).thenReturn(14L);
        var stats = service.dashboardStats();
        assertEquals(12L, stats.get("totalAudits"));
        assertEquals(500L, stats.get("totalEvents"));
    }

}
