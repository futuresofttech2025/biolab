package com.biolab.auth.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email delivery service — sends HTML transactional emails.
 *
 * <p>All sends are {@code @Async} so they never block the HTTP request thread.
 * Failures are logged but not surfaced to the caller — the user always gets
 * a "check your inbox" message and can request a resend.</p>
 *
 * @author BioLab Engineering Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from:noreply@biolab.com}")
    private String fromAddress;

    @Value("${app.email.from-name:BioLabs Platform}")
    private String fromName;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    // ─── Email Verification ───────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String toEmail, String firstName, String rawToken) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + rawToken;
        String subject   = "Verify your BioLabs account";
        String html      = buildVerificationHtml(firstName, verifyUrl);
        send(toEmail, subject, html);
    }

    // ─── Password Reset (bonus — wires up the stubbed forgotPassword flow) ──

    @Async
    public void sendPasswordResetEmail(String toEmail, String firstName, String rawToken) {
        String resetUrl = frontendUrl + "/reset-password?token=" + rawToken;
        String subject  = "Reset your BioLabs password";
        String html     = buildPasswordResetHtml(firstName, resetUrl);
        send(toEmail, subject, html);
    }

    // ─── MFA Email Code ──────────────────────────────────────────────────

    @Async
    public void sendMfaCode(String toEmail, String firstName, String code) {
        String subject = "Your BioLabs verification code: " + code;
        String html = buildMfaCodeHtml(firstName, code);
        send(toEmail, subject, html);
    }

    // ─── Core send ────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        try {
            log.debug("Sending email — from={} to={} subject=\"{}\"", fromAddress, to, subject);
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(msg);
            log.info("Email sent — from={} to={} subject=\"{}\"", fromAddress, to, subject);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email — to={} subject=\"{}\" error={}", to, subject, e.getMessage());
        }
    }

    // ─── HTML Templates ──────────────────────────────────────────────────

    private String buildVerificationHtml(String firstName, String verifyUrl) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:40px 16px\">" +
                "<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)\">" +
                // Header
                "<tr><td style=\"background:linear-gradient(135deg,#0d9488,#059669);padding:36px 40px;text-align:center\">" +
                "<div style=\"font-size:28px;font-weight:900;color:#ffffff;letter-spacing:-0.5px\">🧪 BioLabs</div>" +
                "<div style=\"color:rgba(255,255,255,.8);font-size:13px;margin-top:6px\">Secure Biotech Services Platform</div>" +
                "</td></tr>" +
                // Body
                "<tr><td style=\"padding:40px\">" +
                "<h2 style=\"color:#0f172a;font-size:22px;margin:0 0 12px\">Hi " + escapeHtml(firstName) + ", verify your email</h2>" +
                "<p style=\"color:#475569;font-size:15px;line-height:1.6;margin:0 0 28px\">" +
                "Thanks for joining BioLabs. Click the button below to verify your email address and activate your account." +
                "</p>" +
                "<div style=\"text-align:center;margin:32px 0\">" +
                "<a href=\"" + verifyUrl + "\" style=\"display:inline-block;background:linear-gradient(135deg,#0d9488,#059669);color:#ffffff;font-weight:700;font-size:15px;text-decoration:none;padding:14px 36px;border-radius:10px\">Verify Email Address</a>" +
                "</div>" +
                "<p style=\"color:#94a3b8;font-size:13px;line-height:1.5\">Or paste this link into your browser:<br>" +
                "<a href=\"" + verifyUrl + "\" style=\"color:#0d9488;word-break:break-all\">" + verifyUrl + "</a></p>" +
                "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:28px 0\">" +
                "<p style=\"color:#94a3b8;font-size:12px;margin:0\">This link expires in <strong>24 hours</strong>. If you did not create a BioLabs account, you can safely ignore this email.</p>" +
                "</td></tr>" +
                // Footer
                "<tr><td style=\"background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0\">" +
                "<p style=\"color:#94a3b8;font-size:11px;margin:0\">BioLabs Platform · HIPAA · GDPR · FDA 21 CFR Part 11</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    private String buildPasswordResetHtml(String firstName, String resetUrl) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:40px 16px\">" +
                "<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)\">" +
                "<tr><td style=\"background:linear-gradient(135deg,#0d9488,#059669);padding:36px 40px;text-align:center\">" +
                "<div style=\"font-size:28px;font-weight:900;color:#ffffff\">🧪 BioLabs</div>" +
                "<div style=\"color:rgba(255,255,255,.8);font-size:13px;margin-top:6px\">Secure Biotech Services Platform</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:40px\">" +
                "<h2 style=\"color:#0f172a;font-size:22px;margin:0 0 12px\">Hi " + escapeHtml(firstName) + ", reset your password</h2>" +
                "<p style=\"color:#475569;font-size:15px;line-height:1.6;margin:0 0 28px\">We received a request to reset your password. Click below to choose a new one.</p>" +
                "<div style=\"text-align:center;margin:32px 0\">" +
                "<a href=\"" + resetUrl + "\" style=\"display:inline-block;background:linear-gradient(135deg,#0d9488,#059669);color:#ffffff;font-weight:700;font-size:15px;text-decoration:none;padding:14px 36px;border-radius:10px\">Reset Password</a>" +
                "</div>" +
                "<p style=\"color:#94a3b8;font-size:13px;line-height:1.5\">Or paste this link:<br>" +
                "<a href=\"" + resetUrl + "\" style=\"color:#0d9488;word-break:break-all\">" + resetUrl + "</a></p>" +
                "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:28px 0\">" +
                "<p style=\"color:#94a3b8;font-size:12px;margin:0\">This link expires in <strong>15 minutes</strong>. If you did not request a password reset, ignore this email.</p>" +
                "</td></tr>" +
                "<tr><td style=\"background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0\">" +
                "<p style=\"color:#94a3b8;font-size:11px;margin:0\">BioLabs Platform · HIPAA · GDPR · FDA 21 CFR Part 11</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String buildMfaCodeHtml(String firstName, String code) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Arial,sans-serif\">" +
                "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr><td align=\"center\" style=\"padding:40px 16px\">" +
                "<table width=\"560\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,.08)\">" +
                "<tr><td style=\"background:linear-gradient(135deg,#0d9488,#059669);padding:36px 40px;text-align:center\">" +
                "<div style=\"font-size:28px;font-weight:900;color:#ffffff\">&#x1F9EA; BioLabs</div>" +
                "<div style=\"color:rgba(255,255,255,.8);font-size:13px;margin-top:6px\">Secure Biotech Services Platform</div>" +
                "</td></tr>" +
                "<tr><td style=\"padding:40px\">" +
                "<h2 style=\"color:#0f172a;font-size:22px;margin:0 0 12px\">Hi " + escapeHtml(firstName) + ", here's your verification code</h2>" +
                "<p style=\"color:#475569;font-size:15px;line-height:1.6;margin:0 0 28px\">" +
                "Use the code below to complete your sign-in. This code is valid for <strong>90 seconds</strong>.</p>" +
                "<div style=\"text-align:center;margin:32px 0\">" +
                "<div style=\"display:inline-block;background:#f0fdfa;border:2px solid #0d9488;border-radius:12px;padding:20px 40px\">" +
                "<span style=\"font-size:36px;font-weight:900;letter-spacing:8px;color:#0d9488;font-family:monospace\">" + code + "</span>" +
                "</div></div>" +
                "<p style=\"color:#94a3b8;font-size:13px;line-height:1.5;text-align:center\">" +
                "If you didn't try to sign in, someone may be trying to access your account. " +
                "Change your password immediately.</p>" +
                "<hr style=\"border:none;border-top:1px solid #e2e8f0;margin:28px 0\">" +
                "<p style=\"color:#94a3b8;font-size:12px;margin:0\">Do not share this code with anyone. BioLabs staff will never ask for your verification code.</p>" +
                "</td></tr>" +
                "<tr><td style=\"background:#f8fafc;padding:20px 40px;text-align:center;border-top:1px solid #e2e8f0\">" +
                "<p style=\"color:#94a3b8;font-size:11px;margin:0\">BioLabs Platform · HIPAA · GDPR · FDA 21 CFR Part 11</p>" +
                "</td></tr>" +
                "</table></td></tr></table></body></html>";
    }
}