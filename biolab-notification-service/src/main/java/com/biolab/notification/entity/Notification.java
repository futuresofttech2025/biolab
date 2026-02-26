package com.biolab.notification.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications", schema = "app_schema")
public class Notification {
    @Id @GeneratedValue private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(nullable = false, length = 50) private String type;
    @Column(nullable = false) private String title;
    private String message;
    @Column(length = 500) private String link;
    @Column(name = "is_read") private Boolean isRead = false;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID u) { this.userId = u; }
    public String getType() { return type; }
    public void setType(String t) { this.type = t; }
    public String getTitle() { return title; }
    public void setTitle(String t) { this.title = t; }
    public String getMessage() { return message; }
    public void setMessage(String m) { this.message = m; }
    public String getLink() { return link; }
    public void setLink(String l) { this.link = l; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean r) { this.isRead = r; }
    public Instant getCreatedAt() { return createdAt; }
}
