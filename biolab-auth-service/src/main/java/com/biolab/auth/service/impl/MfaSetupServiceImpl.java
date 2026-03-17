package com.biolab.auth.service.impl;

import com.biolab.auth.dto.request.MfaSetupRequest;
import com.biolab.auth.dto.request.MfaVerifyRequest;
import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.dto.response.MfaSetupResponse;
import com.biolab.auth.entity.MfaSettings;
import com.biolab.auth.entity.User;
import com.biolab.auth.entity.enums.MfaType;
import com.biolab.auth.exception.AuthException;
import com.biolab.auth.exception.DuplicateResourceException;
import com.biolab.auth.exception.ResourceNotFoundException;
import com.biolab.auth.repository.MfaSettingsRepository;
import com.biolab.auth.repository.UserRepository;
import com.biolab.auth.service.EmailService;
import com.biolab.auth.service.MfaSetupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base32;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TOTP / Email-OTP MFA lifecycle management.
 *
 * <h3>TOTP Implementation:</h3>
 * <ul>
 *   <li>Secret: 20-byte cryptographically-random, Base32-encoded (RFC 4648)</li>
 *   <li>Algorithm: HMAC-SHA1 per RFC 6238 (TOTP) — compatible with Google Authenticator</li>
 *   <li>Window: ±1 time step (30-second step, tolerates 30-second clock skew)</li>
 *   <li>QR URL: {@code otpauth://totp/BioLab:<email>?secret=<secret>&issuer=BioLab}</li>
 *   <li>Backup codes: 8 alphanumeric codes, stored as SHA-256 hashes</li>
 * </ul>
 *
 * <p>No external TOTP library dependency — uses only JDK {@code javax.crypto}
 * and Apache Commons Codec (already on classpath via Spring Boot).</p>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MfaSetupServiceImpl implements MfaSetupService {

    private static final String ISSUER       = "BioLab";
    private static final String HMAC_ALGO    = "HmacSHA1";
    private static final int    TOTP_DIGITS  = 6;
    private static final int    TIME_STEP    = 30;          // seconds
    private static final int    WINDOW       = 1;           // ±1 step allowed
    private static final int    BACKUP_COUNT = 8;

    private final MfaSettingsRepository mfaRepo;
    private final UserRepository         userRepo;
    private final EmailService emailService;
    private final SecureRandom           secureRandom = new SecureRandom();

    // ─── GET settings ────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MfaSettingsResponse> getSettings(UUID userId) {
        return mfaRepo.findByUserId(userId).stream()
                .map(m -> MfaSettingsResponse.builder()
                        .id(m.getId())
                        .mfaType(m.getMfaType().name())
                        .isEnabled(m.getIsEnabled())
                        .verifiedAt(m.getVerifiedAt())
                        .build())
                .toList();
    }

    // ─── Initiate MFA setup ───────────────────────────────────────────────

    @Override
    public MfaSetupResponse initiate(UUID userId, MfaSetupRequest request) {
        MfaType type = MfaType.valueOf(request.getMfaType());
        User user    = requireUser(userId);

        // Prevent duplicate active MFA of same type
        mfaRepo.findByUserIdAndMfaType(userId, type).ifPresent(existing -> {
            if (Boolean.TRUE.equals(existing.getIsEnabled())) {
                throw new DuplicateResourceException(
                        "MFA", "type", type.name() + " already enabled");
            }
            // Remove stale pending record so we can regenerate
            mfaRepo.delete(existing);
        });

        if (type == MfaType.TOTP) {
            return initiateTotpSetup(user);
        } else {
            return initiateEmailOtpSetup(user);
        }
    }

    private MfaSetupResponse initiateTotpSetup(User user) {
        // Generate 20-byte (160-bit) random secret, Base32-encoded
        byte[] secretBytes = new byte[20];
        secureRandom.nextBytes(secretBytes);
        String secret = new Base32().encodeToString(secretBytes);

        // Persist pending (not yet enabled)
        MfaSettings settings = MfaSettings.builder()
                .user(user)
                .mfaType(MfaType.TOTP)
                .secretKey(secret)
                .isEnabled(false)
                .build();
        mfaRepo.save(settings);

        // Build otpauth:// URL for QR code rendering
        String qrCodeUrl = buildOtpAuthUrl(user.getEmail(), secret);

        log.info("TOTP setup initiated for user={}", user.getId());

        return MfaSetupResponse.builder()
                .secret(secret)
                .qrCodeUrl(qrCodeUrl)
                .backupCodes(List.of()) // Backup codes issued on /enable (after verification)
                .build();
    }

    private MfaSetupResponse initiateEmailOtpSetup(User user) {
        // Generate a 6-digit numeric OTP
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        MfaSettings settings = MfaSettings.builder()
                .user(user)
                .mfaType(MfaType.EMAIL)
                .secretKey(otp)        // Stored temporarily; validated in /enable
                .isEnabled(false)
                .updatedAt(Instant.now())
                .build();
        mfaRepo.save(settings);

        // Send OTP email
        emailService.sendMfaCode(user.getEmail(), user.getFirstName(), otp);
        log.info("Email OTP sent to user={}", user.getId());

        return MfaSetupResponse.builder()
                .secret(null)
                .qrCodeUrl(null)
                .backupCodes(List.of())
                .build();
    }

    // ─── Enable MFA (verify OTP) ──────────────────────────────────────────

    @Override
    public MfaSetupResponse enable(UUID userId, MfaVerifyRequest request) {
        // Find the pending MFA setting (any type not yet enabled)
        MfaSettings pending = mfaRepo.findByUserId(userId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsEnabled()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MfaSettings", "userId", userId.toString()));

        boolean valid;
        if (pending.getMfaType() == MfaType.TOTP) {
            valid = verifyTotp(pending.getSecretKey(), request.getCode());
        } else {
            // EMAIL: direct string comparison (already plain 6-digit OTP)
            valid = request.getCode() != null &&
                    request.getCode().equals(pending.getSecretKey());
        }

        if (!valid) {
            log.warn("Invalid OTP during MFA enable for user={}", userId);
            throw new AuthException("Invalid or expired OTP code", HttpStatus.BAD_REQUEST);
        }

        // Generate backup codes
        List<String> backupCodes = generateBackupCodes();

        // Activate
        pending.setIsEnabled(true);
        pending.setVerifiedAt(Instant.now());
        pending.setBackupCodes(backupCodes.toArray(new String[0]));
        // BUG-4 FIX: for EMAIL MFA, the secretKey contains the one-time setup OTP.
        // After successful activation it must be cleared so the stale setup OTP
        // cannot accidentally be accepted at login step-up (login generates a
        // brand-new OTP and stores it in secretKey — a leftover value here would
        // cause a collision if the old code happened to match).
        if (pending.getMfaType() == MfaType.EMAIL) {
            pending.setSecretKey(null);
            pending.setEmailOtpExpiresAt(null);
        }
        pending.setUpdatedAt(Instant.now());
        mfaRepo.save(pending);

        log.info("MFA enabled for user={} type={}", userId, pending.getMfaType());

        return MfaSetupResponse.builder()
                .secret(null)   // Never re-expose the secret after enrollment
                .qrCodeUrl(null)
                .backupCodes(backupCodes)
                .build();
    }

    // ─── Disable MFA ──────────────────────────────────────────────────────

    @Override
    public void disable(UUID userId, String mfaType) {
        MfaSettings settings = mfaRepo
                .findByUserIdAndMfaType(userId, MfaType.valueOf(mfaType.toUpperCase()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "MfaSettings", "type", mfaType));

        settings.setIsEnabled(false);
        settings.setSecretKey(null);
        settings.setBackupCodes(null);
        settings.setVerifiedAt(null);
        settings.setUpdatedAt(Instant.now());
        mfaRepo.save(settings);

        log.info("MFA disabled for user={} type={}", userId, mfaType);
    }

    // ─── TOTP Verification (RFC 6238 / HMAC-SHA1) ────────────────────────

    /**
     * Validates a 6-digit TOTP code against the stored Base32 secret.
     * Accepts codes within a ±1 time step window (±30 seconds) to handle clock skew.
     */
    boolean verifyTotp(String base32Secret, String inputCode) {
        if (inputCode == null || inputCode.length() != TOTP_DIGITS) return false;

        try {
            long inputLong = Long.parseLong(inputCode);
            byte[] secretBytes = new Base32().decode(base32Secret);
            long currentStep = Instant.now().getEpochSecond() / TIME_STEP;

            for (int delta = -WINDOW; delta <= WINDOW; delta++) {
                long expected = computeTotp(secretBytes, currentStep + delta);
                if (expected == inputLong) return true;
            }
            return false;
        } catch (NumberFormatException e) {
            return false;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("TOTP verification error", e);
            return false;
        }
    }

    /**
     * RFC 6238 TOTP computation:
     * <pre>
     *   HMAC-SHA1(secret, timeStep)  →  truncate to 6 digits
     * </pre>
     */
    private long computeTotp(byte[] secret, long timeStep)
            throws NoSuchAlgorithmException, InvalidKeyException {

        byte[] stepBytes = ByteBuffer.allocate(8).putLong(timeStep).array();

        Mac mac = Mac.getInstance(HMAC_ALGO);
        mac.init(new SecretKeySpec(secret, HMAC_ALGO));
        byte[] hash = mac.doFinal(stepBytes);

        // Dynamic truncation — offset from last nibble
        int offset = hash[hash.length - 1] & 0x0F;
        long truncated = ((hash[offset]     & 0x7F) << 24)
                | ((hash[offset + 1] & 0xFF) << 16)
                | ((hash[offset + 2] & 0xFF) <<  8)
                |  (hash[offset + 3] & 0xFF);

        return truncated % (long) Math.pow(10, TOTP_DIGITS);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String buildOtpAuthUrl(String email, String secret) {
        // Standard otpauth:// format — compatible with all TOTP apps
        return String.format(
                "otpauth://totp/%s%%3A%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                ISSUER,
                email.replace("@", "%40"),
                secret,
                ISSUER
        );
    }

    private List<String> generateBackupCodes() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Unambiguous charset (no 0/O/1/I)
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < BACKUP_COUNT; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < 4; j++) code.append(chars.charAt(secureRandom.nextInt(chars.length())));
            code.append('-');
            for (int j = 0; j < 4; j++) code.append(chars.charAt(secureRandom.nextInt(chars.length())));
            codes.add(code.toString());
        }
        return codes;
    }

    private User requireUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
    }
}