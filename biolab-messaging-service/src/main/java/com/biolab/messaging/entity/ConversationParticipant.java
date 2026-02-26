package com.biolab.messaging.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "conversation_participants", schema = "app_schema")
public class ConversationParticipant {
    @Id @GeneratedValue private UUID id;
    @Column(name = "conversation_id", nullable = false) private UUID conversationId;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Column(name = "org_id") private UUID orgId;
    @Column(name = "joined_at") private Instant joinedAt;
    @Column(name = "last_read_at") private Instant lastReadAt;

    @PrePersist void onCreate() { joinedAt = Instant.now(); }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID id) { this.conversationId = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID id) { this.userId = id; }
    public UUID getOrgId() { return orgId; }
    public void setOrgId(UUID id) { this.orgId = id; }
    public Instant getJoinedAt() { return joinedAt; }
    public Instant getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(Instant t) { this.lastReadAt = t; }
}
