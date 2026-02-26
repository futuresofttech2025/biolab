package com.biolab.notification.service;

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
 * Unit tests for NotificationService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService service;


    @Mock private com.biolab.notification.repository.NotificationRepository notifRepo;
    @Mock private com.biolab.notification.repository.NotificationPreferenceRepository prefRepo;

    @Test
    @DisplayName("list returns paginated notifications")
    void list_returnsPaginated() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
            .thenReturn(Page.empty());
        Page<?> result = service.list(userId, PageRequest.of(0, 10));
        assertNotNull(result);
    }

    @Test
    @DisplayName("unreadCount returns count")
    void unreadCount_returns() {
        java.util.UUID userId = java.util.UUID.randomUUID();
        when(notifRepo.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);
        assertEquals(5L, service.unreadCount(userId));
    }

}
