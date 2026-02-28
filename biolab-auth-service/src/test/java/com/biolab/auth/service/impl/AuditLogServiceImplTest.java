package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.LoginAuditLogResponse;
import com.biolab.auth.dto.response.PageResponse;
import com.biolab.auth.entity.LoginAuditLog;
import com.biolab.auth.entity.User;
import com.biolab.auth.entity.enums.LoginAction;
import com.biolab.auth.entity.enums.LoginStatus;
import com.biolab.auth.repository.LoginAuditLogRepository;
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
@DisplayName("AuditLogServiceImpl Unit Tests")
class AuditLogServiceImplTest {

    @InjectMocks private AuditLogServiceImpl service;
    @Mock private LoginAuditLogRepository repo;

    private User testUser;
    private LoginAuditLog auditLog;
    private final UUID userId = UUID.randomUUID();
    private final String email = "test@biolab.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder().email(email).passwordHash("h").firstName("A").lastName("B").build();
        testUser.setId(userId);

        auditLog = LoginAuditLog.builder()
                .user(testUser).email(email).ipAddress("10.0.0.1").userAgent("Chrome")
                .action(LoginAction.LOGIN).status(LoginStatus.SUCCESS).mfaUsed(false).build();
        auditLog.setId(UUID.randomUUID());
        auditLog.setCreatedAt(Instant.now());
    }

    // ── getByUserId ──

    @Nested @DisplayName("getByUserId")
    class GetByUserIdTests {
        @Test @DisplayName("[TC-AUTH-066] ✅ Should return paginated audit logs for user")
        void getByUserId_Success() {
            when(repo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                    .thenReturn(new PageImpl<>(List.of(auditLog), PageRequest.of(0, 20), 1));
            PageResponse<LoginAuditLogResponse> result = service.getByUserId(userId, PageRequest.of(0, 20));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEmail()).isEqualTo(email);
            assertThat(result.getContent().get(0).getAction()).isEqualTo("LOGIN");
            assertThat(result.getContent().get(0).getStatus()).isEqualTo("SUCCESS");
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test @DisplayName("[TC-AUTH-067] ✅ Should return empty page for user with no audit logs")
        void getByUserId_Empty() {
            when(repo.findByUserIdOrderByCreatedAtDesc(any(), any())).thenReturn(Page.empty());
            PageResponse<LoginAuditLogResponse> result = service.getByUserId(UUID.randomUUID(), PageRequest.of(0, 20));
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }

        @Test @DisplayName("[TC-AUTH-068] ✅ Should map userId to null when user is null on log entry")
        void getByUserId_NullUser() {
            LoginAuditLog logNoUser = LoginAuditLog.builder()
                    .user(null).email("anon@test.com").ipAddress("1.1.1.1")
                    .action(LoginAction.FAILED_LOGIN).status(LoginStatus.FAILURE).mfaUsed(false).build();
            logNoUser.setId(UUID.randomUUID());
            logNoUser.setCreatedAt(Instant.now());

            when(repo.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                    .thenReturn(new PageImpl<>(List.of(logNoUser)));
            PageResponse<LoginAuditLogResponse> result = service.getByUserId(userId, PageRequest.of(0, 20));
            assertThat(result.getContent().get(0).getUserId()).isNull();
        }
    }

    // ── getByEmail ──

    @Nested @DisplayName("getByEmail")
    class GetByEmailTests {
        @Test @DisplayName("[TC-AUTH-069] ✅ Should return audit logs filtered by email")
        void getByEmail_Success() {
            when(repo.findByEmailOrderByCreatedAtDesc(eq(email), any()))
                    .thenReturn(new PageImpl<>(List.of(auditLog)));
            PageResponse<LoginAuditLogResponse> result = service.getByEmail(email, PageRequest.of(0, 20));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getIpAddress()).isEqualTo("10.0.0.1");
        }

        @Test @DisplayName("[TC-AUTH-070] ✅ Should return empty for non-existent email")
        void getByEmail_NoResults() {
            when(repo.findByEmailOrderByCreatedAtDesc(eq("nobody@test.com"), any()))
                    .thenReturn(Page.empty());
            assertThat(service.getByEmail("nobody@test.com", PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }

    // ── getAll ──

    @Nested @DisplayName("getAll")
    class GetAllTests {
        @Test @DisplayName("[TC-AUTH-071] ✅ Should return all audit logs paginated")
        void getAll_Success() {
            when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(auditLog)));
            PageResponse<LoginAuditLogResponse> result = service.getAll(PageRequest.of(0, 20));
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.isHasNext()).isFalse();
        }

        @Test @DisplayName("[TC-AUTH-072] ✅ Should return page metadata correctly")
        void getAll_PageMetadata() {
            Page<LoginAuditLog> page = new PageImpl<>(List.of(auditLog), PageRequest.of(0, 10), 25);
            when(repo.findAll(any(Pageable.class))).thenReturn(page);
            PageResponse<LoginAuditLogResponse> result = service.getAll(PageRequest.of(0, 10));
            assertThat(result.getPage()).isEqualTo(0);
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(25);
            assertThat(result.getTotalPages()).isEqualTo(3);
            assertThat(result.isHasNext()).isTrue();
        }

        @Test @DisplayName("[TC-AUTH-073] ✅ Should return empty when no logs exist")
        void getAll_Empty() {
            when(repo.findAll(any(Pageable.class))).thenReturn(Page.empty());
            assertThat(service.getAll(PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }

    // ── Response mapping ──

    @Nested @DisplayName("Response Mapping")
    class MappingTests {
        @Test @DisplayName("[TC-AUTH-074] ✅ Should map MFA used flag correctly")
        void mapping_MfaUsed() {
            auditLog = LoginAuditLog.builder()
                    .user(testUser).email(email).ipAddress("10.0.0.1")
                    .action(LoginAction.MFA_CHALLENGE).status(LoginStatus.SUCCESS).mfaUsed(true).build();
            auditLog.setId(UUID.randomUUID());
            auditLog.setCreatedAt(Instant.now());

            when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(auditLog)));
            PageResponse<LoginAuditLogResponse> result = service.getAll(PageRequest.of(0, 10));
            assertThat(result.getContent().get(0).getMfaUsed()).isTrue();
            assertThat(result.getContent().get(0).getAction()).isEqualTo("MFA_CHALLENGE");
        }

        @Test @DisplayName("[TC-AUTH-075] ✅ Should map failure reason for failed logins")
        void mapping_FailureReason() {
            auditLog = LoginAuditLog.builder()
                    .user(testUser).email(email).ipAddress("10.0.0.1")
                    .action(LoginAction.FAILED_LOGIN).status(LoginStatus.FAILURE)
                    .mfaUsed(false).failureReason("Invalid credentials").build();
            auditLog.setId(UUID.randomUUID());
            auditLog.setCreatedAt(Instant.now());

            when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(auditLog)));
            PageResponse<LoginAuditLogResponse> result = service.getAll(PageRequest.of(0, 10));
            assertThat(result.getContent().get(0).getFailureReason()).isEqualTo("Invalid credentials");
        }
    }
}
