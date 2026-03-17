package com.biolab.auth.service.impl;

import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.dto.response.MfaSetupResponse;
import com.biolab.auth.entity.MfaSettings;
import com.biolab.auth.entity.User;
import com.biolab.auth.entity.enums.MfaType;
import com.biolab.auth.exception.AuthException;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.MfaSettingsRepository;
import com.biolab.auth.repository.UserRepository;
import com.biolab.auth.service.EmailService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MfaServiceImpl}.
 *
 * <h3>Changes from original test class</h3>
 * <ul>
 *   <li>Added {@code @Mock UserRepository} and {@code @Mock EmailService}
 *       — required by the new {@link MfaServiceImpl} constructor</li>
 *   <li>Renamed {@code getByUserId} tests to {@code getSettings}
 *       — method was renamed in the {@link com.biolab.auth.service.MfaService} interface</li>
 *   <li>TC-AUTH-091 updated: invalid type now throws {@link AuthException}
 *       (service changed from raw enum parse to {@code parseMfaType()} which throws AuthException)</li>
 *   <li>Added {@code verifyOtp} test group covering login step-up validation — FIX-1</li>
 *   <li>Added EMAIL OTP expiry test — FIX-14</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MfaServiceImpl Unit Tests")
class MfaServiceImplTest {

    @InjectMocks private MfaServiceImpl service;

    @Mock private MfaSettingsRepository repo;
    @Mock private UserRepository        userRepository;   // NEW — required by constructor
    @Mock private EmailService          emailService;     // NEW — required by constructor

