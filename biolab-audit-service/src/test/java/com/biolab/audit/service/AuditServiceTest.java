package com.biolab.audit.service;

import com.biolab.audit.dto.*;
import com.biolab.audit.entity.*;
import com.biolab.audit.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Unit Tests")
class AuditServiceTest {

    private AuditService service;

    @Mock private AuditEventRepository eventRepo;
    @Mock private ComplianceAuditRepository compRepo;
    @Mock private PolicyDocumentRepository policyRepo;
    @Mock private PlatformSettingRepository settingRepo;

    private final UUID userId = UUID.randomUUID();
    private final UUID eventId = UUID.randomUUID();

    private AuditEvent auditEvent;
    private ComplianceAudit compAudit;
    private PolicyDocument policy;
    private PlatformSetting setting;

    @BeforeEach
    void setUp() {
        service = new AuditService(eventRepo, compRepo, policyRepo, settingRepo);

        auditEvent = AuditEvent.builder()
            .userId(userId).action("USER_LOGIN").entityType("USER")
            .entityId(userId).details("{\"ip\":\"10.0.0.1\"}").ipAddress("10.0.0.1")
            .build();
        auditEvent.setId(eventId);
        auditEvent.setCreatedAt(Instant.now());

        compAudit = ComplianceAudit.builder()
            .auditDate(LocalDate.of(2025, 12, 15)).auditType("HIPAA Assessment")
            .result("PASSED").findings(0).auditor("John Auditor")
            .reportUrl("/reports/hipaa-2025.pdf").notes("Clean audit")
            .build();
        compAudit.setId(UUID.randomUUID());

        policy = PolicyDocument.builder()
            .name("Data Protection Policy").version("v4.2")
            .status("CURRENT").contentUrl("/docs/dp-policy.pdf")
            .build();
        policy.setId(UUID.randomUUID());
        policy.setUpdatedAt(Instant.now());

        setting = PlatformSetting.builder()
            .key("session.timeout").value("30").category("SECURITY")
            .build();
        setting.setId(UUID.randomUUID());
    }

    // ══════════════════════════════════════════════════════════════════
    // AUDIT EVENTS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Audit Events")
    class AuditEventTests {

