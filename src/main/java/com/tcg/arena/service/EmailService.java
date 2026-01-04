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

    @Value("${spring.mail.username:noreply@tcgarena.com}")
    private String fromEmail;
    
    @Value("${app.frontend.url:https://tcgarena.com}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    /**
     * Send simple text email
     */
    public void sendSimpleEmail(String to, String subject, String text) {
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
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Process template
            Context context = new Context();
            context.setVariables(variables);
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
                otp
            ));

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
    public void sendOtpEmailHtml(String toEmail, String otp, String username) {
        Map<String, Object> variables = Map.of(
            "username", username,
            "otp", otp,
            "expirationMinutes", 15
        );
        sendHtmlEmail(toEmail, "TCG Arena - Password Reset Code", "email/password-reset", variables);
    }
    
    // ===== TRANSACTIONAL EMAILS =====
    
    /**
     * Send welcome email to new user
     */
    public void sendWelcomeEmail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        sendHtmlEmail(user.getEmail(), "Benvenuto su TCG Arena! 🎴", "email/welcome", variables);
    }
    
    /**
     * Send tournament registration confirmation
     */
    public void sendTournamentRegistration(String email, String username, CommunityEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("eventName", event.getTitle());
        variables.put("eventDate", event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        variables.put("eventLocation", event.getLocationName());
        variables.put("eventId", event.getId());
        sendHtmlEmail(email, "Iscrizione Torneo Confermata - " + event.getTitle(), "email/tournament-registration", variables);
    }
    
    /**
     * Send card reservation with QR code
     */
    public void sendCardReservation(String email, String username, String cardName, String shopName, String qrCodeUrl) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("cardName", cardName);
        variables.put("shopName", shopName);
        variables.put("qrCodeUrl", qrCodeUrl);
        sendHtmlEmail(email, "Prenotazione Carta - " + cardName, "email/card-reservation", variables);
    }
    
    /**
     * Send inactivity reminder
     */
    public void sendInactivityReminder(User user, int daysInactive) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", user.getUsername());
        variables.put("daysInactive", daysInactive);
        sendHtmlEmail(user.getEmail(), "Ti mancano! Torna su TCG Arena 🎴", "email/inactivity-reminder", variables);
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
            ? "✅ Import JustTCG Completato - " + summary.getCardsAdded() + " carte aggiunte"
            : "⚠️ Import JustTCG - Stato: " + summary.getStatus();
            
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
        
        sendHtmlEmail(email, "📊 Il tuo riepilogo TCG Arena giornaliero", "email/daily-digest", variables);
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
        sendHtmlEmail(receiverEmail, "Nuova Proposta di Scambio da " + senderUsername, "email/trade-request", variables);
    }
    
    /**
     * Send trade accepted notification
     */
    public void sendTradeAccepted(String email, String username, String partnerUsername, Long tradeId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("partnerUsername", partnerUsername);
        variables.put("tradeId", tradeId);
        sendHtmlEmail(email, "Scambio Accettato! 🎉", "email/trade-accepted", variables);
    }
    
    /**
     * Send trade completed notification
     */
    public void sendTradeCompleted(String email, String username, String partnerUsername, Long tradeId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("partnerUsername", partnerUsername);
        variables.put("tradeId", tradeId);
        sendHtmlEmail(email, "Scambio Completato! ⭐", "email/trade-completed", variables);
    }
    
    // ===== EVENT NOTIFICATIONS =====
    
    /**
     * Send event reminder (24h before)
     */
    public void sendEventReminder(String email, String username, CommunityEvent event) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("eventTitle", event.getTitle());
        variables.put("eventDate", event.getEventDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        variables.put("eventTime", event.getEventDate().format(DateTimeFormatter.ofPattern("HH:mm")));
        variables.put("eventLocation", event.getLocationName());
        variables.put("eventId", event.getId());
        if (event.getShop() != null) {
            variables.put("eventShopName", event.getShop().getName());
        }
        sendHtmlEmail(email, "🔔 Promemoria: " + event.getTitle() + " domani!", "email/event-reminder", variables);
    }
    
    /**
     * Send event cancelled notification
     */
    public void sendEventCancelled(String email, String username, String eventTitle, String eventDate, 
                                    String eventLocation, String cancellationReason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("eventTitle", eventTitle);
        variables.put("eventDate", eventDate);
        variables.put("eventLocation", eventLocation);
        variables.put("cancellationReason", cancellationReason);
        sendHtmlEmail(email, "❌ Evento Cancellato: " + eventTitle, "email/event-cancelled", variables);
    }
    
    /**
     * Send event updated notification
     */
    public void sendEventUpdated(String email, String username, String eventTitle, Long eventId, 
                                  String newDate, String newTime, String newLocation, String updateNote) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("eventTitle", eventTitle);
        variables.put("eventId", eventId);
        if (newDate != null) variables.put("newDate", newDate);
        if (newTime != null) variables.put("newTime", newTime);
        if (newLocation != null) variables.put("newLocation", newLocation);
        if (updateNote != null) variables.put("updateNote", updateNote);
        sendHtmlEmail(email, "📝 Evento Aggiornato: " + eventTitle, "email/event-updated", variables);
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
        sendHtmlEmail(email, "🔐 Nuovo accesso rilevato al tuo account", "email/security-alert", variables);
    }
    
    /**
     * Send shop approved notification
     */
    public void sendShopApproved(String email, String ownerName, String shopName, Long shopId) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("ownerName", ownerName);
        variables.put("shopName", shopName);
        variables.put("shopId", shopId);
        sendHtmlEmail(email, "🎊 Negozio Approvato: " + shopName, "email/shop-approved", variables);
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
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("verificationCode", verificationCode);
        variables.put("verificationLink", verificationLink);
        sendHtmlEmail(email, "Verifica la tua email - TCG Arena", "email/email-verification", variables);
    }
}


