package com.tcg.arena.service;

import com.tcg.arena.dto.DailyDigestEmailDTO;
import com.tcg.arena.dto.ImportSummaryEmailDTO;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for scheduled email jobs (nightly batches)
 * IMPORTANT: Daily digest and import summary are sent ONLY to admin email
 */
@Service
public class EmailSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(EmailSchedulerService.class);

    private final EmailService emailService;
    private final UserRepository userRepository;

    @Value("${app.admin.email:patriziopezzilli@gmail.com}")
    private String adminEmail;

    public EmailSchedulerService(EmailService emailService, UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    /**
     * Nightly JustTCG Import Summary
     * Runs every night at 3:00 AM
     * ONLY sends to admin email (patriziopezzilli@gmail.com)
     */
    @Scheduled(cron = "0 0 3 * * *") // 3:00 AM every day
    public void sendNightlyImportSummary() {
        logger.info("Starting nightly import summary job...");
        
        try {
            // TODO: Implement actual import logic
            // For now, create a mock summary
            ImportSummaryEmailDTO summary = createMockImportSummary();
            
            // SEND ONLY TO ADMIN
            emailService.sendImportSummary(adminEmail, summary);
            
            logger.info("Import summary sent successfully to admin: {}", adminEmail);
        } catch (Exception e) {
            logger.error("Failed to send import summary", e);
        }
    }

    /**
     * Daily Digest Email
     * Runs every morning at 8:00 AM
     * ONLY sends to admin email (patriziopezzilli@gmail.com)
     */
    @Scheduled(cron = "0 0 8 * * *") // 8:00 AM every day
    public void sendDailyDigest() {
        logger.info("Starting daily digest job...");
        
        try {
            // TODO: Calculate real statistics from yesterday
            DailyDigestEmailDTO digest = createMockDailyDigest();
            
            // SEND ONLY TO ADMIN
            emailService.sendDailyDigest(adminEmail, digest);
            
            logger.info("Daily digest sent successfully to admin: {}", adminEmail);
        } catch (Exception e) {
            logger.error("Failed to send daily digest", e);
        }
    }

    /**
     * Check for inactive users and send reminders
     * Runs every day at 10:00 AM
     * This CAN be sent to regular users (not admin-only)
     */
    @Scheduled(cron = "0 0 10 * * *") // 10:00 AM every day
    public void sendInactivityReminders() {
        logger.info("Starting inactivity reminders job...");
        
        try {
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            
            // TODO: Find users who haven't logged in for 30+ days
            // List<User> inactiveUsers = userRepository.findByLastLoginBefore(thirtyDaysAgo);
            
            // For now, just log
            logger.info("Inactivity reminders job completed (no users to notify)");
        } catch (Exception e) {
            logger.error("Failed to send inactivity reminders", e);
        }
    }

    // ===== MOCK DATA GENERATORS (Replace with real logic) =====

    private ImportSummaryEmailDTO createMockImportSummary() {
        ImportSummaryEmailDTO summary = new ImportSummaryEmailDTO();
        summary.setUsername("Patrizio");
        summary.setImportStartTime(LocalDateTime.now().minusMinutes(5));
        summary.setImportEndTime(LocalDateTime.now());
        summary.setStatus("SUCCESS");
        summary.setTotalCardsProcessed(150);
        summary.setCardsAdded(45);
        summary.setCardsUpdated(12);
        summary.setCardsSkipped(3);
        summary.setErrors(0);

        // Mock deltas
        List<ImportSummaryEmailDTO.CardDelta> deltas = new ArrayList<>();
        deltas.add(new ImportSummaryEmailDTO.CardDelta(
            "Charizard V",
            "Brilliant Stars",
            0,
            3,
            "ADDED"
        ));
        deltas.add(new ImportSummaryEmailDTO.CardDelta(
            "Pikachu VMAX",
            "Vivid Voltage",
            2,
            5,
            "INCREASED"
        ));
        summary.setDeltas(deltas);

        return summary;
    }

    private DailyDigestEmailDTO createMockDailyDigest() {
        DailyDigestEmailDTO digest = new DailyDigestEmailDTO();
        digest.setUsername("Patrizio");
        digest.setDigestDate(LocalDateTime.now());

        // User stats
        DailyDigestEmailDTO.UserStats userStats = new DailyDigestEmailDTO.UserStats();
        userStats.setNewCards(12);
        userStats.setNewTrades(3);
        userStats.setCompletedTrades(2);
        userStats.setMessagesReceived(8);
        userStats.setProfileViews(45);
        userStats.setEventsNearby(2);
        userStats.setCollectionValueChange(125.50);
        digest.setUserStats(userStats);

        // Platform stats
        DailyDigestEmailDTO.PlatformStats platformStats = new DailyDigestEmailDTO.PlatformStats();
        platformStats.setNewUsers(156);
        platformStats.setActiveTrades(89);
        platformStats.setUpcomingEvents(12);
        platformStats.setNewShops(3);
        digest.setPlatformStats(platformStats);

        // Highlights
        List<DailyDigestEmailDTO.Highlight> highlights = new ArrayList<>();
        highlights.add(new DailyDigestEmailDTO.Highlight(
            "TRADE_MATCH",
            "Nuovo match trovato!",
            "Abbiamo trovato un utente con le carte che cerchi a 3km da te",
            "https://tcgarena.it/trades",
            "🔄"
        ));
        highlights.add(new DailyDigestEmailDTO.Highlight(
            "NEW_EVENT",
            "Torneo Pokémon nelle vicinanze",
            "Torneo competitivo organizzato sabato prossimo",
            "https://tcgarena.it/events",
            "🏆"
        ));
        digest.setHighlights(highlights);

        // Recommendations
        List<DailyDigestEmailDTO.Recommendation> recommendations = new ArrayList<>();
        recommendations.add(new DailyDigestEmailDTO.Recommendation(
            "EVENT",
            "Torneo One Piece",
            "Partecipa al torneo One Piece di sabato a Milano",
            null,
            "https://tcgarena.it/events/123"
        ));
        digest.setRecommendations(recommendations);

        return digest;
    }
}