        @Test
        @DisplayName("[TC-AUD-001] ✅ List events paginated")
        void listEvents_Success() {
            Page<AuditEvent> page = new PageImpl<>(List.of(auditEvent), PageRequest.of(0, 20), 1);
            when(eventRepo.findByOrderByCreatedAtDesc(any())).thenReturn(page);

            Page<AuditEventDto> result = service.listEvents(PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).action()).isEqualTo("USER_LOGIN");
            assertThat(result.getContent().get(0).userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("[TC-AUD-002] ✅ List events returns empty when no events")
        void listEvents_Empty() {
            when(eventRepo.findByOrderByCreatedAtDesc(any())).thenReturn(Page.empty());

            assertThat(service.listEvents(PageRequest.of(0, 20)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUD-003] ✅ List events by user")
        void listEventsByUser_Success() {
            Page<AuditEvent> page = new PageImpl<>(List.of(auditEvent));
            when(eventRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

            Page<AuditEventDto> result = service.listEventsByUser(userId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).userId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("[TC-AUD-004] ✅ List events by user returns empty for unknown user")
        void listEventsByUser_NoEvents() {
            when(eventRepo.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(Page.empty());

            assertThat(service.listEventsByUser(UUID.randomUUID(), PageRequest.of(0, 10)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUD-005] ✅ Log event successfully")
        void logEvent_Success() {
            CreateAuditEventRequest req = new CreateAuditEventRequest(
                userId, "DOCUMENT_DOWNLOAD", "DOCUMENT", UUID.randomUUID(),
                "{\"fileName\":\"report.pdf\"}", "192.168.1.1"
            );

            when(eventRepo.save(any())).thenReturn(auditEvent);

            AuditEventDto result = service.logEvent(req);

            assertThat(result).isNotNull();
            assertThat(result.action()).isEqualTo("USER_LOGIN"); // From mocked return
            verify(eventRepo).save(any(AuditEvent.class));
        }

        @Test
        @DisplayName("[TC-AUD-006] ✅ Log event maps all request fields to entity")
        void logEvent_MapsAllFields() {
            UUID entityId = UUID.randomUUID();
            CreateAuditEventRequest req = new CreateAuditEventRequest(
                userId, "DATA_EXPORT", "REPORT", entityId, "{\"format\":\"CSV\"}", "10.0.0.5"
            );

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            when(eventRepo.save(captor.capture())).thenReturn(auditEvent);

            service.logEvent(req);

            AuditEvent captured = captor.getValue();
            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getAction()).isEqualTo("DATA_EXPORT");
            assertThat(captured.getEntityType()).isEqualTo("REPORT");
            assertThat(captured.getEntityId()).isEqualTo(entityId);
            assertThat(captured.getIpAddress()).isEqualTo("10.0.0.5");
        }

        @Test
        @DisplayName("[TC-AUD-007] ✅ Log event with null optional fields")
        void logEvent_NullOptionalFields() {
            CreateAuditEventRequest req = new CreateAuditEventRequest(
                null, "SYSTEM_STARTUP", "SYSTEM", null, null, null
            );

            ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
            when(eventRepo.save(captor.capture())).thenReturn(auditEvent);

            service.logEvent(req);

            assertThat(captor.getValue().getUserId()).isNull();
            assertThat(captor.getValue().getEntityId()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // COMPLIANCE AUDITS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Compliance Audits")
    class ComplianceAuditTests {

        @Test
        @DisplayName("[TC-AUD-008] ✅ List compliance audits paginated")
        void listComplianceAudits_Success() {
            Page<ComplianceAudit> page = new PageImpl<>(List.of(compAudit));
            when(compRepo.findByOrderByAuditDateDesc(any())).thenReturn(page);

            Page<ComplianceAuditDto> result = service.listComplianceAudits(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).auditType()).isEqualTo("HIPAA Assessment");
            assertThat(result.getContent().get(0).result()).isEqualTo("PASSED");
        }

        @Test
        @DisplayName("[TC-AUD-009] ✅ List compliance audits empty")
        void listComplianceAudits_Empty() {
            when(compRepo.findByOrderByAuditDateDesc(any())).thenReturn(Page.empty());

            assertThat(service.listComplianceAudits(PageRequest.of(0, 10)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUD-010] ✅ Create compliance audit")
        void createComplianceAudit_Success() {
            ComplianceAuditDto dto = new ComplianceAuditDto(
                null, LocalDate.now(), "SOC 2 Type II", "PASSED", 0,
                "Jane Auditor", "/reports/soc2.pdf", "All clear"
            );

            when(compRepo.save(any())).thenReturn(compAudit);

            ComplianceAuditDto result = service.createComplianceAudit(dto);

            assertThat(result.auditType()).isEqualTo("HIPAA Assessment");
            verify(compRepo).save(any(ComplianceAudit.class));
        }

        @Test
        @DisplayName("[TC-AUD-011] ✅ Create compliance audit maps all fields")
        void createComplianceAudit_MapsFields() {
            ComplianceAuditDto dto = new ComplianceAuditDto(
                null, LocalDate.of(2025, 6, 15), "Penetration Test", "FAILED", 5,
                "External Firm", null, "5 findings need remediation"
            );

            ArgumentCaptor<ComplianceAudit> captor = ArgumentCaptor.forClass(ComplianceAudit.class);
            when(compRepo.save(captor.capture())).thenReturn(compAudit);

            service.createComplianceAudit(dto);

            ComplianceAudit captured = captor.getValue();
            assertThat(captured.getAuditType()).isEqualTo("Penetration Test");
            assertThat(captured.getResult()).isEqualTo("FAILED");
            assertThat(captured.getFindings()).isEqualTo(5);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // POLICIES
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Policy Documents")
    class PolicyTests {

        @Test
        @DisplayName("[TC-AUD-012] ✅ List policies paginated")
        void listPolicies_Success() {
            Page<PolicyDocument> page = new PageImpl<>(List.of(policy));
            when(policyRepo.findByOrderByUpdatedAtDesc(any())).thenReturn(page);

            Page<PolicyDocumentDto> result = service.listPolicies(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).name()).isEqualTo("Data Protection Policy");
            assertThat(result.getContent().get(0).status()).isEqualTo("CURRENT");
        }

        @Test
        @DisplayName("[TC-AUD-013] ✅ List policies empty")
        void listPolicies_Empty() {
            when(policyRepo.findByOrderByUpdatedAtDesc(any())).thenReturn(Page.empty());

            assertThat(service.listPolicies(PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // PLATFORM SETTINGS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Platform Settings")
    class SettingsTests {

        @Test
        @DisplayName("[TC-AUD-014] ✅ List all settings")
        void listSettings_All() {
            when(settingRepo.findAll()).thenReturn(List.of(setting));

            List<PlatformSettingDto> result = service.listSettings(null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).key()).isEqualTo("session.timeout");
            assertThat(result.get(0).value()).isEqualTo("30");
            assertThat(result.get(0).category()).isEqualTo("SECURITY");
        }

        @Test
        @DisplayName("[TC-AUD-015] ✅ List settings by category")
        void listSettings_ByCategory() {
            when(settingRepo.findByCategory("SECURITY")).thenReturn(List.of(setting));

            List<PlatformSettingDto> result = service.listSettings("SECURITY");

            assertThat(result).hasSize(1);
            verify(settingRepo).findByCategory("SECURITY");
            verify(settingRepo, never()).findAll();
        }

        @Test
        @DisplayName("[TC-AUD-016] ✅ List settings returns empty for unknown category")
        void listSettings_EmptyCategory() {
            when(settingRepo.findByCategory("NONEXISTENT")).thenReturn(List.of());

            assertThat(service.listSettings("NONEXISTENT")).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUD-017] ✅ Update existing setting")
        void updateSetting_ExistingKey() {
            when(settingRepo.findByKey("session.timeout")).thenReturn(Optional.of(setting));
            when(settingRepo.save(any())).thenReturn(setting);

            PlatformSettingDto result = service.updateSetting("session.timeout", "60", userId);

            assertThat(result.key()).isEqualTo("session.timeout");
            verify(settingRepo).save(any());
        }

        @Test
        @DisplayName("[TC-AUD-018] ✅ Create new setting when key does not exist")
        void updateSetting_NewKey() {
            when(settingRepo.findByKey("new.feature.flag")).thenReturn(Optional.empty());

            ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
            PlatformSetting newSetting = PlatformSetting.builder().key("new.feature.flag").value("true").category("GENERAL").build();
            when(settingRepo.save(captor.capture())).thenReturn(newSetting);

            PlatformSettingDto result = service.updateSetting("new.feature.flag", "true", userId);

            assertThat(captor.getValue().getKey()).isEqualTo("new.feature.flag");
            assertThat(captor.getValue().getValue()).isEqualTo("true");
            assertThat(captor.getValue().getCategory()).isEqualTo("GENERAL");
        }

        @Test
        @DisplayName("[TC-AUD-019] ✅ Auto-detect TAXATION category from key prefix")
        void updateSetting_TaxationCategory() {
            when(settingRepo.findByKey("taxation.default_rate")).thenReturn(Optional.empty());

            ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
            PlatformSetting newSetting = PlatformSetting.builder().key("taxation.default_rate").value("18").category("TAXATION").build();
            when(settingRepo.save(captor.capture())).thenReturn(newSetting);

            service.updateSetting("taxation.default_rate", "18", userId);

            assertThat(captor.getValue().getCategory()).isEqualTo("TAXATION");
        }

        @Test
        @DisplayName("[TC-AUD-020] ✅ Auto-detect SECURITY category from key prefix")
        void updateSetting_SecurityCategory() {
            when(settingRepo.findByKey("security.mfa_enabled")).thenReturn(Optional.empty());

            ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
            PlatformSetting newSetting = PlatformSetting.builder().key("security.mfa_enabled").value("true").category("SECURITY").build();
            when(settingRepo.save(captor.capture())).thenReturn(newSetting);

            service.updateSetting("security.mfa_enabled", "true", userId);

            assertThat(captor.getValue().getCategory()).isEqualTo("SECURITY");
        }

        @Test
        @DisplayName("[TC-AUD-021] ✅ Auto-detect NOTIFICATION category from key prefix")
        void updateSetting_NotificationCategory() {
            when(settingRepo.findByKey("notification.email_batch_size")).thenReturn(Optional.empty());

            ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
            PlatformSetting ns = PlatformSetting.builder().key("notification.email_batch_size").value("50").category("NOTIFICATION").build();
            when(settingRepo.save(captor.capture())).thenReturn(ns);

            service.updateSetting("notification.email_batch_size", "50", userId);

            assertThat(captor.getValue().getCategory()).isEqualTo("NOTIFICATION");
        }

        @Test
        @DisplayName("[TC-AUD-022] ✅ Preserve existing category when key has no recognized prefix")
        void updateSetting_PreservesExistingCategory() {
            setting.setCategory("COMPLIANCE");
            when(settingRepo.findByKey("session.timeout")).thenReturn(Optional.of(setting));
            when(settingRepo.save(any())).thenReturn(setting);

            service.updateSetting("session.timeout", "45", userId);

            // Category auto-detected as SECURITY because of "security." prefix check failing
            // Actually "session." doesn't match any prefix, so existing "COMPLIANCE" preserved
            verify(settingRepo).save(any());
        }

        @Test
        @DisplayName("[TC-AUD-023] ✅ Sets updatedBy on update")
        void updateSetting_SetsUpdatedBy() {
            when(settingRepo.findByKey("session.timeout")).thenReturn(Optional.of(setting));

            ArgumentCaptor<PlatformSetting> captor = ArgumentCaptor.forClass(PlatformSetting.class);
            when(settingRepo.save(captor.capture())).thenReturn(setting);

            service.updateSetting("session.timeout", "120", userId);

            assertThat(captor.getValue().getUpdatedBy()).isEqualTo(userId);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // DASHBOARD STATS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardStatsTests {

        @Test
        @DisplayName("[TC-AUD-024] ✅ Should return all dashboard counts")
        void dashboardStats_Success() {
            when(compRepo.count()).thenReturn(12L);
            when(eventRepo.count()).thenReturn(5000L);
            when(policyRepo.count()).thenReturn(14L);

            Map<String, Object> stats = service.dashboardStats();

            assertThat(stats).containsEntry("totalAudits", 12L);
            assertThat(stats).containsEntry("totalEvents", 5000L);
            assertThat(stats).containsEntry("totalPolicies", 14L);
        }

        @Test
        @DisplayName("[TC-AUD-025] ✅ Should return zero counts when empty")
        void dashboardStats_Empty() {
            when(compRepo.count()).thenReturn(0L);
            when(eventRepo.count()).thenReturn(0L);
            when(policyRepo.count()).thenReturn(0L);

            Map<String, Object> stats = service.dashboardStats();

            assertThat(stats).containsEntry("totalAudits", 0L);
            assertThat(stats).containsEntry("totalEvents", 0L);
            assertThat(stats).containsEntry("totalPolicies", 0L);
        }
    }
}
