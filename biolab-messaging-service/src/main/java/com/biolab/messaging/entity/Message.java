package com.biolab.messaging.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "messages", schema = "app_schema")
public class Message {
    @Id @GeneratedValue private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "sender_id", nullable = false) private UUID senderId;
    @Column(nullable = false) private String content;
    @Column(name = "attachment_id") private UUID attachmentId;
    @Column(name = "is_read") private Boolean isRead = false;
    @Column(name = "created_at") private Instant createdAt;

    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID id) { this.conversationId = id; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID id) { this.senderId = id; }
    public String getContent() { return content; }
    public void setContent(String c) { this.content = c; }
    public UUID getAttachmentId() { return attachmentId; }
    public void setAttachmentId(UUID id) { this.attachmentId = id; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean r) { this.isRead = r; }
    public Instant getCreatedAt() { return createdAt; }
}
