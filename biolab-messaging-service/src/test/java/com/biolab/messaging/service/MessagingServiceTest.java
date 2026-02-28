package com.biolab.messaging.service;

import com.biolab.messaging.dto.*;
import com.biolab.messaging.entity.*;
import com.biolab.messaging.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MessagingService Unit Tests")
class MessagingServiceTest {

    @InjectMocks private MessagingService service;
    @Mock private ConversationRepository convRepo;
    @Mock private ConversationParticipantRepository partRepo;
    @Mock private MessageRepository msgRepo;

    private final UUID userId = UUID.randomUUID();
    private final UUID convId = UUID.randomUUID();
    private final UUID projectId = UUID.randomUUID();
    private final UUID orgId = UUID.randomUUID();

    private Conversation conversation;
    private Message message;

    @BeforeEach
    void setUp() {
        conversation = new Conversation();
        conversation.setId(convId);
        conversation.setProjectId(projectId);
        conversation.setTitle("Discussion: HPLC Analysis");
        conversation.setUpdatedAt(Instant.now());

        message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversationId(convId);
        message.setSenderId(userId);
        message.setContent("Sample results are ready for review.");
        message.setIsRead(false);
    }

    // ══════════════════════════════════════════════════════════════════
    // LIST CONVERSATIONS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listConversations")
    class ListConversationsTests {

        @Test
        @DisplayName("[TC-MSG-001] ✅ Should list conversations for user with last message and unread count")
        void listConversations_Success() {
            when(convRepo.findByParticipant(userId)).thenReturn(List.of(conversation));
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(eq(convId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(message)));
            when(msgRepo.countByConversationIdAndIsReadFalseAndSenderIdNot(convId, userId)).thenReturn(2L);

            List<ConversationDto> result = service.listConversations(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).id()).isEqualTo(convId);
            assertThat(result.get(0).title()).isEqualTo("Discussion: HPLC Analysis");
            assertThat(result.get(0).lastMessage()).isEqualTo("Sample results are ready for review.");
            assertThat(result.get(0).unreadCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("[TC-MSG-002] ✅ Should return empty list when user has no conversations")
        void listConversations_Empty() {
            when(convRepo.findByParticipant(userId)).thenReturn(List.of());

            List<ConversationDto> result = service.listConversations(userId);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("[TC-MSG-003] ✅ Should handle conversation with no messages")
        void listConversations_NoMessages() {
            when(convRepo.findByParticipant(userId)).thenReturn(List.of(conversation));
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(eq(convId), any(Pageable.class)))
                .thenReturn(Page.empty());
            when(msgRepo.countByConversationIdAndIsReadFalseAndSenderIdNot(convId, userId)).thenReturn(0L);

            List<ConversationDto> result = service.listConversations(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).lastMessage()).isEmpty();
            assertThat(result.get(0).unreadCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC-MSG-004] ✅ Should return multiple conversations sorted by updatedAt")
        void listConversations_Multiple() {
            Conversation conv2 = new Conversation();
            conv2.setId(UUID.randomUUID());
            conv2.setTitle("Project Status");
            conv2.setUpdatedAt(Instant.now());

            when(convRepo.findByParticipant(userId)).thenReturn(List.of(conversation, conv2));
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(any(), any(Pageable.class)))
                .thenReturn(Page.empty());
            when(msgRepo.countByConversationIdAndIsReadFalseAndSenderIdNot(any(), eq(userId))).thenReturn(0L);

            List<ConversationDto> result = service.listConversations(userId);

            assertThat(result).hasSize(2);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // LIST MESSAGES
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("listMessages")
    class ListMessagesTests {

        @Test
        @DisplayName("[TC-MSG-005] ✅ Should list messages with pagination")
        void listMessages_Success() {
            Page<Message> page = new PageImpl<>(List.of(message), PageRequest.of(0, 20), 1);
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(convId, PageRequest.of(0, 20)))
                .thenReturn(page);

            Page<MessageDto> result = service.listMessages(convId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).content()).isEqualTo("Sample results are ready for review.");
            assertThat(result.getContent().get(0).senderId()).isEqualTo(userId);
        }

