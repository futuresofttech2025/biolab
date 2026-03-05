package com.biolab.auth.service;

/**
 * Contract for all transactional email sending in the auth service.
 *
 * <p>Currently covers:</p>
 * <ul>
 *   <li>Password reset link emails</li>
 *   <li>Password change confirmation emails</li>
 * </ul>
 */
public interface EmailService {

    /**
     * Sends a password-reset email containing a time-limited link.
     *
     * @param toEmail   recipient address
     * @param firstName recipient first name (used in greeting)
     * @param resetLink full reset URL (https://app.../reset-password?token=...)
     * @param expiryMin token validity window in minutes
     */
    void sendPasswordResetEmail(String toEmail,
                                String firstName,
                                String resetLink,
                                int    expiryMin);

    /**
     * Sends a notification email confirming a successful password change.
     * Alerts the user to contact support if the change was not authorised.
     *
     * @param toEmail   recipient address
     * @param firstName recipient first name
     */
    void sendPasswordChangedEmail(String toEmail, String firstName);
}