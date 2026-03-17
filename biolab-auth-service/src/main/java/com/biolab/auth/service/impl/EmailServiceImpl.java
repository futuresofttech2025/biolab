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
 * SMTP-backed implementation of {@link EmailService}.
 *
 * <h3>Sprint 0 — GAP-03 (FIX-18):</h3>
 * <p>The stale duplicate {@code com.biolab.auth.email.EmailService} class
 * has been deleted. This class is now the <strong>sole</strong> email
 * implementation, implementing the canonical
 * {@code com.biolab.auth.service.EmailService} interface which now also
 * exposes {@link #sendVerificationEmail}.</p>
 *
 * <p>All methods are {@code @Async} — emails never block the HTTP thread.
 * Failures are caught and logged, never re-thrown to the caller.</p>
 *
 * @author BioLab Engineering Team
 * @version 1.3.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@biolab.com}")
    private String fromAddress;

    @Value("${app.mail.from-name:BioLabs Platform}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ── Public API ────────────────────────────────────────────────────────

    @Async
    @Override
    public void sendVerificationEmail(String toEmail, String firstName, String rawToken) {
        log.info("Sending verification email to: {}", toEmail);
        String verifyUrl = frontendUrl + "/verify-email?token=" + rawToken;
        try {
            sendHtmlEmail(toEmail,
                    "Verify your BioLabs account",
                    buildVerificationHtml(firstName, verifyUrl));
            log.info("Verification email delivered to: {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send verification email to {}: {}", toEmail, ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordResetEmail(String toEmail,
                                       String firstName,
                                       String resetLink,
                                       int    expiryMin) {
        log.info("Sending password-reset email to: {}", toEmail);
        try {
            sendHtmlEmail(toEmail,
                    "Reset your BioLabs password",
                    buildResetEmailHtml(firstName, resetLink, expiryMin));
            log.info("Password-reset email delivered to: {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send password-reset email to {}: {}", toEmail, ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendPasswordChangedEmail(String toEmail, String firstName) {
        log.info("Sending password-changed confirmation to: {}", toEmail);
        try {
            sendHtmlEmail(toEmail,
                    "Your BioLabs password has been changed",
                    buildPasswordChangedHtml(firstName));
            log.info("Password-changed email delivered to: {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send password-changed email to {}: {}", toEmail, ex.getMessage());
        }
    }

    @Async
    @Override
    public void sendMfaCode(String toEmail, String firstName, String code) {
        log.info("Sending MFA code email to: {}", toEmail);
        try {
            sendHtmlEmail(toEmail,
                    "Your BioLabs verification code: " + code,
                    buildMfaCodeHtml(firstName, code));
            log.info("MFA code email delivered to: {}", toEmail);
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Failed to send MFA code email to {}: {}", toEmail, ex.getMessage());
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
        helper.setText(htmlBody, true);
        mailSender.send(msg);
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ── HTML Templates ───────────────────────────────────────────────────

    private String buildVerificationHtml(String firstName, String verifyUrl) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#fff;border-radius:16px;overflow:hidden;
                            box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);padding:32px 40px;text-align:center;">
                    <h1 style="margin:0;color:#fff;font-size:22px;font-weight:800;">&#x1F9EA; BioLabs</h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">Biotech Services Platform</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px 40px 32px;">
                    <h2 style="margin:0 0 16px;color:#0f172a;font-size:20px;font-weight:800;">Verify your email address</h2>
                    <p style="margin:0 0 8px;color:#475569;font-size:14px;line-height:1.6;">
                      Hi <strong>%s</strong>,
                    </p>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;">
                      Thanks for joining BioLabs. Click the button below to verify your email and activate your account.
                      This link expires in <strong>24 hours</strong>.
                    </p>
                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                      <tr><td style="background:linear-gradient(135deg,#0d9488,#059669);border-radius:10px;">
                        <a href="%s" style="display:inline-block;padding:14px 36px;color:#fff;text-decoration:none;font-size:15px;font-weight:700;">
                          Verify Email Address →
                        </a>
                      </td></tr>
                    </table>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:16px 20px;">
                      <p style="margin:0 0 6px;color:#64748b;font-size:12px;font-weight:600;">Or paste this link:</p>
                      <p style="margin:0;color:#0d9488;font-size:12px;word-break:break-all;">%s</p>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;">© 2026 BioLabs · HIPAA · GDPR · FDA 21 CFR Part 11</p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(esc(firstName), verifyUrl, verifyUrl);
    }

    private String buildResetEmailHtml(String firstName, String resetLink, int expiryMin) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#fff;border-radius:16px;overflow:hidden;
                            box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);padding:32px 40px;text-align:center;">
                    <h1 style="margin:0;color:#fff;font-size:22px;font-weight:800;">&#x1F9EA; BioLabs</h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">Biotech Services Platform</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px 40px 32px;">
                    <h2 style="margin:0 0 16px;color:#0f172a;font-size:20px;font-weight:800;">Password Reset Request</h2>
                    <p style="margin:0 0 8px;color:#475569;font-size:14px;line-height:1.6;">
                      Hi <strong>%s</strong>,
                    </p>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;">
                      Click the button below to reset your password. This link expires in <strong>%d minutes</strong>.
                    </p>
                    <table cellpadding="0" cellspacing="0" style="margin:0 auto 28px;">
                      <tr><td style="background:linear-gradient(135deg,#0d9488,#059669);border-radius:10px;">
                        <a href="%s" style="display:inline-block;padding:14px 36px;color:#fff;text-decoration:none;font-size:15px;font-weight:700;">
                          Reset My Password →
                        </a>
                      </td></tr>
                    </table>
                    <div style="background:#f8fafc;border:1px solid #e2e8f0;border-radius:10px;padding:16px 20px;margin-bottom:24px;">
                      <p style="margin:0 0 6px;color:#64748b;font-size:12px;font-weight:600;">Or paste this link:</p>
                      <p style="margin:0;color:#0d9488;font-size:12px;word-break:break-all;">%s</p>
                    </div>
                    <div style="background:#fef9c3;border:1px solid #fde68a;border-radius:10px;padding:14px 18px;">
                      <p style="margin:0;color:#92400e;font-size:12px;line-height:1.6;">
                        &#x26A0;&#xFE0F; <strong>Didn't request this?</strong> Ignore this email — your password won't change.
                      </p>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;">© 2026 BioLabs · HIPAA · GDPR · FDA 21 CFR Part 11</p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(esc(firstName), expiryMin, resetLink, resetLink);
    }

    private String buildPasswordChangedHtml(String firstName) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);padding:32px 40px;text-align:center;">
                    <h1 style="margin:0;color:#fff;font-size:22px;font-weight:800;">&#x1F9EA; BioLabs</h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">Security Notification</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px;text-align:center;">
                    <div style="background:#dcfce7;border:2px solid #bbf7d0;border-radius:50%%;width:64px;height:64px;line-height:60px;font-size:28px;margin:0 auto 20px;">✓</div>
                    <h2 style="margin:0 0 12px;color:#0f172a;font-size:20px;font-weight:800;">Password Changed</h2>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;">
                      Hi <strong>%s</strong>, your password was changed successfully.
                      All active sessions have been signed out.
                    </p>
                    <div style="background:#fef2f2;border:1px solid #fecaca;border-radius:10px;padding:16px 20px;text-align:left;">
                      <p style="margin:0;color:#991b1b;font-size:13px;line-height:1.6;">
                        &#x1F512; <strong>Wasn't you?</strong>
                        <a href="mailto:support@biolab.com" style="color:#dc2626;text-decoration:underline;">Contact support immediately</a>.
                      </p>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;">© 2026 BioLabs. All rights reserved.</p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(esc(firstName));
    }

    private String buildMfaCodeHtml(String firstName, String code) {
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head><meta charset="UTF-8"/></head>
        <body style="margin:0;padding:0;background:#f1f5f9;font-family:'Segoe UI',Arial,sans-serif;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f1f5f9;padding:40px 16px;">
            <tr><td align="center">
              <table width="600" cellpadding="0" cellspacing="0"
                     style="background:#fff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);max-width:600px;width:100%%;">
                <tr>
                  <td style="background:linear-gradient(135deg,#0d9488,#059669);padding:32px 40px;text-align:center;">
                    <h1 style="margin:0;color:#fff;font-size:22px;font-weight:800;">&#x1F9EA; BioLabs</h1>
                    <p style="margin:4px 0 0;color:rgba(255,255,255,0.75);font-size:13px;">Secure Verification</p>
                  </td>
                </tr>
                <tr>
                  <td style="padding:40px;">
                    <h2 style="margin:0 0 12px;color:#0f172a;font-size:20px;font-weight:800;">
                      Hi %s, here's your verification code
                    </h2>
                    <p style="margin:0 0 28px;color:#475569;font-size:14px;line-height:1.6;">
                      Use the code below to complete your sign-in. Valid for <strong>90 seconds</strong>.
                    </p>
                    <div style="text-align:center;margin:32px 0;">
                      <div style="display:inline-block;background:#f0fdfa;border:2px solid #0d9488;border-radius:12px;padding:20px 40px;">
                        <span style="font-size:36px;font-weight:900;letter-spacing:8px;color:#0d9488;font-family:monospace;">%s</span>
                      </div>
                    </div>
                    <p style="margin:0;color:#94a3b8;font-size:13px;line-height:1.5;text-align:center;">
                      Do not share this code. BioLabs staff will never ask for your verification code.
                    </p>
                  </td>
                </tr>
                <tr>
                  <td style="background:#f8fafc;border-top:1px solid #e2e8f0;padding:20px 40px;text-align:center;">
                    <p style="margin:0;color:#94a3b8;font-size:11px;">© 2026 BioLabs · HIPAA · GDPR · FDA 21 CFR Part 11</p>
                  </td>
                </tr>
              </table>
            </td></tr>
          </table>
        </body>
        </html>
        """.formatted(esc(firstName), code);
    }
}
