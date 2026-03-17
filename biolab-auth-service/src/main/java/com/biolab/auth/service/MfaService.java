package com.biolab.auth.service;

import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.dto.response.MfaSetupResponse;

import java.util.List;
import java.util.UUID;

/**
 * Contract for the MFA lifecycle within the auth service.
 *
 * <h3>Setup flow</h3>
 * <ol>
 *   <li>{@link #getSettings(UUID)}        — check current MFA status</li>
 *   <li>{@link #initiate(UUID, String)}   — generate secret / QR / email OTP</li>
 *   <li>{@link #enable(UUID, String)}     — verify first OTP code to activate</li>
 * </ol>
 *
 * <h3>Login step-up flow</h3>
 * <ol>
 *   <li>{@code AuthService.login()} detects MFA is required</li>
 *   <li>{@code AuthService.verifyMfa()} calls {@link #verifyOtp(UUID, String, String)}</li>
 * </ol>
 *
 * @author BioLab Engineering Team
 */
public interface MfaService {

    /**
     * Returns all MFA settings records for a user (one per method: TOTP, EMAIL).
     *
     * @param userId target user UUID
     * @return list of MFA settings; empty if user has never configured MFA
     */
    List<MfaSettingsResponse> getSettings(UUID userId);

    /**
     * Initiates MFA setup for the given method.
     *
     * @param userId  target user UUID
     * @param mfaType {@code "TOTP"} or {@code "EMAIL"}
     * @return setup payload (secret + QR URL for TOTP; empty for EMAIL)
     */
    MfaSetupResponse initiate(UUID userId, String mfaType);

    /**
     * Verifies the supplied OTP and activates the MFA method.
     * Returns backup codes that the user must save.
     *
     * @param userId target user UUID
     * @param code   6-digit OTP
     * @return response containing one-time backup codes
     */
    MfaSetupResponse enable(UUID userId, String code);

    /**
     * Verifies an OTP code for an already-enabled MFA method during login step-up.
     * Unlike {@link #enable}, this does NOT change the MFA enabled state or
     * generate backup codes — it is a pure validation call.
     *
     * @param userId  the user attempting to complete MFA login
     * @param mfaType the method type ({@code "TOTP"} or {@code "EMAIL"})
     * @param code    the 6-digit OTP submitted by the user
     * @throws com.biolab.auth.exception.AuthException if the code is invalid
     */
    void verifyOtp(UUID userId, String mfaType, String code);

    /**
     * Disables the specified MFA method for the user.
     *
     * @param userId  target user UUID
     * @param mfaType the method to disable
     */
    void disable(UUID userId, String mfaType);
}
