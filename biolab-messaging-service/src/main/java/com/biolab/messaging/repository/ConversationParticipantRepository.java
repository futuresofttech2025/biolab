package com.biolab.messaging.repository;

import com.biolab.messaging.entity.ConversationParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {
    List<ConversationParticipant> findByConversationId(UUID conversationId);
    Optional<ConversationParticipant> findByConversationIdAndUserId(UUID conversationId, UUID userId);
}
