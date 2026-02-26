package com.biolab.audit.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "compliance_audits", schema = "app_schema")
public class ComplianceAudit {
    @Id @GeneratedValue private UUID id;
    @Column(name = "audit_date", nullable = false) private LocalDate auditDate;
    @Column(name = "audit_type", nullable = false, length = 100) private String auditType;
    @Column(length = 20) private String result = "PASSED";
    private Integer findings = 0;
    private String auditor;
    @Column(name = "report_url", length = 512) private String reportUrl;
    private String notes;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public void setAuditDate(LocalDate d) { this.auditDate = d; }

    public void setAuditType(String t) { this.auditType = t; }

    public void setResult(String r) { this.result = r; }

    public void setFindings(Integer f) { this.findings = f; }

    public void setAuditor(String a) { this.auditor = a; }

    public void setReportUrl(String u) { this.reportUrl = u; }

    public void setNotes(String n) { this.notes = n; }
}
