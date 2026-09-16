package com.habify.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * MailService
 * 
 * Dispatches account verification and password reset emails.
 * Gracefully falls back to console logging when SMTP credentials are not configured,
 * ensuring effortless testing and development in offline/local environments.
 */
@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${habify.app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    /**
     * Sends account verification link to user.
     */
    public void sendVerificationEmail(String recipientEmail, String token) {
        String verifyUrl = appBaseUrl + "/auth.html?verifyToken=" + token + "#login";
        String subject = "Verify your Habify Account";
        String message = "Welcome to Habify!\n\n"
                + "Please verify your email address to unlock your full analytics and weekly reports by clicking the link below:\n"
                + verifyUrl + "\n\n"
                + "If you did not register for Habify, please ignore this email.";

        sendEmailOrLog(recipientEmail, subject, message, "EMAIL VERIFICATION", verifyUrl);
    }

    /**
     * Sends password reset link with single-use token.
     */
    public void sendPasswordResetEmail(String recipientEmail, String token) {
        String resetUrl = appBaseUrl + "/auth.html?resetToken=" + token + "#reset-password";
        String subject = "Reset your Habify Password";
        String message = "Hello,\n\n"
                + "You requested a password reset for your Habify account. Click the link below to set a new password:\n"
                + resetUrl + "\n\n"
                + "This link will expire in 15 minutes. If you did not request this, please disregard this email.";

        sendEmailOrLog(recipientEmail, subject, message, "PASSWORD RESET", resetUrl);
    }

    private void sendEmailOrLog(String recipientEmail, String subject, String text, String purpose, String actionUrl) {
        boolean canSendSmtp = mailSender != null && mailUsername != null && !mailUsername.trim().isEmpty();

        if (canSendSmtp) {
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom(mailUsername);
                mailMessage.setTo(recipientEmail);
                mailMessage.setSubject(subject);
                mailMessage.setText(text);
                mailSender.send(mailMessage);
                log.info("[SMTP SUCCESS] Dispatched {} email to {}", purpose, recipientEmail);
                return;
            } catch (Exception e) {
                log.warn("[SMTP FALLBACK] Failed to dispatch {} email via SMTP ({}). Falling back to console log.", purpose, e.getMessage());
            }
        }

        // Local development console fallback - output real actionable link for local development & testing
        log.info("================================================================================");
        log.info("[HABIFY DEV EMAIL SIMULATION] Action: {}", purpose);
        log.info("To: {}", recipientEmail);
        log.info("Subject: {}", subject);
        log.info("Action Link (Open in Browser): {}", actionUrl);
        log.info("================================================================================");
    }
}
