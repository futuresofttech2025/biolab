package com.biolab.notification.service;

import com.biolab.notification.dto.*;
import com.biolab.notification.entity.*;
import com.biolab.notification.repository.*;
import com.biolab.common.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifRepo;
    private final NotificationPreferenceRepository prefRepo;

    public NotificationService(NotificationRepository nr, NotificationPreferenceRepository pr) {
        this.notifRepo = nr; this.prefRepo = pr;
    }

    @Transactional(readOnly = true)
    public Page<NotificationDto> list(UUID userId, Pageable pageable) {
        log.debug("Listing notifications for user={}, page={}", userId, pageable.getPageNumber());
        return notifRepo.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(n -> new NotificationDto(n.getId(), n.getType(), n.getTitle(),
                n.getMessage(), n.getLink(), n.getIsRead(), n.getCreatedAt()));
    }

    @Transactional(readOnly = true)
    public long unreadCount(UUID userId) {
        long count = notifRepo.countByUserIdAndIsReadFalse(userId);
        log.debug("Unread count for user={}: {}", userId, count);
        return count;
    }

    public NotificationDto create(CreateNotificationRequest req) {
        Notification n = new Notification();
        n.setUserId(req.userId()); n.setType(req.type());
        n.setTitle(req.title()); n.setMessage(req.message()); n.setLink(req.link());
        n = notifRepo.save(n);
        log.info("Notification created: user={}, type={}, title='{}'", req.userId(), req.type(), req.title());
        return new NotificationDto(n.getId(), n.getType(), n.getTitle(),
            n.getMessage(), n.getLink(), n.getIsRead(), n.getCreatedAt());
    }

    public void markRead(UUID id) {
        notifRepo.findById(id).ifPresent(n -> {
            n.setIsRead(true); notifRepo.save(n);
            log.debug("Notification marked read: id={}", id);
        });
    }

    public void markAllRead(UUID userId) {
        notifRepo.findByUserIdOrderByCreatedAtDesc(userId, Pageable.unpaged())
            .forEach(n -> { if (!n.getIsRead()) { n.setIsRead(true); notifRepo.save(n); } });
        log.info("All notifications marked read for user={}", userId);
    }

    @Transactional(readOnly = true)
    public NotificationPreferenceDto getPreferences(UUID userId) {
        log.debug("Fetching notification preferences for user={}", userId);
        NotificationPreference p = prefRepo.findByUserId(userId)
            .orElseGet(() -> { NotificationPreference np = new NotificationPreference(); np.setUserId(userId); return prefRepo.save(np); });
        return new NotificationPreferenceDto(p.getEmailEnabled(), p.getProjectUpdates(),
            p.getNewMessages(), p.getInvoiceReminders(), p.getSecurityAlerts(), p.getMarketing());
    }

    public NotificationPreferenceDto updatePreferences(UUID userId, NotificationPreferenceDto dto) {
        NotificationPreference p = prefRepo.findByUserId(userId)
            .orElseGet(() -> { NotificationPreference np = new NotificationPreference(); np.setUserId(userId); return np; });
        p.setEmailEnabled(dto.emailEnabled()); p.setProjectUpdates(dto.projectUpdates());
        p.setNewMessages(dto.newMessages()); p.setInvoiceReminders(dto.invoiceReminders());
        p.setSecurityAlerts(dto.securityAlerts()); p.setMarketing(dto.marketing());
        prefRepo.save(p);
        log.info("Notification preferences updated for user={}", userId);
        return dto;
    }
}
