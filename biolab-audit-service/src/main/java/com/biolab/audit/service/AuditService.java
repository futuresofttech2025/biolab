package com.biolab.audit.service;

import com.biolab.audit.dto.*;
import com.biolab.audit.entity.*;
import com.biolab.audit.repository.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Getter
@Setter
@Builder
@AllArgsConstructor
@Slf4j
public class AuditService {

    private final AuditEventRepository eventRepo;
    private final ComplianceAuditRepository compRepo;
    private final PolicyDocumentRepository policyRepo;
    private final PlatformSettingRepository settingRepo;

    @Transactional(readOnly = true)
    public Page<AuditEventDto> listEvents(Pageable pageable) {
        return eventRepo.findByOrderByCreatedAtDesc(pageable).map(this::toEventDto);
    }

    @Transactional(readOnly = true)
    public Page<AuditEventDto> listEventsByUser(UUID userId, Pageable pageable) {
        return eventRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toEventDto);
    }

    public AuditEventDto logEvent(CreateAuditEventRequest req) {
        AuditEvent e = AuditEvent.builder()
            .userId(req.userId()).action(req.action())
            .entityType(req.entityType()).entityId(req.entityId())
            .details(req.details()).ipAddress(req.ipAddress())
            .build();
        log.info("Audit event logged: user={}, action={}", req.userId(), req.action());
        return toEventDto(eventRepo.save(e));
    }

    @Transactional(readOnly = true)
    public Page<ComplianceAuditDto> listComplianceAudits(Pageable pageable) {
        return compRepo.findByOrderByAuditDateDesc(pageable).map(this::toCompDto);
    }

    public ComplianceAuditDto createComplianceAudit(ComplianceAuditDto dto) {
        ComplianceAudit ca = ComplianceAudit.builder()
            .auditDate(dto.auditDate()).auditType(dto.auditType())
            .result(dto.result()).findings(dto.findings())
            .auditor(dto.auditor()).reportUrl(dto.reportUrl()).notes(dto.notes())
            .build();
        log.info("Compliance audit created: type={}", dto.auditType());
        return toCompDto(compRepo.save(ca));
    }

    @Transactional(readOnly = true)
    public Page<PolicyDocumentDto> listPolicies(Pageable pageable) {
        return policyRepo.findByOrderByUpdatedAtDesc(pageable).map(this::toPolicyDto);
    }

    @Transactional(readOnly = true)
    public List<PlatformSettingDto> listSettings(String category) {
        List<PlatformSetting> list = category != null ? settingRepo.findByCategory(category) : settingRepo.findAll();
        return list.stream().map(s -> new PlatformSettingDto(s.getKey(), s.getValue(), s.getCategory()))
            .collect(Collectors.toList());
    }

    public PlatformSettingDto updateSetting(String key, String value, UUID updatedBy) {
        PlatformSetting s = settingRepo.findByKey(key).orElseGet(() -> PlatformSetting.builder().key(key).build());
        s.setValue(value); s.setUpdatedBy(updatedBy);
        // Auto-detect category from key prefix
        if (key.startsWith("taxation.")) s.setCategory("TAXATION");
        else if (key.startsWith("security.")) s.setCategory("SECURITY");
        else if (key.startsWith("notification.")) s.setCategory("NOTIFICATION");
        else if (s.getCategory() == null) s.setCategory("GENERAL");
        settingRepo.save(s);
        log.info("Platform setting updated: key='{}', category='{}'", key, s.getCategory());
        return new PlatformSettingDto(s.getKey(), s.getValue(), s.getCategory());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboardStats() {
        return Map.of("totalAudits", compRepo.count(), "totalEvents", eventRepo.count(), "totalPolicies", policyRepo.count());
    }

    private AuditEventDto toEventDto(AuditEvent e) {
        return new AuditEventDto(e.getId(), e.getUserId(), e.getAction(), e.getEntityType(), e.getEntityId(), e.getDetails(), e.getIpAddress(), e.getCreatedAt());
    }
    private ComplianceAuditDto toCompDto(ComplianceAudit c) {
        return new ComplianceAuditDto(c.getId(), c.getAuditDate(), c.getAuditType(), c.getResult(), c.getFindings(), c.getAuditor(), c.getReportUrl(), c.getNotes());
    }
    private PolicyDocumentDto toPolicyDto(PolicyDocument p) {
        return new PolicyDocumentDto(p.getId(), p.getName(), p.getVersion(), p.getStatus(), p.getContentUrl(), p.getUpdatedAt());
    }
}
