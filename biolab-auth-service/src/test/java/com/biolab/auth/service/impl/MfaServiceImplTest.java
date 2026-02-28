package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.entity.MfaSettings;
import com.biolab.auth.entity.enums.MfaType;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.MfaSettingsRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MfaServiceImpl Unit Tests")
class MfaServiceImplTest {

    @InjectMocks private MfaServiceImpl service;
    @Mock private MfaSettingsRepository repo;
    private final UUID userId = UUID.randomUUID();

    private MfaSettings makeMfa(MfaType type, boolean enabled) {
        MfaSettings m = MfaSettings.builder().mfaType(type).isEnabled(enabled).build();
        m.setId(UUID.randomUUID());
        if (enabled) m.setVerifiedAt(Instant.now());
        return m;
    }

    @Nested @DisplayName("getByUserId")
    class GetByUserIdTests {
        @Test @DisplayName("[TC-AUTH-085] ✅ Should return MFA settings for user with TOTP enabled")
        void getByUserId_WithTotp() {
            when(repo.findByUserId(userId)).thenReturn(List.of(makeMfa(MfaType.TOTP, true)));
            List<MfaSettingsResponse> result = service.getByUserId(userId);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMfaType()).isEqualTo("TOTP");
            assertThat(result.get(0).getIsEnabled()).isTrue();
            assertThat(result.get(0).getVerifiedAt()).isNotNull();
        }

        @Test @DisplayName("[TC-AUTH-086] ✅ Should return multiple MFA methods (TOTP + EMAIL)")
        void getByUserId_Multiple() {
            when(repo.findByUserId(userId)).thenReturn(List.of(makeMfa(MfaType.TOTP, true), makeMfa(MfaType.EMAIL, false)));
            List<MfaSettingsResponse> result = service.getByUserId(userId);
            assertThat(result).hasSize(2);
        }

        @Test @DisplayName("[TC-AUTH-087] ✅ Should return empty list for user with no MFA")
        void getByUserId_Empty() {
            when(repo.findByUserId(userId)).thenReturn(List.of());
            assertThat(service.getByUserId(userId)).isEmpty();
        }

        @Test @DisplayName("[TC-AUTH-088] ✅ Should map verifiedAt as null for unverified MFA")
        void getByUserId_Unverified() {
            MfaSettings m = makeMfa(MfaType.EMAIL, false);
            m.setVerifiedAt(null);
            when(repo.findByUserId(userId)).thenReturn(List.of(m));
            assertThat(service.getByUserId(userId).get(0).getVerifiedAt()).isNull();
        }
    }

    @Nested @DisplayName("disable")
    class DisableTests {
        @Test @DisplayName("[TC-AUTH-089] ✅ Should disable TOTP MFA for user")
        void disable_Success() {
            MfaSettings m = makeMfa(MfaType.TOTP, true);
            when(repo.findByUserIdAndMfaType(userId, MfaType.TOTP)).thenReturn(Optional.of(m));
            when(repo.save(any())).thenReturn(m);

            service.disable(userId, "TOTP");

            ArgumentCaptor<MfaSettings> cap = ArgumentCaptor.forClass(MfaSettings.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getIsEnabled()).isFalse();
        }

        @Test @DisplayName("[TC-AUTH-090] ❌ Should throw when MFA type not found for user")
        void disable_NotFound() {
            when(repo.findByUserIdAndMfaType(any(), any())).thenReturn(Optional.empty());
            assertThatThrownBy(() -> service.disable(userId, "TOTP"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test @DisplayName("[TC-AUTH-091] ❌ Should throw on invalid MFA type string")
        void disable_InvalidType() {
            assertThatThrownBy(() -> service.disable(userId, "INVALID"))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test @DisplayName("[TC-AUTH-092] ✅ Should be idempotent when already disabled")
        void disable_AlreadyDisabled() {
            MfaSettings m = makeMfa(MfaType.EMAIL, false);
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(m));
            when(repo.save(any())).thenReturn(m);

            service.disable(userId, "EMAIL");
            verify(repo).save(any());
        }
    }
}
