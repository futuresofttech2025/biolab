package com.biolab.auth.service.impl;

import com.biolab.auth.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

/**
 * SMTP-backed implementation of {@link EmailService} using Gmail.
 *
 * <p>All methods are annotated {@code @Async} — emails are dispatched on a
 * background thread and never block the HTTP request that triggered them.
 * Any {@link MailException} or {@link MessagingException} is caught and logged;
 * it is <strong>not</strong> re-thrown so that the forgot-password endpoint
 * always returns the same generic response (prevents email enumeration).</p>
 *
 * <h3>Gmail SMTP setup:</h3>
 * <pre>
 *   spring.mail.host     = smtp.gmail.com
 *   spring.mail.port     = 587
 *   spring.mail.username = your-app@gmail.com
 *   spring.mail.password = [Gmail App Password — 16 chars, NOT your login password]
 *
 *   How to generate an App Password:
 *     1. Enable 2-Step Verification on your Google account
 *     2. Visit https://myaccount.google.com/apppasswords
 *     3. Select Mail → Other → enter "BioLabs" → Generate
 *     4. Copy the 16-char code → set as MAIL_PASSWORD environment variable
 * </pre>
 *
 * @author BioLab Engineering Team
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:biolab.noreply@gmail.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:BioLabs Platform}")
    private String fromName;

    // ── Public API ────────────────────────────────────────────────────────

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail,
                                       String firstName,
                                       String resetLink,
                                       int    expiryMin) {
        log.info("Sending password-reset email to: {}", toEmail);
        try {
            sendHtmlEmail(
                    toEmail,
                    "Reset your BioLabs password",
                    buildResetEmailHtml(firstName, resetLink, expiryMin)
            );
            log.info("Password-reset email delivered to: {}", toEmail);
        } catch (MailException | MessagingException ex) {
            log.error("Failed to send password-reset email to {}: {}", toEmail, ex.getMessage());
            // Intentionally swallowed — caller returns generic response regardless
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    @Override
    public void sendPasswordChangedEmail(String toEmail, String firstName) {
        log.info("Sending password-changed confirmation to: {}", toEmail);
        try {
            sendHtmlEmail(
                    toEmail,
                    "Your BioLabs password has been changed",
                    buildPasswordChangedHtml(firstName)
            );
            log.info("Password-changed email delivered to: {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send password-changed email to {}: {}", toEmail, ex.getMessage());
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromAddress, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, /* html = */ true);
        mailSender.send(msg);
    }

    // ── HTML Email Templates ──────────────────────────────────────────────

    /**
     * Inline HTML for the password-reset email.
     * Table-based layout for maximum email-client compatibility.
     *
     * @param firstName  user's first name
     * @param resetLink  full one-click reset URL
     * @param expiryMin  token validity in minutes (shown in body)
     */
    private String buildResetEmailHtml(String firstName, String resetLink, int expiryMin) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Reset your BioLabs password</title>
        </head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:16px;overflow:hidden;
                            box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">

                <!-- Header -->
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);
                             padding:32px 40px;text-align:center;">
                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 12px;">
                      <tr>
                        <td style="background:rgba(255,255,255,0.15);border-radius:12px;
                                   padding:10px 14px;">
                          <span style="color:#ffffff;font-size:24px;line-height:1;">🧪</span>
                        </td>
                      </tr>
                    </table>
                    <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:800;
                               letter-spacing:-0.3px;">BioLabs</h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">
                      Biotech Services Marketplace
                    </p>
                  </td>
                </tr>

                <!-- Body -->
                <tr>
                  <td style="padding:40px 40px 32px;">
                    <h2 style="margin:0 0 16px;color:#0f172a;font-size:20px;font-weight:800;">
                      Password Reset Request
                    </h2>
                    <p style="margin:0 0 8px;color:#475569;font-size:14px;line-height:1.6;">
                      Hi <strong style="color:#0f172a;">%s</strong>,
                    </p>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;">
                      We received a request to reset the password for your BioLabs account.
                      Click the button below to choose a new password. This link will expire
                      in <strong style="color:#0f172a;">%d minutes</strong>.
                    </p>

                    <!-- CTA Button -->
                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                      <tr>
                        <td align="center"
                            style="background:linear-gradient(135deg,#0d9488,#059669);
                                   border-radius:10px;">
                          <a href="%s"
                             style="display:inline-block;padding:14px 36px;
                                    color:#ffffff;text-decoration:none;
                                    font-size:15px;font-weight:700;
                                    white-space:nowrap;">
                            Reset My Password →
                          </a>
                        </td>
                      </tr>
                    </table>

                    <!-- Fallback link -->
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;
                                padding:16px 20px;margin-bottom:24px;">
                      <p style="margin:0 0 6px;color:#64748b;font-size:12px;font-weight:600;">
                        Button not working? Paste this link in your browser:
                      </p>
                      <p style="margin:0;color:#0d9488;font-size:12px;word-break:break-all;
                                line-height:1.5;">
                        %s
                      </p>
                    </div>

                    <!-- Security notice -->
                    <div style="background:#fef9c3;border:1px solid #fde68a;border-radius:10px;
                                padding:14px 18px;">
                      <p style="margin:0;color:#92400e;font-size:12px;line-height:1.6;">
                        ⚠️ <strong>Didn't request this?</strong> You can safely ignore this email —
                        your password will not change unless you click the link above.
                      </p>
                    </div>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;
                             padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.8;">
                      This is an automated message from BioLabs Platform.<br/>
                      Questions? Contact
                      <a href="mailto:support@biolab.com"
                         style="color:#0d9488;text-decoration:none;">support@biolab.com</a>
                    </p>
                    <p style="margin:6px 0 0;color:#cbd5e1;font-size:11px;">
                      © 2026 BioLabs. All rights reserved.
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(firstName, expiryMin, resetLink, resetLink);
    }

    /**
     * Inline HTML for the password-change confirmation / security alert email.
     *
     * @param firstName user's first name
     */
    private String buildPasswordChangedHtml(String firstName) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta name="viewport" content="width=device-width,initial-scale=1"/>
          <title>Password Changed — BioLabs</title>
        </head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#ffffff;border-radius:16px;overflow:hidden;
                            box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">

                <!-- Header -->
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);
                             padding:32px 40px;text-align:center;">
                    <h1 style="margin:0;color:#ffffff;font-size:22px;font-weight:800;">
                      🧪 BioLabs
                    </h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">
                      Security Notification
                    </p>
                  </td>
                </tr>

                <!-- Body -->
                <tr>
                  <td style="padding:40px;">
                    <!-- Success icon -->
                    <div style="text-align:center;margin-bottom:20px;">
                      <div style="display:inline-block;background:#dcfce7;
                                  border:2px solid #bbf7d0;border-radius:50%%;
                                  width:64px;height:64px;line-height:60px;
                                  text-align:center;font-size:28px;">✓</div>
                    </div>

                    <h2 style="margin:0 0 12px;color:#0f172a;font-size:20px;font-weight:800;
                               text-align:center;">
                      Password Changed Successfully
                    </h2>
                    <p style="margin:0 0 8px;color:#475569;font-size:14px;line-height:1.6;
                              text-align:center;">
                      Hi <strong style="color:#0f172a;">%s</strong>,
                    </p>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;
                              text-align:center;">
                      Your BioLabs account password was successfully changed.
                      All active sessions have been signed out for your security.
                    </p>

                    <!-- Security alert -->
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:10px;
                                padding:16px 20px;">
                      <p style="margin:0;color:#991b1b;font-size:13px;line-height:1.6;">
                        🔒 <strong>Wasn't you?</strong> If you did not make this change,
                        your account may be compromised. Please
                        <a href="mailto:support@biolab.com"
                           style="color:#dc2626;text-decoration:underline;font-weight:600;">
                          contact support immediately
                        </a>
                        and reset your password right away.
                      </p>
                    </div>
                  </td>
                </tr>

                <!-- Footer -->
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;
                             padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;line-height:1.8;">
                      © 2026 BioLabs. All rights reserved. |
                      <a href="mailto:support@biolab.com"
                         style="color:#0d9488;text-decoration:none;">support@biolab.com</a>
                    </p>
                  </td>
                </tr>

              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(firstName);
    }
}