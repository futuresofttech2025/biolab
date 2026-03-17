package com.biolab.auth.service;

import com.biolab.auth.dto.request.MfaSetupRequest;
import com.biolab.auth.dto.request.MfaVerifyRequest;
import com.biolab.auth.dto.response.MfaSettingsResponse;
import com.biolab.auth.dto.response.MfaSetupResponse;

import java.util.List;
import java.util.UUID;

/**
 * MFA lifecycle management: initiate setup, enable after verification, disable.
 *
 * <h3>Flow:</h3>
 * <pre>
 *   1. POST /mfa/{userId}/setup   → returns QR / secret (pending, not yet active)
 *   2. POST /mfa/{userId}/enable  → user scans QR, submits TOTP code → MFA goes live
 *   3. POST /mfa/{userId}/disable → user disables MFA
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
public interface MfaSetupService {

    /**
     * Returns all registered MFA method records for the given user,
     * including enabled/disabled status and verification timestamp.
     */
    List<MfaSettingsResponse> getSettings(UUID userId);

    /**
     * Initiates MFA enrollment.
     * <ul>
     *   <li>TOTP: generates a TOTP secret, persists it as un-enabled, returns QR URL + secret.</li>
     *   <li>EMAIL: triggers an OTP email, no secret returned in response.</li>
     * </ul>
     *
     * @throws com.biolab.auth.exception.DuplicateResourceException if MFA of this type already enabled
     */
    MfaSetupResponse initiate(UUID userId, MfaSetupRequest request);

    /**
     * Verifies the OTP code and marks the MFA method as enabled.
     * Returns backup codes on first activation.
     *
     * @throws com.biolab.auth.exception.AuthException if code is invalid or expired
     */
    MfaSetupResponse enable(UUID userId, MfaVerifyRequest request);

    /**
     * Disables the specified MFA type for the user.
     *
     * @throws com.biolab.auth.exception.ResourceNotFoundException if the MFA type does not exist
     */
    void disable(UUID userId, String mfaType);
}
