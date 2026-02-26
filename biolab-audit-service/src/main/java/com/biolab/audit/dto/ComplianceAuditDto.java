package com.biolab.audit.dto;
import java.time.LocalDate;
import java.util.UUID;
public record ComplianceAuditDto(UUID id, LocalDate auditDate, String auditType,
    String result, int findings, String auditor, String reportUrl, String notes) {}
