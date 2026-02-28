package com.biolab.auth.security;

import com.biolab.auth.repository.LoginAuditLogRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginAnomalyDetector Unit Tests")
class LoginAnomalyDetectorTest {

    @InjectMocks private LoginAnomalyDetector detector;
    @Mock private LoginAuditLogRepository auditLogRepo;
    private final UUID userId = UUID.randomUUID();

    @Test @DisplayName("[TC-AUTH-136] ✅ Score 0 for known IP, no anomalies")
    void normal() {
        when(auditLogRepo.existsByUserIdAndIpAddressAndCreatedAtAfter(eq(userId), any(), any(Instant.class))).thenReturn(true);
        when(auditLogRepo.countByIpAddressAndStatusAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(auditLogRepo.countDistinctUserIdByIpAddressAndCreatedAtAfter(any(), any())).thenReturn(1L);
        assertThat(detector.calculateAnomalyScore(userId, "10.0.0.1", "Chrome")).isEqualTo(0);
    }

    @Test @DisplayName("[TC-AUTH-137] ✅ +2 for new IP")
    void newIp() {
        when(auditLogRepo.existsByUserIdAndIpAddressAndCreatedAtAfter(any(), any(), any())).thenReturn(false);
        when(auditLogRepo.countByIpAddressAndStatusAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(auditLogRepo.countDistinctUserIdByIpAddressAndCreatedAtAfter(any(), any())).thenReturn(1L);
        assertThat(detector.calculateAnomalyScore(userId, "99.99.99.99", "X")).isEqualTo(2);
    }

    @Test @DisplayName("[TC-AUTH-138] ✅ +2 for brute force (3+ failures)")
    void bruteForce() {
        when(auditLogRepo.existsByUserIdAndIpAddressAndCreatedAtAfter(any(), any(), any())).thenReturn(true);
        when(auditLogRepo.countByIpAddressAndStatusAndCreatedAtAfter(any(), eq("FAILURE"), any())).thenReturn(5L);
        when(auditLogRepo.countDistinctUserIdByIpAddressAndCreatedAtAfter(any(), any())).thenReturn(1L);
        assertThat(detector.calculateAnomalyScore(userId, "1.1.1.1", "X")).isEqualTo(2);
    }

    @Test @DisplayName("[TC-AUTH-139] ✅ +3 for credential stuffing (5+ users)")
    void credStuffing() {
        when(auditLogRepo.existsByUserIdAndIpAddressAndCreatedAtAfter(any(), any(), any())).thenReturn(true);
        when(auditLogRepo.countByIpAddressAndStatusAndCreatedAtAfter(any(), any(), any())).thenReturn(0L);
        when(auditLogRepo.countDistinctUserIdByIpAddressAndCreatedAtAfter(any(), any())).thenReturn(10L);
        assertThat(detector.calculateAnomalyScore(userId, "1.1.1.1", "X")).isEqualTo(3);
    }

    @Test @DisplayName("[TC-AUTH-140] ✅ Cumulative score: new IP + brute force + credential stuffing = 7")
    void cumulative() {
        when(auditLogRepo.existsByUserIdAndIpAddressAndCreatedAtAfter(any(), any(), any())).thenReturn(false);
        when(auditLogRepo.countByIpAddressAndStatusAndCreatedAtAfter(any(), any(), any())).thenReturn(10L);
        when(auditLogRepo.countDistinctUserIdByIpAddressAndCreatedAtAfter(any(), any())).thenReturn(8L);
        assertThat(detector.calculateAnomalyScore(userId, "bad-ip", "X")).isEqualTo(7);
    }

    @Test @DisplayName("[TC-AUTH-141] ✅ shouldBlock: false < 5, true >= 5")
    void block() { assertThat(detector.shouldBlock(4)).isFalse(); assertThat(detector.shouldBlock(5)).isTrue(); }

    @Test @DisplayName("[TC-AUTH-142] ✅ shouldRequireMfa: false < 3, true >= 3")
    void mfa() { assertThat(detector.shouldRequireMfa(2)).isFalse(); assertThat(detector.shouldRequireMfa(3)).isTrue(); }
}
