package com.biolab.auth.security;

import com.biolab.auth.entity.UserSession;
import com.biolab.auth.repository.UserSessionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConcurrentSessionManager Unit Tests")
class ConcurrentSessionManagerTest {

    @InjectMocks private ConcurrentSessionManager manager;
    @Mock private UserSessionRepository sessionRepo;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach void setUp() { ReflectionTestUtils.setField(manager, "maxConcurrentSessions", 5); }

    private UserSession makeSession(int minutesAgo) {
        UserSession s = UserSession.builder().isActive(true).ipAddress("10.0.0.1")
                .expiresAt(Instant.now().plusSeconds(3600)).lastAccessedAt(Instant.now()).build();
        s.setId(UUID.randomUUID()); s.setCreatedAt(Instant.now().minusSeconds(minutesAgo * 60L));
        return s;
    }

    @Test @DisplayName("[TC-AUTH-130] ✅ No termination when under limit")
    void underLimit() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)).thenReturn(List.of(makeSession(1)));
        manager.enforceSessionLimit(userId);
        verify(sessionRepo, never()).save(any());
    }

    @Test @DisplayName("[TC-AUTH-131] ✅ Terminates oldest when at limit")
    void atLimit() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>(IntStream.range(0, 5).mapToObj(this::makeSession).toList()));
        manager.enforceSessionLimit(userId);
        verify(sessionRepo, times(1)).save(any(UserSession.class));
    }

    @Test @DisplayName("[TC-AUTH-132] ✅ Terminates multiple when over limit")
    void overLimit() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId))
                .thenReturn(new ArrayList<>(IntStream.range(0, 7).mapToObj(this::makeSession).toList()));
        manager.enforceSessionLimit(userId);
        verify(sessionRepo, times(3)).save(any(UserSession.class));
    }

    @Test @DisplayName("[TC-AUTH-133] ✅ No sessions, no action")
    void noSessions() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        manager.enforceSessionLimit(userId);
        verify(sessionRepo, never()).save(any());
    }

    @Test @DisplayName("[TC-AUTH-134] ✅ Active session count")
    void count() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(makeSession(1), makeSession(2)));
        assertThat(manager.getActiveSessionCount(userId)).isEqualTo(2);
    }

    @Test @DisplayName("[TC-AUTH-135] ✅ Zero active sessions")
    void countZero() {
        when(sessionRepo.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId)).thenReturn(List.of());
        assertThat(manager.getActiveSessionCount(userId)).isEqualTo(0);
    }
}
