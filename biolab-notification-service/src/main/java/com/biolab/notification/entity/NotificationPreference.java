package com.biolab.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences", schema = "app_schema")
public class NotificationPreference {
    @Id @GeneratedValue private UUID id;
    @Column(name = "user_id", nullable = false, unique = true) private UUID userId;
    @Column(name = "email_enabled") private Boolean emailEnabled = true;
    @Column(name = "project_updates") private Boolean projectUpdates = true;
    @Column(name = "new_messages") private Boolean newMessages = true;
    @Column(name = "invoice_reminders") private Boolean invoiceReminders = true;
    @Column(name = "security_alerts") private Boolean securityAlerts = true;
    private Boolean marketing = false;
    @Column(name = "updated_at") private Instant updatedAt;

    @PrePersist @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID u) { this.userId = u; }
    public Boolean getEmailEnabled() { return emailEnabled; }
    public void setEmailEnabled(Boolean e) { this.emailEnabled = e; }
    public Boolean getProjectUpdates() { return projectUpdates; }
    public void setProjectUpdates(Boolean p) { this.projectUpdates = p; }
    public Boolean getNewMessages() { return newMessages; }
    public void setNewMessages(Boolean n) { this.newMessages = n; }
    public Boolean getInvoiceReminders() { return invoiceReminders; }
    public void setInvoiceReminders(Boolean i) { this.invoiceReminders = i; }
    public Boolean getSecurityAlerts() { return securityAlerts; }
    public void setSecurityAlerts(Boolean s) { this.securityAlerts = s; }
    public Boolean getMarketing() { return marketing; }
    public void setMarketing(Boolean m) { this.marketing = m; }
    public Instant getUpdatedAt() { return updatedAt; }
}