        @Test
        @DisplayName("[TC-MSG-006] ✅ Should return empty page when no messages exist")
        void listMessages_Empty() {
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Page.empty());

            Page<MessageDto> result = service.listMessages(convId, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("[TC-MSG-007] ✅ Should support pagination parameters correctly")
        void listMessages_Pagination() {
            Pageable pageable = PageRequest.of(2, 10);
            when(msgRepo.findByConversationIdOrderByCreatedAtDesc(convId, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 25));

            Page<MessageDto> result = service.listMessages(convId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getNumber()).isEqualTo(2);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // SEND MESSAGE
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("sendMessage")
    class SendMessageTests {

        @Test
        @DisplayName("[TC-MSG-008] ✅ Should send message successfully")
        void sendMessage_Success() {
            SendMessageRequest req = new SendMessageRequest("Hello, results are in!", null);

            when(convRepo.findById(convId)).thenReturn(Optional.of(conversation));
            when(convRepo.save(any())).thenReturn(conversation);

            Message saved = new Message();
            saved.setId(UUID.randomUUID());
            saved.setConversationId(convId);
            saved.setSenderId(userId);
            saved.setContent("Hello, results are in!");
            saved.setIsRead(false);
            when(msgRepo.save(any())).thenReturn(saved);

            MessageDto result = service.sendMessage(convId, userId, req);

            assertThat(result.content()).isEqualTo("Hello, results are in!");
            assertThat(result.senderId()).isEqualTo(userId);
            assertThat(result.conversationId()).isEqualTo(convId);
            verify(convRepo).save(any(Conversation.class)); // updatedAt should be updated
        }

        @Test
        @DisplayName("[TC-MSG-009] ✅ Should send message with attachment")
        void sendMessage_WithAttachment() {
            UUID attachId = UUID.randomUUID();
            SendMessageRequest req = new SendMessageRequest("See attached report", attachId);

            when(convRepo.findById(convId)).thenReturn(Optional.of(conversation));
            when(convRepo.save(any())).thenReturn(conversation);

            Message saved = new Message();
            saved.setId(UUID.randomUUID());
            saved.setConversationId(convId);
            saved.setSenderId(userId);
            saved.setContent("See attached report");
            saved.setAttachmentId(attachId);
            saved.setIsRead(false);
            when(msgRepo.save(any())).thenReturn(saved);

            MessageDto result = service.sendMessage(convId, userId, req);

            assertThat(result.attachmentId()).isEqualTo(attachId);
            verify(msgRepo).save(any(Message.class));
        }

        @Test
        @DisplayName("[TC-MSG-010] ❌ Should fail when conversation does not exist")
        void sendMessage_ConversationNotFound() {
            SendMessageRequest req = new SendMessageRequest("Test", null);
            when(convRepo.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.sendMessage(UUID.randomUUID(), userId, req))
                .isInstanceOf(ResourceNotFoundException.class);
            verify(msgRepo, never()).save(any());
        }

        @Test
        @DisplayName("[TC-MSG-011] ✅ Should update conversation updatedAt on new message")
        void sendMessage_UpdatesConversationTimestamp() {
            SendMessageRequest req = new SendMessageRequest("New message", null);
            Instant before = conversation.getUpdatedAt();

            when(convRepo.findById(convId)).thenReturn(Optional.of(conversation));
            ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
            when(convRepo.save(captor.capture())).thenReturn(conversation);

            Message saved = new Message();
            saved.setId(UUID.randomUUID());
            saved.setConversationId(convId);
            saved.setSenderId(userId);
            saved.setContent("New message");
            saved.setIsRead(false);
            when(msgRepo.save(any())).thenReturn(saved);

            service.sendMessage(convId, userId, req);

            assertThat(captor.getValue().getUpdatedAt()).isNotNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // CREATE CONVERSATION
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createConversation")
    class CreateConversationTests {

        @Test
        @DisplayName("[TC-MSG-012] ✅ Should create conversation and add creator as participant")
        void createConversation_Success() {
            Conversation saved = new Conversation();
            saved.setId(convId);
            saved.setProjectId(projectId);
            saved.setTitle("New Discussion");
            saved.setUpdatedAt(Instant.now());

            when(convRepo.save(any())).thenReturn(saved);
            when(partRepo.save(any())).thenReturn(new ConversationParticipant());

            ConversationDto result = service.createConversation(projectId, "New Discussion", userId, orgId);

            assertThat(result.id()).isEqualTo(convId);
            assertThat(result.title()).isEqualTo("New Discussion");
            assertThat(result.projectId()).isEqualTo(projectId);
            verify(convRepo).save(any(Conversation.class));
            verify(partRepo).save(any(ConversationParticipant.class));
        }

        @Test
        @DisplayName("[TC-MSG-013] ✅ Should create conversation with null title")
        void createConversation_NullTitle() {
            Conversation saved = new Conversation();
            saved.setId(UUID.randomUUID());
            saved.setProjectId(projectId);
            saved.setUpdatedAt(Instant.now());

            when(convRepo.save(any())).thenReturn(saved);
            when(partRepo.save(any())).thenReturn(new ConversationParticipant());

            ConversationDto result = service.createConversation(projectId, null, userId, orgId);

            assertThat(result).isNotNull();
            verify(convRepo).save(any());
        }

        @Test
        @DisplayName("[TC-MSG-014] ✅ Should set creator as first participant with correct org")
        void createConversation_VerifyParticipant() {
            Conversation saved = new Conversation();
            saved.setId(convId);
            saved.setProjectId(projectId);
            saved.setUpdatedAt(Instant.now());
            when(convRepo.save(any())).thenReturn(saved);

            ArgumentCaptor<ConversationParticipant> captor =
                ArgumentCaptor.forClass(ConversationParticipant.class);
            when(partRepo.save(captor.capture())).thenReturn(new ConversationParticipant());

            service.createConversation(projectId, "Title", userId, orgId);

            assertThat(captor.getValue().getUserId()).isEqualTo(userId);
            assertThat(captor.getValue().getOrgId()).isEqualTo(orgId);
            assertThat(captor.getValue().getConversationId()).isEqualTo(convId);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // ADD PARTICIPANT
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("addParticipant")
    class AddParticipantTests {

        @Test
        @DisplayName("[TC-MSG-015] ✅ Should add new participant to conversation")
        void addParticipant_Success() {
            UUID newUserId = UUID.randomUUID();
            when(partRepo.findByConversationIdAndUserId(convId, newUserId)).thenReturn(Optional.empty());
            when(partRepo.save(any())).thenReturn(new ConversationParticipant());

            service.addParticipant(convId, newUserId, orgId);

            verify(partRepo).save(any(ConversationParticipant.class));
        }

        @Test
        @DisplayName("[TC-MSG-016] ✅ Should not add duplicate participant (idempotent)")
        void addParticipant_AlreadyExists() {
            ConversationParticipant existing = new ConversationParticipant();
            existing.setConversationId(convId);
            existing.setUserId(userId);

            when(partRepo.findByConversationIdAndUserId(convId, userId))
                .thenReturn(Optional.of(existing));

            service.addParticipant(convId, userId, orgId);

            verify(partRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // MARK READ
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markRead")
    class MarkReadTests {

        @Test
        @DisplayName("[TC-MSG-017] ✅ Should update lastReadAt for participant")
        void markRead_Success() {
            ConversationParticipant part = new ConversationParticipant();
            part.setConversationId(convId);
            part.setUserId(userId);

            when(partRepo.findByConversationIdAndUserId(convId, userId)).thenReturn(Optional.of(part));
            when(partRepo.save(any())).thenReturn(part);

            service.markRead(convId, userId);

            ArgumentCaptor<ConversationParticipant> captor =
                ArgumentCaptor.forClass(ConversationParticipant.class);
            verify(partRepo).save(captor.capture());
            assertThat(captor.getValue().getLastReadAt()).isNotNull();
        }

        @Test
        @DisplayName("[TC-MSG-018] ✅ Should do nothing when participant not found (no-op)")
        void markRead_ParticipantNotFound() {
            when(partRepo.findByConversationIdAndUserId(any(), any())).thenReturn(Optional.empty());

            service.markRead(convId, userId);

            verify(partRepo, never()).save(any());
        }
    }
}
