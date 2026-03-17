package com.biolab.auth.controller;

import com.biolab.auth.dto.request.MfaSetupRequest;
import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.dto.response.MfaSetupResponse;
import com.biolab.auth.dto.response.MessageResponse;
import com.biolab.auth.email.EmailService;
import com.biolab.auth.entity.MfaSettings;
import com.biolab.auth.entity.User;
import com.biolab.auth.entity.enums.MfaType;
import com.biolab.auth.exception.AuthException;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.MfaSettingsRepository;
import com.biolab.auth.repository.UserRepository;
import com.biolab.auth.security.TotpUtil;
import com.biolab.common.security.CurrentUser;
import com.biolab.common.security.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * MFA Controller — setup, verify, enable, disable with REAL TOTP and Email OTP.
 *
 * TOTP: Generates real Base32 secret compatible with Google Authenticator / Authy.
 *       Validates 6-digit codes using HMAC-SHA1 (RFC 6238) with ±30s drift tolerance.
 *
 * EMAIL: Generates a TOTP-derived 6-digit code and emails it to the user.
 *        Same validation logic — code is valid for 30 seconds (±1 step = 90s window).
 */
@RestController
@RequestMapping("/api/auth/mfa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "MFA", description = "Multi-Factor Authentication with TOTP and Email OTP")
public class MfaController {

    private final MfaSettingsRepository mfaSettingsRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    // ── GET /api/auth/mfa/status ──────────────────────────────────────────

    @GetMapping("/status")
    @Operation(summary = "Get MFA settings for current user")
    public ResponseEntity<List<MfaSettingsResponse>> getStatus() {
        CurrentUser current = CurrentUserContext.require();
        List<MfaSettingsResponse> settings = mfaSettingsRepository.findByUserId(current.userId())
                .stream()
                .map(m -> MfaSettingsResponse.builder()
                        .id(m.getId()).mfaType(m.getMfaType().name())
                        .isEnabled(m.getIsEnabled()).verifiedAt(m.getVerifiedAt()).build())
                .toList();
        return ResponseEntity.ok(settings);
    }

    // ── POST /api/auth/mfa/setup ──────────────────────────────────────────

    @PostMapping("/setup")
    @Operation(summary = "Initiate MFA setup — generates TOTP secret or sends email code")
    public ResponseEntity<MfaSetupResponse> setup(@Valid @RequestBody MfaSetupRequest request) {
        CurrentUser current = CurrentUserContext.require();
        UUID userId = current.userId();
        MfaType mfaType = MfaType.valueOf(request.getMfaType());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Optional<MfaSettings> existing = mfaSettingsRepository.findByUserIdAndMfaType(userId, mfaType);
        if (existing.isPresent() && existing.get().getIsEnabled()) {
            throw new AuthException("MFA " + mfaType + " is already enabled", HttpStatus.CONFLICT);
        }

        // Generate real TOTP secret
        String secret = TotpUtil.generateSecret();
        List<String> backupCodes = TotpUtil.generateBackupCodes();
        String qrCodeUrl = null;

        if (mfaType == MfaType.TOTP) {
            qrCodeUrl = TotpUtil.buildOtpAuthUrl(secret, user.getEmail(), "BioLabs");
        }

        // For EMAIL type: send the current code to the user's email
        if (mfaType == MfaType.EMAIL) {
            String emailCode = TotpUtil.generateCurrentCode(secret);
            emailService.sendMfaCode(user.getEmail(), user.getFirstName(), emailCode);
            log.info("MFA email code sent to: {}", user.getEmail());
        }

        // Save or update MFA settings (not yet enabled)
        MfaSettings settings = existing.orElse(MfaSettings.builder()
                .user(user).mfaType(mfaType).build());
        settings.setSecretKey(secret);
        settings.setBackupCodes(backupCodes.toArray(new String[0]));
        settings.setIsEnabled(false);
        settings.setUpdatedAt(Instant.now());
        mfaSettingsRepository.save(settings);

        log.info("MFA setup initiated for user: {} type: {}", userId, mfaType);

        return ResponseEntity.ok(MfaSetupResponse.builder()
                .secret(mfaType == MfaType.TOTP ? secret : null) // Don't expose secret for EMAIL
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(backupCodes)
                .build());
    }

    // ── POST /api/auth/mfa/enable ─────────────────────────────────────────

    @PostMapping("/enable")
    @Operation(summary = "Verify code and enable MFA — validates real TOTP/email code")
    public ResponseEntity<MessageResponse> enable(@RequestParam String mfaType, @RequestParam String code) {
        CurrentUser current = CurrentUserContext.require();
        UUID userId = current.userId();
        MfaType type = MfaType.valueOf(mfaType);

        MfaSettings settings = mfaSettingsRepository.findByUserIdAndMfaType(userId, type)
                .orElseThrow(() -> new AuthException("MFA not set up. Call /mfa/setup first.", HttpStatus.BAD_REQUEST));

        if (settings.getIsEnabled()) {
            throw new AuthException("MFA is already enabled", HttpStatus.CONFLICT);
        }

        // ── REAL VALIDATION ──────────────────────────────────────────────
        if (!TotpUtil.validateCode(settings.getSecretKey(), code)) {
            // Also check backup codes as fallback
            if (!isValidBackupCode(settings, code)) {
                throw new AuthException("Invalid verification code. Please try again.", HttpStatus.BAD_REQUEST);
            }
        }

        settings.setIsEnabled(true);
        settings.setVerifiedAt(Instant.now());
        settings.setUpdatedAt(Instant.now());
        mfaSettingsRepository.save(settings);

        log.info("MFA enabled for user: {} type: {}", userId, type);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("MFA has been enabled successfully. Your account is now protected.").build());
    }

    // ── POST /api/auth/mfa/disable ────────────────────────────────────────

    @PostMapping("/disable")
    @Operation(summary = "Disable MFA")
    public ResponseEntity<MessageResponse> disable(@RequestParam String mfaType) {
        CurrentUser current = CurrentUserContext.require();
        UUID userId = current.userId();

        MfaSettings settings = mfaSettingsRepository.findByUserIdAndMfaType(userId, MfaType.valueOf(mfaType))
                .orElseThrow(() -> new ResourceNotFoundException("MfaSettings", "type", mfaType));

        settings.setIsEnabled(false);
        settings.setVerifiedAt(null);
        settings.setSecretKey(null);
        settings.setBackupCodes(null);
        settings.setUpdatedAt(Instant.now());
        mfaSettingsRepository.save(settings);

        log.info("MFA disabled for user: {} type: {}", userId, mfaType);
        return ResponseEntity.ok(MessageResponse.builder().message("MFA has been disabled.").build());
    }

    // ── POST /api/auth/mfa/send-code (for EMAIL type — resend code) ──────

    @PostMapping("/send-code")
    @Operation(summary = "Send/resend email MFA code for login verification")
    public ResponseEntity<MessageResponse> sendCode() {
        CurrentUser current = CurrentUserContext.require();
        UUID userId = current.userId();

        MfaSettings settings = mfaSettingsRepository.findByUserIdAndMfaType(userId, MfaType.EMAIL)
                .orElseThrow(() -> new AuthException("Email MFA is not set up", HttpStatus.BAD_REQUEST));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String code = TotpUtil.generateCurrentCode(settings.getSecretKey());
        emailService.sendMfaCode(user.getEmail(), user.getFirstName(), code);

        log.info("MFA email code resent to user: {}", userId);
        return ResponseEntity.ok(MessageResponse.builder()
                .message("Verification code sent to your email.").build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private boolean isValidBackupCode(MfaSettings settings, String code) {
        if (settings.getBackupCodes() == null || code == null) return false;
        String normalized = code.toUpperCase().trim();
        String[] codes = settings.getBackupCodes();
        for (int i = 0; i < codes.length; i++) {
            if (codes[i] != null && codes[i].equals(normalized)) {
                // Consume the backup code (one-time use)
                codes[i] = null;
                settings.setBackupCodes(codes);
                log.info("Backup code consumed for user: {}", settings.getUser().getId());
                return true;
            }
        }
        return false;
    }
}