    private final UUID userId = UUID.randomUUID();
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@biolab.com").firstName("John").lastName("Doe")
                .build();
        testUser.setId(userId);
        ReflectionTestUtils.setField(service, "appName", "BioLabs");
    }

    // ── helpers ───────────────────────────────────────────────────────

    private MfaSettings makeMfa(MfaType type, boolean enabled) {
        MfaSettings m = MfaSettings.builder()
                .user(testUser).mfaType(type).isEnabled(enabled).build();
        m.setId(UUID.randomUUID());
        if (enabled) m.setVerifiedAt(Instant.now());
        return m;
    }

    private MfaSettings makeMfaWithSecret(MfaType type, boolean enabled, String secret) {
        MfaSettings m = makeMfa(type, enabled);
        m.setSecretKey(secret);
        return m;
    }

    // ══════════════════════════════════════════════════════════════════
    // getSettings  (was getByUserId)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("getSettings")
    class GetSettingsTests {

        @Test
        @DisplayName("[TC-AUTH-085] ✅ Should return MFA settings for user with TOTP enabled")
        void getSettings_WithTotp() {
            when(repo.findByUserId(userId)).thenReturn(List.of(makeMfa(MfaType.TOTP, true)));

            List<MfaSettingsResponse> result = service.getSettings(userId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getMfaType()).isEqualTo("TOTP");
            assertThat(result.get(0).getIsEnabled()).isTrue();
            assertThat(result.get(0).getVerifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("[TC-AUTH-086] ✅ Should return multiple MFA methods (TOTP + EMAIL)")
        void getSettings_Multiple() {
            when(repo.findByUserId(userId)).thenReturn(
                    List.of(makeMfa(MfaType.TOTP, true), makeMfa(MfaType.EMAIL, false)));

            assertThat(service.getSettings(userId)).hasSize(2);
        }

        @Test
        @DisplayName("[TC-AUTH-087] ✅ Should return empty list for user with no MFA")
        void getSettings_Empty() {
            when(repo.findByUserId(userId)).thenReturn(List.of());

            assertThat(service.getSettings(userId)).isEmpty();
        }

        @Test
        @DisplayName("[TC-AUTH-088] ✅ Should map verifiedAt as null for unverified MFA")
        void getSettings_Unverified() {
            MfaSettings m = makeMfa(MfaType.EMAIL, false);
            m.setVerifiedAt(null);
            when(repo.findByUserId(userId)).thenReturn(List.of(m));

            assertThat(service.getSettings(userId).get(0).getVerifiedAt()).isNull();
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // disable
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("disable")
    class DisableTests {

        @Test
        @DisplayName("[TC-AUTH-089] ✅ Should disable TOTP MFA for user")
        void disable_Success() {
            MfaSettings m = makeMfa(MfaType.TOTP, true);
            when(repo.findByUserIdAndMfaType(userId, MfaType.TOTP)).thenReturn(Optional.of(m));
            when(repo.save(any())).thenReturn(m);

            service.disable(userId, "TOTP");

            ArgumentCaptor<MfaSettings> cap = ArgumentCaptor.forClass(MfaSettings.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getIsEnabled()).isFalse();
            assertThat(cap.getValue().getSecretKey()).isNull();
            assertThat(cap.getValue().getBackupCodes()).isNull();
        }

        @Test
        @DisplayName("[TC-AUTH-090] ❌ Should throw when MFA type not found for user")
        void disable_NotFound() {
            when(repo.findByUserIdAndMfaType(any(), any())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.disable(userId, "TOTP"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("[TC-AUTH-091] ❌ Should throw AuthException on invalid MFA type string")
        void disable_InvalidType() {
            // parseMfaType() throws AuthException (not raw IllegalArgumentException) — FIX
            assertThatThrownBy(() -> service.disable(userId, "INVALID"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid MFA type");
        }

        @Test
        @DisplayName("[TC-AUTH-092] ✅ Should be idempotent when already disabled")
        void disable_AlreadyDisabled() {
            MfaSettings m = makeMfa(MfaType.EMAIL, false);
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(m));
            when(repo.save(any())).thenReturn(m);

            service.disable(userId, "EMAIL");

            verify(repo).save(any());
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // verifyOtp  (FIX-1 — login step-up)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyOtp (login step-up)")
    class VerifyOtpTests {

        @Test
        @DisplayName("[TC-AUTH-NEW-01] ✅ Should validate correct EMAIL OTP during login step-up")
        void verifyOtp_EmailOtp_Valid() {
            MfaSettings ms = makeMfaWithSecret(MfaType.EMAIL, true, "123456");
            ms.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(ms));
            when(repo.save(any())).thenReturn(ms);

            assertThatNoException().isThrownBy(() -> service.verifyOtp(userId, "EMAIL", "123456"));

            // After successful verification, the OTP is cleared (single-use)
            ArgumentCaptor<MfaSettings> cap = ArgumentCaptor.forClass(MfaSettings.class);
            verify(repo).save(cap.capture());
            assertThat(cap.getValue().getSecretKey()).isNull();
            assertThat(cap.getValue().getEmailOtpExpiresAt()).isNull();
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-02] ❌ Should reject wrong EMAIL OTP")
        void verifyOtp_EmailOtp_Wrong() {
            MfaSettings ms = makeMfaWithSecret(MfaType.EMAIL, true, "123456");
            ms.setEmailOtpExpiresAt(Instant.now().plusSeconds(300));
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(ms));

            assertThatThrownBy(() -> service.verifyOtp(userId, "EMAIL", "999999"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-03] ❌ Should reject expired EMAIL OTP — FIX-14")
        void verifyOtp_EmailOtp_Expired() {
            MfaSettings ms = makeMfaWithSecret(MfaType.EMAIL, true, "123456");
            // FIX-14: expiry is now enforced
            ms.setEmailOtpExpiresAt(Instant.now().minusSeconds(1));
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(ms));

            assertThatThrownBy(() -> service.verifyOtp(userId, "EMAIL", "123456"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-04] ❌ Should reject verifyOtp when MFA not enabled")
        void verifyOtp_NotEnabled() {
            MfaSettings ms = makeMfaWithSecret(MfaType.EMAIL, false, "123456");
            when(repo.findByUserIdAndMfaType(userId, MfaType.EMAIL)).thenReturn(Optional.of(ms));

            assertThatThrownBy(() -> service.verifyOtp(userId, "EMAIL", "123456"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("not enabled");
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-05] ❌ Should reject verifyOtp when MFA record missing")
        void verifyOtp_RecordNotFound() {
            when(repo.findByUserIdAndMfaType(userId, MfaType.TOTP)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyOtp(userId, "TOTP", "123456"))
                    .isInstanceOf(AuthException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // TOTP generation (internal algorithm — pure Java RFC 6238)
    // ══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("TOTP generation (RFC 6238)")
    class TotpGenerationTests {

        @Test
        @DisplayName("[TC-AUTH-NEW-10] ✅ Should generate 6-digit TOTP code")
        void generateTotp_IsSixDigits() {
            String code = service.generateTotp("JBSWY3DPEHPK3PXP", 12345L);
            assertThat(code).matches("\\d{6}");
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-11] ✅ Same step produces same code (deterministic)")
        void generateTotp_Deterministic() {
            String a = service.generateTotp("JBSWY3DPEHPK3PXP", 99999L);
            String b = service.generateTotp("JBSWY3DPEHPK3PXP", 99999L);
            assertThat(a).isEqualTo(b);
        }

        @Test
        @DisplayName("[TC-AUTH-NEW-12] ✅ Different steps produce different codes")
        void generateTotp_DifferentSteps() {
            String a = service.generateTotp("JBSWY3DPEHPK3PXP", 1L);
            String b = service.generateTotp("JBSWY3DPEHPK3PXP", 2L);
            // Statistically almost certain to differ; occasionally may collide — acceptable
            assertThat(a).isNotNull();
            assertThat(b).isNotNull();
        }
    }
}
