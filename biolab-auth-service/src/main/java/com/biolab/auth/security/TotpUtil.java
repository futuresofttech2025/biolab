package com.biolab.auth.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

/**
 * TOTP (Time-based One-Time Password) implementation — RFC 6238.
 * Zero external dependencies: uses JDK's {@code javax.crypto.Mac}.
 *
 * <p>Compatible with Google Authenticator, Authy, Microsoft Authenticator.</p>
 *
 * @author BioLab Engineering Team
 */
public final class TotpUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    /** Allow ±1 time step drift (30s before and after) to handle clock skew. */
    private static final int ALLOWED_DRIFT = 1;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";

    private TotpUtil() {}

    /**
     * Generate a random 20-byte Base32-encoded secret.
     * This is the secret shared between server and authenticator app.
     */
    public static String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * Generate the otpauth:// URI for QR code generation.
     * Scan this QR with Google Authenticator / Authy.
     */
    public static String buildOtpAuthUrl(String secret, String email, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&digits=%d&period=%d",
                issuer, email, secret, issuer, CODE_DIGITS, TIME_STEP_SECONDS);
    }

    /**
     * Validate a TOTP code against the secret.
     * Allows ±1 time step drift to handle clock skew.
     *
     * @param secret Base32-encoded secret
     * @param code   6-digit code entered by user
     * @return true if valid
     */
    public static boolean validateCode(String secret, String code) {
        if (code == null || code.length() != CODE_DIGITS) return false;

        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return false;
        }

        byte[] secretBytes = base32Decode(secret);
        long currentTimeStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;

        // Check current time step and ±1 drift window
        for (int drift = -ALLOWED_DRIFT; drift <= ALLOWED_DRIFT; drift++) {
            int expected = generateCode(secretBytes, currentTimeStep + drift);
            if (expected == codeInt) {
                return true;
            }
        }
        return false;
    }

    /**
     * Generate a TOTP code for the current time step.
     * Used for testing / email OTP generation.
     */
    public static String generateCurrentCode(String secret) {
        byte[] secretBytes = base32Decode(secret);
        long timeStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        int code = generateCode(secretBytes, timeStep);
        return String.format("%0" + CODE_DIGITS + "d", code);
    }

    // ── Core HOTP/TOTP calculation (RFC 4226 / RFC 6238) ────────────────

    private static int generateCode(byte[] secret, long counter) {
        byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();
        byte[] hash = hmacSha1(secret, counterBytes);

        // Dynamic truncation (RFC 4226 §5.4)
        int offset = hash[hash.length - 1] & 0x0F;
        int binary = ((hash[offset] & 0x7F) << 24)
                   | ((hash[offset + 1] & 0xFF) << 16)
                   | ((hash[offset + 2] & 0xFF) << 8)
                   | (hash[offset + 3] & 0xFF);

        return binary % (int) Math.pow(10, CODE_DIGITS);
    }

    private static byte[] hmacSha1(byte[] key, byte[] data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA1 computation failed", e);
        }
    }

    // ── Base32 encoding/decoding (RFC 4648) ─────────────────────────────

    public static String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0, bitsLeft = 0;
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                sb.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            sb.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
        }
        return sb.toString();
    }

    public static byte[] base32Decode(String encoded) {
        String s = encoded.toUpperCase().replaceAll("[^A-Z2-7]", "");
        byte[] result = new byte[s.length() * 5 / 8];
        int buffer = 0, bitsLeft = 0, index = 0;
        for (char c : s.toCharArray()) {
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) continue;
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                result[index++] = (byte) (buffer >> (bitsLeft - 8));
                bitsLeft -= 8;
            }
        }
        return java.util.Arrays.copyOf(result, index);
    }

    /**
     * Generate 6 backup codes in XXXX-XXXX format.
     */
    public static java.util.List<String> generateBackupCodes() {
        java.util.List<String> codes = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
            codes.add(String.format("%04X-%04X", RANDOM.nextInt(0xFFFF), RANDOM.nextInt(0xFFFF)));
        }
        return codes;
    }
}
