package com.biolab.messaging.repository;

import com.biolab.messaging.entity.Message;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    Page<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    long countByConversationIdAndIsReadFalseAndSenderIdNot(UUID conversationId, UUID userId);
}
