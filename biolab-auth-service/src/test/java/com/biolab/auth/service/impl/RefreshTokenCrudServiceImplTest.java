package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.RefreshTokenInfoResponse;
import com.biolab.auth.entity.RefreshToken;
import com.biolab.auth.entity.User;
import com.biolab.auth.repository.RefreshTokenRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenCrudServiceImpl Unit Tests")
class RefreshTokenCrudServiceImplTest {

    @InjectMocks private RefreshTokenCrudServiceImpl service;
    @Mock private RefreshTokenRepository repo;
    private final UUID userId = UUID.randomUUID();

    private RefreshToken makeToken(UUID family, int gen) {
        User u = User.builder().email("u@b.com").passwordHash("h").firstName("A").lastName("B").build();
        u.setId(userId);
        RefreshToken t = RefreshToken.builder()
                .user(u).tokenHash("hash").tokenFamily(family).generation(gen)
                .isRevoked(false).expiresAt(Instant.now().plusSeconds(86400))
                .ipAddress("10.0.0.1").userAgent("Chrome").build();
        t.setId(UUID.randomUUID());
        t.setIssuedAt(Instant.now());
        return t;
    }

    @Nested @DisplayName("getActiveByUserId")
    class GetActiveTests {
        @Test @DisplayName("[TC-AUTH-096] ✅ Should return active (non-revoked) tokens for user")
        void getActive_Success() {
            UUID family = UUID.randomUUID();
            when(repo.findByUserIdAndIsRevokedFalse(userId)).thenReturn(List.of(makeToken(family, 0), makeToken(family, 1)));
            List<RefreshTokenInfoResponse> result = service.getActiveByUserId(userId);
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTokenFamily()).isEqualTo(family);
            assertThat(result.get(0).getIsRevoked()).isFalse();
        }

        @Test @DisplayName("[TC-AUTH-097] ✅ Should return empty when no active tokens")
        void getActive_Empty() {
            when(repo.findByUserIdAndIsRevokedFalse(userId)).thenReturn(List.of());
            assertThat(service.getActiveByUserId(userId)).isEmpty();
        }

        @Test @DisplayName("[TC-AUTH-098] ✅ Should map revokedReason as null for active tokens")
        void getActive_NullReason() {
            RefreshToken t = makeToken(UUID.randomUUID(), 0);
            when(repo.findByUserIdAndIsRevokedFalse(userId)).thenReturn(List.of(t));
            assertThat(service.getActiveByUserId(userId).get(0).getRevokedReason()).isNull();
        }
    }

    @Nested @DisplayName("revokeAllByUserId")
    class RevokeAllTests {
        @Test @DisplayName("[TC-AUTH-099] ✅ Should call repo to revoke all tokens")
        void revokeAll_Success() {
            service.revokeAllByUserId(userId);
            verify(repo).revokeAllByUserId(userId);
        }
    }
}
