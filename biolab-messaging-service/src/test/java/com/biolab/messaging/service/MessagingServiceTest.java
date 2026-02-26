package com.biolab.messaging.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MessagingService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MessagingService Unit Tests")
class MessagingServiceTest {

    @InjectMocks
    private MessagingService service;


    @Mock private com.biolab.messaging.repository.ConversationRepository convRepo;
    @Mock private com.biolab.messaging.repository.ConversationParticipantRepository partRepo;
    @Mock private com.biolab.messaging.repository.MessageRepository msgRepo;

    @Test
    @DisplayName("listConversations returns user conversations")
    void listConversations_returnsForUser() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        when(convRepo.findByParticipant(userId)).thenReturn(java.util.List.of());
        var result = service.listConversations(userId);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

}
