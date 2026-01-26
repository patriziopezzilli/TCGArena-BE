package com.tcg.arena.service;

import com.tcg.arena.dto.DailyDigestEmailDTO;
import com.tcg.arena.dto.ImportSummaryEmailDTO;
import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final org.springframework.context.MessageSource messageSource;

    @Value("${spring.mail.username:noreply@tcgarena.com}")
    private String fromEmail;

    @Value("${app.frontend.url:https://tcgarena.com}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine,
            org.springframework.context.MessageSource messageSource) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.messageSource = messageSource;
    }

    /**
     * Helper to get localized message
     */
    private String getMessage(String key, Object[] args, String localeStr) {
        java.util.Locale locale = java.util.Locale.ITALIAN; // Default to Italian
        if (localeStr != null) {
            try {
                locale = new java.util.Locale(localeStr);
            } catch (Exception e) {
                logger.warn("Invalid locale '{}', falling back to Italian", localeStr);
            }
        }
        return messageSource.getMessage(key, args, locale);
    }

    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        // Check if email is an Apple private relay address
        if (isApplePrivateRelayEmail(to)) {
            logger.info("Skipping email to Apple private relay address: {} (subject: {})", to, subject);
            return; // Don't attempt to send to Apple relay addresses
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            logger.info("Simple email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send HTML email using Thymeleaf template
     */
    public void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        // Check if email is an Apple private relay address
        if (isApplePrivateRelayEmail(to)) {
            logger.info("Skipping HTML email to Apple private relay address: {} (template: {})", to, templateName);
            return; // Don't attempt to send to Apple relay addresses
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            // Use MimeMessageHelper with explicit encoding for headers
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);

            // Sanitize subject to prevent SMTP errors
            String sanitizedSubject = sanitizeForSmtp(subject);
            helper.setSubject(sanitizedSubject);

            // Sanitize all string variables to prevent SMTP errors
            Map<String, Object> sanitizedVariables = sanitizeVariables(variables);

            // Process template
            Context context = new Context();
            context.setVariables(sanitizedVariables);
            context.setVariable("frontendUrl", frontendUrl);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("HTML email sent to: {} using template: {}", to, templateName);
        } catch (MessagingException e) {
            logger.error("Failed to send HTML email to: {}", to, e);
            throw new RuntimeException("Failed to send HTML email", e);
        }
    }

    /**
     * Send OTP for password reset (simple text version)
     */
    public void sendOtpEmail(String toEmail, String otp) {
        // Check if email is an Apple private relay address
        if (isApplePrivateRelayEmail(toEmail)) {
            logger.info("Skipping OTP email to Apple private relay address: {}", toEmail);
            return; // Don't attempt to send OTP to Apple relay addresses
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("TCG Arena - Password Reset Code");
            message.setText(String.format(
                    "Il tuo codice di verifica per il reset della password è:\n\n%s\n\n" +
                            "Questo codice scadrà tra 15 minuti.\n\n" +
                            "Se non hai richiesto il reset della password, ignora questa email.\n\n" +
                            "TCG Arena Team",
                    otp));

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send OTP for password reset (HTML version)
     */
    public void sendOtpEmailHtml(String toEmail, String otp, String username) { // Fallback if user obj not avail:
                                                                                // assume 'it' or pass locale
        // If possible, this signature should accept User or Locale string.
        // For now, we default to 'it' unless we query the user here, but that's
        // expensive.
        // Assuming this is used where we might not have full user context yet (e.g.
        // forgot password),
        // or we can fetch user by email if needed.
        // Let's assume 'it' for safety if just email is passed, or update controller to
        // pass locale.

        Map<String, Object> variables = Map.of(
                "username", username,
                "otp", otp,
                "expirationMinutes", 15);
        // Defaulting to IT for now as this method signature doesn't have locale
        String subject = getMessage("email.subject.password_reset", null, "it");
        sendHtmlEmail(toEmail, subject, "email/password-reset", variables);
    }

    // ===== TRANSACTIONAL EMAILS =====

    /**
     * Check if user has enabled email notifications (general flag)
     * Also checks if email is an Apple private relay address (which cannot receive
     * emails)
     */
    private boolean shouldSendEmail(User user) {
        // First check if user has email notifications enabled
        boolean notificationsEnabled = user.getEmailNotificationsEnabled() != null ? user.getEmailNotificationsEnabled()
                : true;
        if (!notificationsEnabled) {
            return false;
        }

        // Check if email is an Apple private relay address
        return !isApplePrivateRelayEmail(user.getEmail());
    }

    /**
     * Send welcome email to new user
     */
    public void sendWelcomeEmail(User user) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping welcome email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());

        String subject = getMessage("email.subject.welcome", null, user.getLocale());
        // For welcome email, we might want localized templates in future,
        // e.g., "email/welcome_" + user.getLocale()
        // For now, using the standard one but we could pass locale to context
        variables.put("locale", user.getLocale());

        sendHtmlEmail(user.getEmail(), subject, "email/welcome", variables);
    }

    /**
     * Send tournament registration confirmation
     */
    public void sendTournamentRegistration(User user, CommunityEvent event) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping tournament registration email for user {} - email notifications disabled",
                    user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("eventName", event.getTitle());
        variables.put("eventDate", event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        variables.put("eventLocation", event.getLocationName());
        variables.put("eventId", event.getId());

        String subject = getMessage("email.subject.tournament_registration", new Object[] { event.getTitle() },
                user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/tournament-registration", variables);
    }

    /**
     * Send tournament registration confirmation (for non-registered users)
     */
    public void sendTournamentRegistration(String email, String username, CommunityEvent event) {
        // For non-registered users, we don't have preferences to check, so send the
        // email
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("eventName", event.getTitle());
        variables.put("eventDate", event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        variables.put("eventLocation", event.getLocationName());
        variables.put("eventId", event.getId());
        sendHtmlEmail(email, "Iscrizione Torneo Confermata - " + event.getTitle(), "email/tournament-registration",
                variables);
    }

    /**
     * Send card reservation with QR code
     */
    public void sendCardReservation(User user, String cardName, String shopName, String qrCodeUrl) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping card reservation email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("cardName", cardName);
        variables.put("shopName", shopName);
        variables.put("qrCodeUrl", qrCodeUrl);

        String subject = getMessage("email.subject.card_reservation", new Object[] { cardName }, user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/card-reservation", variables);
    }

    /**
     * Send inactivity reminder
     */
    public void sendInactivityReminder(User user, int daysInactive) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping inactivity reminder for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("daysInactive", daysInactive);

        String subject = getMessage("email.subject.inactivity_reminder", null, user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/inactivity-reminder", variables);
    }

    // ===== NIGHTLY BATCH EMAILS =====

    /**
     * Send JustTCG import summary email
     */
    public void sendImportSummary(String email, ImportSummaryEmailDTO summary) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("summary", summary);
        variables.put("username", summary.getUsername());
        variables.put("status", summary.getStatus());
        variables.put("totalCards", summary.getTotalCardsProcessed());
        variables.put("cardsAdded", summary.getCardsAdded());
        variables.put("cardsUpdated", summary.getCardsUpdated());
        variables.put("cardsSkipped", summary.getCardsSkipped());
        variables.put("errors", summary.getErrors());
        variables.put("deltas", summary.getDeltas());
        variables.put("durationMinutes", summary.getDurationMinutes());
        variables.put("errorMessage", summary.getErrorMessage());

        String subject = summary.getStatus().equals("SUCCESS")
                ? "Import TCG Completato - " + summary.getCardsAdded() + " carte aggiunte"
                : "Import TCG - Stato: " + summary.getStatus();

        sendHtmlEmail(email, subject, "email/import-summary", variables);
    }

    /**
     * Send daily digest email
     */
    public void sendDailyDigest(String email, DailyDigestEmailDTO digest) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("digest", digest);
        variables.put("username", digest.getUsername());
        variables.put("digestDate", digest.getDigestDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));
        variables.put("userStats", digest.getUserStats());
        variables.put("platformStats", digest.getPlatformStats());
        variables.put("highlights", digest.getHighlights());
        variables.put("recommendations", digest.getRecommendations());

        sendHtmlEmail(email, "Il tuo riepilogo TCG Arena giornaliero", "email/daily-digest", variables);
    }

    // ===== TRADE NOTIFICATIONS =====

    /**
     * Send trade request notification
     */
    public void sendTradeRequest(String receiverEmail, String receiverUsername, String senderUsername,
            Long tradeId, String offeredCards, String requestedCards) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("receiverUsername", receiverUsername);
        variables.put("senderUsername", senderUsername);
        variables.put("tradeId", tradeId);
        variables.put("offeredCards", offeredCards);
        variables.put("requestedCards", requestedCards);

        // We only have email here, not User object. Ideally call should pass locale.
        // For backward compatibility, default to 'it' if we can't easily fetch user.
        // Or better: update signature later. For now: default 'it'.
        String subject = getMessage("email.subject.trade_request", new Object[] { senderUsername }, "it");

        sendHtmlEmail(receiverEmail, subject, "email/trade-request", variables);
    }

    /**
     * Send trade accepted notification
     */
    public void sendTradeAccepted(User user, String partnerUsername, Long tradeId) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping trade accepted email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("partnerUsername", partnerUsername);
        variables.put("tradeId", tradeId);

        String subject = getMessage("email.subject.trade_accepted", null, user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/trade-accepted", variables);
    }

    /**
     * Send trade completed notification
     */
    public void sendTradeCompleted(User user, String partnerUsername, Long tradeId) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping trade completed email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("partnerUsername", partnerUsername);
        variables.put("tradeId", tradeId);

        String subject = getMessage("email.subject.trade_completed", null, user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/trade-completed", variables);
    }

    // ===== EVENT NOTIFICATIONS =====

    /**
     * Send event reminder (24h before)
     */
    public void sendEventReminder(User user, CommunityEvent event) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping event reminder email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("eventTitle", event.getTitle());
        variables.put("eventDate", event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        variables.put("eventTime", event.getEventDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        variables.put("eventLocation", event.getLocationName());
        variables.put("eventId", event.getId());
        if (event.getShop() != null) {
            variables.put("eventShopName", event.getShop().getName());
        }

        String subject = getMessage("email.subject.event_reminder", new Object[] { event.getTitle() },
                user.getLocale());
        sendHtmlEmail(user.getEmail(), subject, "email/event-reminder", variables);
    }

    /**
     * Send event cancelled notification
     */
    public void sendEventCancelled(User user, String eventTitle, String eventDate,
            String eventLocation, String cancellationReason) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping event cancelled email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("eventTitle", eventTitle);
        variables.put("eventDate", eventDate);
        variables.put("eventLocation", eventLocation);
        variables.put("cancellationReason", cancellationReason);
        sendHtmlEmail(user.getEmail(), "Evento Cancellato: " + eventTitle, "email/event-cancelled", variables);
    }

    /**
     * Send event updated notification
     */
    public void sendEventUpdated(User user, String eventTitle, Long eventId,
            String newDate, String newTime, String newLocation, String updateNote) {
        if (!shouldSendEmail(user)) {
            logger.debug("Skipping event updated email for user {} - email notifications disabled", user.getId());
            return;
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("eventTitle", eventTitle);
        variables.put("eventId", eventId);
        if (newDate != null)
            variables.put("newDate", newDate);
        if (newTime != null)
            variables.put("newTime", newTime);
        if (newLocation != null)
            variables.put("newLocation", newLocation);
        if (updateNote != null)
            variables.put("updateNote", updateNote);
        sendHtmlEmail(user.getEmail(), "Evento Aggiornato: " + eventTitle, "email/event-updated", variables);
    }

    // ===== SECURITY & SHOP NOTIFICATIONS =====

    /**
     * Send security alert for new device login
     */
    public void sendSecurityAlert(String email, String username, String loginTime, String deviceInfo,
            String location, String ipAddress) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("loginTime", loginTime);
        variables.put("deviceInfo", deviceInfo);
        variables.put("location", location);
        variables.put("ipAddress", ipAddress);
        sendHtmlEmail(email, "Nuovo accesso rilevato al tuo account", "email/security-alert", variables);
    }

    /**
     * Send shop approved notification
     */
    public void sendShopApproved(String email, String ownerName, String shopName, Long shopId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ownerName", ownerName);
        variables.put("shopName", shopName);
        variables.put("shopId", shopId);
        sendHtmlEmail(email, "Negozio Approvato: " + shopName, "email/shop-approved", variables);
    }

    /**
     * Send shop rejected notification
     */
    public void sendShopRejected(String email, String ownerName, String shopName, String rejectionReason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ownerName", ownerName);
        variables.put("shopName", shopName);
        variables.put("rejectionReason", rejectionReason);
        sendHtmlEmail(email, "Richiesta Negozio - " + shopName, "email/shop-rejected", variables);
    }

    /**
     * Send email verification
     */
    public void sendEmailVerification(String email, String username, String verificationCode, String verificationLink) {
        // Check if email is an Apple private relay address
        if (isApplePrivateRelayEmail(email)) {
            logger.info("Skipping email verification to Apple private relay address: {} for user: {}", email, username);
            return; // Don't attempt to send verification to Apple relay addresses
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("verificationCode", verificationCode);
        variables.put("verificationLink", verificationLink);
        sendHtmlEmail(email, "Verifica la tua email - TCG Arena", "email/email-verification", variables);
    }

    // ===== UTILITY METHODS =====

    /**
     * Sanitize string for SMTP to prevent "555 syntax error"
     * Removes control characters and special characters that can cause issues
     * Gmail SMTP requires proper ASCII or encoded subjects
     */
    private String sanitizeForSmtp(String input) {
        if (input == null) {
            return "";
        }

        // Remove all emojis and special unicode characters that cause SMTP issues
        // Keep only ASCII-safe characters for email headers
        String sanitized = input
                // Remove emojis and pictographs
                .replaceAll("[\\x{1F300}-\\x{1F9FF}]", "")
                // Remove other symbols and pictographs
                .replaceAll("[\\x{2600}-\\x{26FF}]", "")
                .replaceAll("[\\x{2700}-\\x{27BF}]", "")
                // Remove control characters except newline and tab
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "")
                // Trim extra spaces
                .replaceAll("\\s+", " ")
                .trim();

        // Limit length for email subjects (recommended max 78 characters)
        if (sanitized.length() > 78) {
            sanitized = sanitized.substring(0, 75) + "...";
        }

        return sanitized;
    }

    /**
     * Recursively sanitize all string values in a map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> sanitizeVariables(Map<String, Object> variables) {
        Map<String, Object> sanitized = new HashMap<>();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                // Sanitize string values (less strict than headers)
                String stringValue = (String) value;
                // Only remove non-printable control characters
                String cleaned = stringValue.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
                sanitized.put(entry.getKey(), cleaned);
            } else if (value instanceof Map) {
                // Recursively sanitize nested maps
                sanitized.put(entry.getKey(), sanitizeVariables((Map<String, Object>) value));
            } else {
                // Keep other types as-is (numbers, booleans, objects, etc.)
                sanitized.put(entry.getKey(), value);
            }
        }

        return sanitized;
    }

    /**
     * Check if the email address is an Apple Private Relay address
     * Apple Private Relay addresses have the domain privaterelay.appleid.com
     * These addresses cannot receive emails from external senders
     */
    private boolean isApplePrivateRelayEmail(String email) {
        if (email == null) {
            return false;
        }
        return email.toLowerCase().endsWith("@privaterelay.appleid.com");
    }
}
