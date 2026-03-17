package com.biolab.auth.service;

/**
 * Contract for all transactional email in the auth service.
 *
 * <h3>Sprint 0 — GAP-03 (FIX-18) note:</h3>
 * <p>The stale duplicate {@code com.biolab.auth.email.EmailService} class
 * MUST be deleted. Only this interface and its single implementation
 * {@link com.biolab.auth.service.impl.EmailServiceImpl} should exist.
 * Keeping the duplicate causes an ambiguous Spring bean injection error.</p>
 *
 * <p>File to delete:</p>
 * <pre>src/main/java/com/biolab/auth/email/EmailService.java</pre>
 *
 * @author BioLab Engineering Team
 * @version 1.2.0
 */
public interface EmailService {

    /**
     * Sends an email-verification link after user registration.
     *
     * @param toEmail   recipient email address
     * @param firstName user's first name
     * @param rawToken  the unencoded verification token (included in the URL)
     */
    void sendVerificationEmail(String toEmail, String firstName, String rawToken);

    /**
     * Sends a password-reset link containing a time-limited token.
     *
     * @param toEmail   recipient email address
     * @param firstName user's first name
     * @param resetLink full reset URL (https://app.../reset-password?token=...)
     * @param expiryMin token validity window in minutes
     */
    void sendPasswordResetEmail(String toEmail,
                                String firstName,
                                String resetLink,
                                int    expiryMin);

    /**
     * Sends a security notification confirming a successful password change.
     * Prompts the user to contact support if the change was not authorised.
     *
     * @param toEmail   recipient email address
     * @param firstName user's first name
     */
    void sendPasswordChangedEmail(String toEmail, String firstName);

    /**
     * Sends a 6-digit MFA verification code via email.
     * Used for EMAIL-type MFA during login step-up.
     *
     * @param toEmail   recipient email address
     * @param firstName user's first name
     * @param code      6-digit verification code
     */
    void sendMfaCode(String toEmail, String firstName, String code);
}
