package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.JwtTokenBlacklistResponse;
import com.biolab.auth.entity.JwtTokenBlacklist;
import com.biolab.auth.entity.enums.TokenType;
import com.biolab.auth.repository.JwtTokenBlacklistRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenBlacklistServiceImpl Unit Tests")
class TokenBlacklistServiceImplTest {

    @InjectMocks private TokenBlacklistServiceImpl service;
    @Mock private JwtTokenBlacklistRepository repo;
    private final UUID userId = UUID.randomUUID();

    @Nested @DisplayName("isBlacklisted")
    class IsBlacklistedTests {
        @Test @DisplayName("[TC-AUTH-110] ✅ Should return true for blacklisted JTI")
        void isBlacklisted_True() {
            when(repo.existsByJti("jti-blacklisted")).thenReturn(true);
            assertThat(service.isBlacklisted("jti-blacklisted")).isTrue();
        }

        @Test @DisplayName("[TC-AUTH-111] ✅ Should return false for non-blacklisted JTI")
        void isBlacklisted_False() {
            when(repo.existsByJti("jti-clean")).thenReturn(false);
            assertThat(service.isBlacklisted("jti-clean")).isFalse();
        }
    }

    @Nested @DisplayName("getByUserId")
    class GetByUserIdTests {
        @Test @DisplayName("[TC-AUTH-112] ✅ Should return blacklisted tokens for user")
        void getByUserId_Success() {
            JwtTokenBlacklist entry = JwtTokenBlacklist.builder()
                    .jti("jti-1").tokenType(TokenType.ACCESS).expiresAt(Instant.now().plusSeconds(3600))
                    .reason("User logout").build();
            entry.setId(UUID.randomUUID());
            entry.setBlacklistedAt(Instant.now());

            when(repo.findByUserId(userId)).thenReturn(List.of(entry));

            List<JwtTokenBlacklistResponse> result = service.getByUserId(userId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getJti()).isEqualTo("jti-1");
            assertThat(result.get(0).getTokenType()).isEqualTo("ACCESS");
            assertThat(result.get(0).getReason()).isEqualTo("User logout");
        }

        @Test @DisplayName("[TC-AUTH-113] ✅ Should return empty list when user has no blacklisted tokens")
        void getByUserId_Empty() {
            when(repo.findByUserId(userId)).thenReturn(List.of());
            assertThat(service.getByUserId(userId)).isEmpty();
        }

        @Test @DisplayName("[TC-AUTH-114] ✅ Should map REFRESH token type correctly")
        void getByUserId_RefreshType() {
            JwtTokenBlacklist entry = JwtTokenBlacklist.builder()
                    .jti("jti-r1").tokenType(TokenType.REFRESH).expiresAt(Instant.now().plusSeconds(86400))
                    .reason("Forced logout").build();
            entry.setId(UUID.randomUUID());
            entry.setBlacklistedAt(Instant.now());

            when(repo.findByUserId(userId)).thenReturn(List.of(entry));
            assertThat(service.getByUserId(userId).get(0).getTokenType()).isEqualTo("REFRESH");
        }
    }
}
