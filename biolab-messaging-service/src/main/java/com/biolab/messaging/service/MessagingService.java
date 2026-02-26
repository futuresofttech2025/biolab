package com.biolab.messaging.service;

import com.biolab.messaging.dto.*;
import com.biolab.messaging.entity.*;
import com.biolab.messaging.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MessagingService {

    private static final Logger log = LoggerFactory.getLogger(MessagingService.class);

    private final ConversationRepository convRepo;
    private final ConversationParticipantRepository partRepo;
    private final MessageRepository msgRepo;

    public MessagingService(ConversationRepository cr, ConversationParticipantRepository pr, MessageRepository mr) {
        this.convRepo = cr; this.partRepo = pr; this.msgRepo = mr;
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> listConversations(UUID userId) {
        log.debug("Listing conversations for user={}", userId);
        return convRepo.findByParticipant(userId).stream().map(c -> {
            Page<Message> lastPage = msgRepo.findByConversationIdOrderByCreatedAtDesc(c.getId(), PageRequest.of(0, 1));
            String lastMsg = lastPage.hasContent() ? lastPage.getContent().get(0).getContent() : "";
            String lastTime = lastPage.hasContent() ? lastPage.getContent().get(0).getCreatedAt().toString() : "";
            long unread = msgRepo.countByConversationIdAndIsReadFalseAndSenderIdNot(c.getId(), userId);
            return new ConversationDto(c.getId(), c.getProjectId(), c.getTitle(), c.getUpdatedAt(), lastMsg, lastTime, unread);
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<MessageDto> listMessages(UUID conversationId, Pageable pageable) {
        log.debug("Listing messages for conversation={}, page={}", conversationId, pageable.getPageNumber());
        return msgRepo.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable)
            .map(m -> new MessageDto(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getContent(), m.getAttachmentId(), m.getIsRead(), m.getCreatedAt()));
    }

    public MessageDto sendMessage(UUID conversationId, UUID senderId, SendMessageRequest req) {
        Conversation conv = convRepo.findById(conversationId)
            .orElseThrow(() -> new ResourceNotFoundException("Conversation", "id", conversationId));
        conv.setUpdatedAt(Instant.now());
        convRepo.save(conv);

        Message msg = new Message();
        msg.setConversationId(conversationId);
        msg.setSenderId(senderId);
        msg.setContent(req.content());
        msg.setAttachmentId(req.attachmentId());
        msg = msgRepo.save(msg);
        log.info("Message sent: conversation={}, sender={}, length={}", conversationId, senderId, req.content().length());
        return new MessageDto(msg.getId(), msg.getConversationId(), msg.getSenderId(),
            msg.getContent(), msg.getAttachmentId(), msg.getIsRead(), msg.getCreatedAt());
    }

    public ConversationDto createConversation(UUID projectId, String title, UUID creatorId, UUID creatorOrgId) {
        Conversation conv = new Conversation();
        conv.setProjectId(projectId); conv.setTitle(title);
        conv = convRepo.save(conv);

        ConversationParticipant p = new ConversationParticipant();
        p.setConversationId(conv.getId()); p.setUserId(creatorId); p.setOrgId(creatorOrgId);
        partRepo.save(p);

        log.info("Conversation created: id={}, title='{}', creator={}", conv.getId(), title, creatorId);
        return new ConversationDto(conv.getId(), conv.getProjectId(), conv.getTitle(), conv.getUpdatedAt(), "", "", 0);
    }

    public void addParticipant(UUID conversationId, UUID userId, UUID orgId) {
        if (partRepo.findByConversationIdAndUserId(conversationId, userId).isEmpty()) {
            ConversationParticipant p = new ConversationParticipant();
            p.setConversationId(conversationId); p.setUserId(userId); p.setOrgId(orgId);
            partRepo.save(p);
            log.info("Participant added: conversation={}, user={}", conversationId, userId);
        }
    }

    public void markRead(UUID conversationId, UUID userId) {
        partRepo.findByConversationIdAndUserId(conversationId, userId).ifPresent(p -> {
            p.setLastReadAt(Instant.now());
            partRepo.save(p);
            log.debug("Conversation marked read: conversation={}, user={}", conversationId, userId);
        });
    }
}
