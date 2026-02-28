package com.biolab.notification.service;

import com.biolab.notification.dto.*;
import com.biolab.notification.entity.*;
import com.biolab.notification.repository.*;
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
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @InjectMocks private NotificationService service;
    @Mock private NotificationRepository notifRepo;
    @Mock private NotificationPreferenceRepository prefRepo;

    private final UUID userId = UUID.randomUUID();
    private final UUID notifId = UUID.randomUUID();
    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setUserId(userId);
        notification.setType("PROJECT_UPDATE");
        notification.setTitle("Project Status Changed");
        notification.setMessage("Your project moved to IN_PROGRESS");
        notification.setLink("/projects/123");
        notification.setIsRead(false);
        // Use reflection or direct set since getId is on the entity
        try {
            var idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(notification, notifId);
            var createdField = Notification.class.getDeclaredField("createdAt");
            createdField.setAccessible(true);
            createdField.set(notification, Instant.now());
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ══════════════════════════════════════════════════════════════════
    // LIST NOTIFICATIONS
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("list")
    class ListTests {

        @Test
        @DisplayName("[TC-NTF-001] ✅ Should list notifications with pagination")
        void list_Success() {
            Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 20), 1);
            when(notifRepo.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, 20))).thenReturn(page);

            Page<NotificationDto> result = service.list(userId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).title()).isEqualTo("Project Status Changed");
            assertThat(result.getContent().get(0).type()).isEqualTo("PROJECT_UPDATE");
            assertThat(result.getContent().get(0).isRead()).isFalse();
        }

        @Test
        @DisplayName("[TC-NTF-002] ✅ Should return empty page when no notifications")
        void list_Empty() {
            when(notifRepo.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(Page.empty());

            Page<NotificationDto> result = service.list(userId, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test
        @DisplayName("[TC-NTF-003] ✅ Should correctly map all fields to DTO")
        void list_MapsAllFields() {
            Page<Notification> page = new PageImpl<>(List.of(notification));
            when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any())).thenReturn(page);

            Page<NotificationDto> result = service.list(userId, PageRequest.of(0, 10));

            NotificationDto dto = result.getContent().get(0);
            assertThat(dto.id()).isEqualTo(notifId);
            assertThat(dto.message()).isEqualTo("Your project moved to IN_PROGRESS");
            assertThat(dto.link()).isEqualTo("/projects/123");
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UNREAD COUNT
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("unreadCount")
    class UnreadCountTests {

        @Test
        @DisplayName("[TC-NTF-004] ✅ Should return correct unread count")
        void unreadCount_HasUnread() {
            when(notifRepo.countByUserIdAndIsReadFalse(userId)).thenReturn(5L);

            long count = service.unreadCount(userId);

            assertThat(count).isEqualTo(5);
        }

        @Test
        @DisplayName("[TC-NTF-005] ✅ Should return zero when all notifications are read")
        void unreadCount_AllRead() {
            when(notifRepo.countByUserIdAndIsReadFalse(userId)).thenReturn(0L);

            long count = service.unreadCount(userId);

            assertThat(count).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // CREATE NOTIFICATION
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("create")
    class CreateTests {

        @Test
        @DisplayName("[TC-NTF-006] ✅ Should create notification with all fields")
        void create_Success() {
            CreateNotificationRequest req = new CreateNotificationRequest(
                userId, "INVOICE", "Invoice Ready", "Invoice INV-42 is ready for review", "/invoices/42"
            );

            Notification saved = new Notification();
            saved.setUserId(userId);
            saved.setType("INVOICE");
            saved.setTitle("Invoice Ready");
            saved.setMessage("Invoice INV-42 is ready for review");
            saved.setLink("/invoices/42");
            saved.setIsRead(false);
            try {
                var idField = Notification.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, UUID.randomUUID());
                var createdField = Notification.class.getDeclaredField("createdAt");
                createdField.setAccessible(true);
                createdField.set(saved, Instant.now());
            } catch (Exception e) { throw new RuntimeException(e); }

            when(notifRepo.save(any())).thenReturn(saved);

            NotificationDto result = service.create(req);

            assertThat(result.type()).isEqualTo("INVOICE");
            assertThat(result.title()).isEqualTo("Invoice Ready");
            assertThat(result.isRead()).isFalse();
            verify(notifRepo).save(any(Notification.class));
        }

        @Test
        @DisplayName("[TC-NTF-007] ✅ Should create notification with null message and link")
        void create_MinimalFields() {
            CreateNotificationRequest req = new CreateNotificationRequest(
                userId, "SYSTEM", "Maintenance", null, null
            );

            Notification saved = new Notification();
            saved.setUserId(userId);
            saved.setType("SYSTEM");
            saved.setTitle("Maintenance");
            saved.setIsRead(false);
            try {
                var idField = Notification.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, UUID.randomUUID());
                var createdField = Notification.class.getDeclaredField("createdAt");
                createdField.setAccessible(true);
                createdField.set(saved, Instant.now());
            } catch (Exception e) { throw new RuntimeException(e); }

            when(notifRepo.save(any())).thenReturn(saved);

            NotificationDto result = service.create(req);

            assertThat(result.type()).isEqualTo("SYSTEM");
            assertThat(result.message()).isNull();
            assertThat(result.link()).isNull();
        }

        @Test
        @DisplayName("[TC-NTF-008] ✅ Should set default isRead to false on creation")
        void create_DefaultsUnread() {
            CreateNotificationRequest req = new CreateNotificationRequest(userId, "T", "T", null, null);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            Notification saved = new Notification();
            saved.setIsRead(false);
            try {
                var idField = Notification.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(saved, UUID.randomUUID());
                var createdField = Notification.class.getDeclaredField("createdAt");
                createdField.setAccessible(true);
                createdField.set(saved, Instant.now());
            } catch (Exception e) { throw new RuntimeException(e); }
            when(notifRepo.save(captor.capture())).thenReturn(saved);

            service.create(req);

            assertThat(captor.getValue().getIsRead()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // MARK READ
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markRead")
    class MarkReadTests {

        @Test
        @DisplayName("[TC-NTF-009] ✅ Should mark notification as read")
        void markRead_Success() {
            when(notifRepo.findById(notifId)).thenReturn(Optional.of(notification));
            when(notifRepo.save(any())).thenReturn(notification);

            service.markRead(notifId);

            ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
            verify(notifRepo).save(captor.capture());
            assertThat(captor.getValue().getIsRead()).isTrue();
        }

        @Test
        @DisplayName("[TC-NTF-010] ✅ Should do nothing when notification not found (no-op)")
        void markRead_NotFound() {
            when(notifRepo.findById(any())).thenReturn(Optional.empty());

            service.markRead(UUID.randomUUID());

            verify(notifRepo, never()).save(any());
        }

        @Test
        @DisplayName("[TC-NTF-011] ✅ Should be idempotent when already read")
        void markRead_AlreadyRead() {
            notification.setIsRead(true);
            when(notifRepo.findById(notifId)).thenReturn(Optional.of(notification));
            when(notifRepo.save(any())).thenReturn(notification);

            service.markRead(notifId);

            verify(notifRepo).save(any()); // Still called, just sets true again
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // MARK ALL READ
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("markAllRead")
    class MarkAllReadTests {

        @Test
        @DisplayName("[TC-NTF-012] ✅ Should mark all unread notifications as read")
        void markAllRead_Success() {
            Notification n1 = new Notification();
            n1.setIsRead(false);
            Notification n2 = new Notification();
            n2.setIsRead(false);
            Notification n3 = new Notification();
            n3.setIsRead(true); // Already read

            when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(n1, n2, n3)));

            service.markAllRead(userId);

            verify(notifRepo, times(2)).save(any(Notification.class)); // Only n1 and n2
        }

        @Test
        @DisplayName("[TC-NTF-013] ✅ Should handle empty notification list")
        void markAllRead_NoNotifications() {
            when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(Page.empty());

            service.markAllRead(userId);

            verify(notifRepo, never()).save(any());
        }

        @Test
        @DisplayName("[TC-NTF-014] ✅ Should not save already-read notifications")
        void markAllRead_AllAlreadyRead() {
            Notification n1 = new Notification();
            n1.setIsRead(true);

            when(notifRepo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                .thenReturn(new PageImpl<>(List.of(n1)));

            service.markAllRead(userId);

            verify(notifRepo, never()).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // GET PREFERENCES
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getPreferences")
    class GetPreferencesTests {

        @Test
        @DisplayName("[TC-NTF-015] ✅ Should return existing preferences")
        void getPreferences_Existing() {
            NotificationPreference pref = new NotificationPreference();
            pref.setUserId(userId);
            pref.setEmailEnabled(true);
            pref.setProjectUpdates(true);
            pref.setNewMessages(false);
            pref.setInvoiceReminders(true);
            pref.setSecurityAlerts(true);
            pref.setMarketing(false);

            when(prefRepo.findByUserId(userId)).thenReturn(Optional.of(pref));

            NotificationPreferenceDto result = service.getPreferences(userId);

            assertThat(result.emailEnabled()).isTrue();
            assertThat(result.newMessages()).isFalse();
            assertThat(result.marketing()).isFalse();
        }

        @Test
        @DisplayName("[TC-NTF-016] ✅ Should create default preferences when none exist")
        void getPreferences_CreatesDefault() {
            NotificationPreference defaultPref = new NotificationPreference();
            defaultPref.setUserId(userId);

            when(prefRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(prefRepo.save(any())).thenReturn(defaultPref);

            NotificationPreferenceDto result = service.getPreferences(userId);

            assertThat(result).isNotNull();
            assertThat(result.emailEnabled()).isTrue(); // default
            assertThat(result.marketing()).isFalse(); // default
            verify(prefRepo).save(any(NotificationPreference.class));
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // UPDATE PREFERENCES
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePreferences")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("[TC-NTF-017] ✅ Should update all preference fields")
        void updatePreferences_Success() {
            NotificationPreference existing = new NotificationPreference();
            existing.setUserId(userId);

            when(prefRepo.findByUserId(userId)).thenReturn(Optional.of(existing));
            when(prefRepo.save(any())).thenReturn(existing);

            NotificationPreferenceDto dto = new NotificationPreferenceDto(
                false, true, true, false, true, true
            );

            NotificationPreferenceDto result = service.updatePreferences(userId, dto);

            assertThat(result.emailEnabled()).isFalse();
            assertThat(result.marketing()).isTrue();

            ArgumentCaptor<NotificationPreference> captor =
                ArgumentCaptor.forClass(NotificationPreference.class);
            verify(prefRepo).save(captor.capture());
            assertThat(captor.getValue().getEmailEnabled()).isFalse();
            assertThat(captor.getValue().getMarketing()).isTrue();
        }

        @Test
        @DisplayName("[TC-NTF-018] ✅ Should create new preference record if none exists")
        void updatePreferences_CreatesNew() {
            when(prefRepo.findByUserId(userId)).thenReturn(Optional.empty());
            when(prefRepo.save(any())).thenReturn(new NotificationPreference());

            NotificationPreferenceDto dto = new NotificationPreferenceDto(
                true, true, true, true, true, false
            );

            service.updatePreferences(userId, dto);

            verify(prefRepo).save(any(NotificationPreference.class));
        }
    }
}
