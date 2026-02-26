package com.biolab.messaging.repository;

import com.biolab.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.*;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {
    Optional<Conversation> findByProjectId(UUID projectId);
    @Query("SELECT c FROM Conversation c JOIN ConversationParticipant p ON c.id = p.conversationId WHERE p.userId = :userId ORDER BY c.updatedAt DESC")
    List<Conversation> findByParticipant(UUID userId);
}
