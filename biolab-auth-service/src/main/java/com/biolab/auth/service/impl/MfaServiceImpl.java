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
import com.biolab.auth.service.MfaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Full MFA lifecycle — TOTP (RFC 6238) and Email OTP setup, enable, disable,
 * and step-up verification for the login flow.
 *
 * <h3>TOTP (RFC 6238 / RFC 4226) — no external library</h3>
 * <ul>
 *   <li>Secret: 20-byte CSPRNG, encoded as Base32 (A–Z + 2–7, no padding)</li>
 *   <li>Algorithm: HMAC-SHA1</li>
 *   <li>Step: 30 seconds; tolerance: ±1 step (±30 s)</li>
 *   <li>QR URL: standard {@code otpauth://totp/} format</li>
 * </ul>
 *
 * <h3>Email OTP</h3>
 * <ul>
 *   <li>6-digit numeric, stored as {@code secret_key} in DB</li>
 *   <li>Expiry enforced via {@code email_otp_expires_at}</li>
 *   <li>Cleared from DB after successful verification</li>
 * </ul>
 *
 * <h3>Backup codes</h3>
 * <ul>
 *   <li>8 codes of format {@code XXXX-YYYY}, unambiguous charset</li>
 *   <li>Generated at {@link #enable}, returned once, not re-retrievable</li>
 * </ul>
 *
 * @author BioLab Engineering Team
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MfaServiceImpl implements MfaService {

    private final MfaSettingsRepository mfaRepo;
    private final UserRepository        userRepo;
    private final EmailService          emailService;

    @Value("${app.name:BioLabs}")
    private String appName;

    // ── TOTP constants ─────────────────────────────────────────────────────
    private static final int TOTP_DIGITS    = 6;
    private static final int TOTP_STEP_SEC  = 30;
    private static final int TOTP_TOLERANCE = 1;
    private static final int SECRET_BYTES   = 20;

    // ── EMAIL OTP constants ────────────────────────────────────────────────
    private static final long EMAIL_OTP_TTL_SECONDS = 600; // 10 minutes (FIX-14)

    // ── Backup code constants ──────────────────────────────────────────────
    private static final int    BACKUP_CODE_COUNT = 8;
    private static final String BACKUP_CHARS      = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0,O,I,1,l

    private static final SecureRandom RANDOM = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<MfaSettingsResponse> getSettings(UUID userId) {
        return mfaRepo.findByUserId(userId).stream().map(this::toResponse).toList();
    }

    @Override
    public MfaSetupResponse initiate(UUID userId, String mfaType) {
        User     user = loadUser(userId);
        MfaType  type = parseMfaType(mfaType);
        MfaSettings ms = loadOrCreate(userId, user, type);

        return type == MfaType.TOTP ? initiateTotpSetup(ms, user) : initiateEmailSetup(ms, user);
    }

    @Override
    public MfaSetupResponse enable(UUID userId, String code) {
        MfaSettings ms = mfaRepo.findByUserId(userId).stream()
                .filter(m -> !Boolean.TRUE.equals(m.getIsEnabled()))
                .max(java.util.Comparator.comparing(
                        m -> m.getUpdatedAt() != null ? m.getUpdatedAt() : Instant.EPOCH))
                .orElseThrow(() -> new AuthException(
                        "No pending MFA setup found. Call /setup first."));

        validateOtpInternal(ms, code);

        List<String> backupCodes = generateBackupCodes();
        ms.setIsEnabled(true);
        ms.setVerifiedAt(Instant.now());
        ms.setBackupCodes(backupCodes.toArray(new String[0]));
        if (ms.getMfaType() == MfaType.EMAIL) {
            ms.setSecretKey(null);
            ms.setEmailOtpExpiresAt(null);  // FIX-14: clear expiry after use
        }
        ms.setUpdatedAt(Instant.now());
        mfaRepo.save(ms);

        log.info("MFA enabled — userId={} type={}", userId, ms.getMfaType());
        return MfaSetupResponse.builder().backupCodes(backupCodes).build();
    }

    /**
     * {@inheritDoc}
     * FIX-1: Used by AuthServiceImpl.verifyMfa() during login step-up.
     * Validates against the ACTIVE (enabled) MFA record — does not modify it.
     */
    @Override
    public void verifyOtp(UUID userId, String mfaType, String code) {
        MfaType type = parseMfaType(mfaType);
        MfaSettings ms = mfaRepo.findByUserIdAndMfaType(userId, type)
                .orElseThrow(() -> new AuthException(
                        "MFA method " + mfaType + " not found for user."));

        if (!Boolean.TRUE.equals(ms.getIsEnabled())) {
            throw new AuthException("MFA method " + mfaType + " is not enabled.");
        }

        // For EMAIL login step-up: issue a fresh OTP first (no stored code yet)
        // The login OTP was sent when MFA challenge was issued in login()
        // Here we just validate whatever is stored as secretKey
        validateOtpInternal(ms, code);

        // For EMAIL: clear the one-time code after successful validation
        if (type == MfaType.EMAIL) {
            ms.setSecretKey(null);
            ms.setEmailOtpExpiresAt(null);
            ms.setUpdatedAt(Instant.now());
            mfaRepo.save(ms);
        }

        log.info("OTP verified for login step-up — userId={} type={}", userId, type);
    }

    @Override
    public void disable(UUID userId, String mfaType) {
        MfaType type = parseMfaType(mfaType);
        MfaSettings ms = mfaRepo.findByUserIdAndMfaType(userId, type)
                .orElseThrow(() -> new ResourceNotFoundException("MfaSettings", "type", mfaType));

        ms.setIsEnabled(false);
        ms.setSecretKey(null);
        ms.setBackupCodes(null);
        ms.setVerifiedAt(null);
        ms.setEmailOtpExpiresAt(null);
        ms.setUpdatedAt(Instant.now());
        mfaRepo.save(ms);

        log.info("MFA disabled — userId={} type={}", userId, type);
    }

    // ─────────────────────────────────────────────────────────────────────
    // TOTP helpers
    // ─────────────────────────────────────────────────────────────────────

    private MfaSetupResponse initiateTotpSetup(MfaSettings ms, User user) {
        byte[] secretBytes = new byte[SECRET_BYTES];
        RANDOM.nextBytes(secretBytes);
        String secret = encodeBase32(secretBytes);

        String accountName = URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8);
        String issuer      = URLEncoder.encode(appName, StandardCharsets.UTF_8);
        String qrCodeUrl   = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                issuer, accountName, secret, issuer, TOTP_DIGITS, TOTP_STEP_SEC);

        ms.setSecretKey(secret);
        ms.setIsEnabled(false);
        ms.setVerifiedAt(null);
        ms.setBackupCodes(null);
        ms.setUpdatedAt(Instant.now());
        mfaRepo.save(ms);

        log.info("TOTP setup initiated — userId={}", user.getId());
        return MfaSetupResponse.builder().secret(secret).qrCodeUrl(qrCodeUrl).build();
    }

    private MfaSetupResponse initiateEmailSetup(MfaSettings ms, User user) {
        String otp = String.format("%06d", RANDOM.nextInt(1_000_000));

        ms.setSecretKey(otp);
        ms.setIsEnabled(false);
        ms.setVerifiedAt(null);
        ms.setBackupCodes(null);
        // FIX-14: set expiry on the email OTP
        ms.setEmailOtpExpiresAt(Instant.now().plusSeconds(EMAIL_OTP_TTL_SECONDS));
        ms.setUpdatedAt(Instant.now());
        mfaRepo.save(ms);

        emailService.sendMfaCode(user.getEmail(), user.getFirstName(), otp);

        log.info("EMAIL MFA setup initiated — userId={}", user.getId());
        return MfaSetupResponse.builder().build();
    }

    /** Dispatches to TOTP or EMAIL validation based on the record's type. */
    private void validateOtpInternal(MfaSettings ms, String code) {
        if (ms.getMfaType() == MfaType.TOTP) {
            validateTotp(ms, code);
        } else {
            validateEmailOtp(ms, code);
        }
    }

    private void validateTotp(MfaSettings ms, String userCode) {
        if (ms.getSecretKey() == null) {
            throw new AuthException("MFA setup incomplete — please call /setup first.");
        }
        long step  = Instant.now().getEpochSecond() / TOTP_STEP_SEC;
        boolean ok = false;
        for (int d = -TOTP_TOLERANCE; d <= TOTP_TOLERANCE; d++) {
            if (generateTotp(ms.getSecretKey(), step + d).equals(userCode)) { ok = true; break; }
        }
        if (!ok) {
            log.warn("TOTP verification failed — userId={}", ms.getUser().getId());
            throw new AuthException("Invalid verification code. Please try again.",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    private void validateEmailOtp(MfaSettings ms, String userCode) {
        if (ms.getSecretKey() == null) {
            throw new AuthException(
                    "MFA setup incomplete — please call /setup first to receive a code.");
        }
        // FIX-14: enforce expiry on EMAIL OTP
        if (ms.getEmailOtpExpiresAt() != null && Instant.now().isAfter(ms.getEmailOtpExpiresAt())) {
            log.warn("EMAIL OTP expired — userId={}", ms.getUser().getId());
            throw new AuthException(
                    "Verification code has expired. Please request a new one.",
                    HttpStatus.UNAUTHORIZED);
        }
        if (!ms.getSecretKey().equals(userCode.trim())) {
            log.warn("EMAIL OTP verification failed — userId={}", ms.getUser().getId());
            throw new AuthException(
                    "Invalid verification code. Please check your email and try again.",
                    HttpStatus.UNAUTHORIZED);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // RFC 6238 TOTP generation (pure Java, no external library)
    // ─────────────────────────────────────────────────────────────────────

    /** Package-private for unit tests. */
    String generateTotp(String base32Secret, long timeStep) {
        try {
            byte[] key     = decodeBase32(base32Secret);
            byte[] counter = ByteBuffer.allocate(8).putLong(timeStep).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hmac    = mac.doFinal(counter);
            int offset     = hmac[hmac.length - 1] & 0x0F;
            int truncated  = ((hmac[offset]     & 0x7F) << 24)
                           | ((hmac[offset + 1] & 0xFF) << 16)
                           | ((hmac[offset + 2] & 0xFF) << 8)
                           |  (hmac[offset + 3] & 0xFF);
            return String.format("%0" + TOTP_DIGITS + "d",
                    truncated % (int) Math.pow(10, TOTP_DIGITS));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("TOTP generation failed", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Base32 codec (RFC 4648 — no external library)
    // ─────────────────────────────────────────────────────────────────────

    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private static String encodeBase32(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buf = 0, bitsLeft = 0;
        for (byte b : data) {
            buf = (buf << 8) | (b & 0xFF); bitsLeft += 8;
            while (bitsLeft >= 5) { bitsLeft -= 5; sb.append(BASE32.charAt((buf >> bitsLeft) & 0x1F)); }
        }
        if (bitsLeft > 0) sb.append(BASE32.charAt((buf << (5 - bitsLeft)) & 0x1F));
        return sb.toString();
    }

    private static byte[] decodeBase32(String base32) {
        String clean = base32.toUpperCase().replaceAll("[=\\s]", "");
        byte[] out   = new byte[clean.length() * 5 / 8];
        int buf = 0, bitsLeft = 0, idx = 0;
        for (char c : clean.toCharArray()) {
            int v = BASE32.indexOf(c);
            if (v < 0) throw new IllegalArgumentException("Invalid Base32 char: " + c);
            buf = (buf << 5) | v; bitsLeft += 5;
            if (bitsLeft >= 8) { bitsLeft -= 8; out[idx++] = (byte) ((buf >> bitsLeft) & 0xFF); }
        }
        return Arrays.copyOf(out, idx);
    }

    // ─────────────────────────────────────────────────────────────────────
    // Backup codes
    // ─────────────────────────────────────────────────────────────────────

    private List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>(BACKUP_CODE_COUNT);
        for (int i = 0; i < BACKUP_CODE_COUNT; i++) {
            codes.add(segment(4) + "-" + segment(4));
        }
        return codes;
    }

    private String segment(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) sb.append(BACKUP_CHARS.charAt(RANDOM.nextInt(BACKUP_CHARS.length())));
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    private User loadUser(UUID userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId.toString()));
    }

    private MfaType parseMfaType(String mfaType) {
        try { return MfaType.valueOf(mfaType.toUpperCase()); }
        catch (IllegalArgumentException e) {
            throw new AuthException("Invalid MFA type: " + mfaType + ". Must be TOTP or EMAIL.");
        }
    }

    private MfaSettings loadOrCreate(UUID userId, User user, MfaType type) {
        return mfaRepo.findByUserIdAndMfaType(userId, type)
                .orElseGet(() -> mfaRepo.save(
                        MfaSettings.builder().user(user).mfaType(type).isEnabled(false).build()));
    }

    private MfaSettingsResponse toResponse(MfaSettings ms) {
        return MfaSettingsResponse.builder()
                .id(ms.getId()).mfaType(ms.getMfaType().name())
                .isEnabled(ms.getIsEnabled()).verifiedAt(ms.getVerifiedAt()).build();
    }
}
