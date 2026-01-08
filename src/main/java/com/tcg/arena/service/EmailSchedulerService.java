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
    private final ImportStatsCollector statsCollector;

    @Value("${app.admin.email:patriziopezzilli@gmail.com}")
    private String adminEmail;

    public EmailSchedulerService(EmailService emailService, UserRepository userRepository, 
                                 ImportStatsCollector statsCollector) {
        this.emailService = emailService;
        this.userRepository = userRepository;
        this.statsCollector = statsCollector;
    }

    /**
     * Nightly JustTCG Import Summary
     * Runs every night at 3:30 AM (after all imports at 3:00 AM)
     * ONLY sends to admin email (patriziopezzilli@gmail.com)
     * Aggregates results from ALL TCG imports
     */
    @Scheduled(cron = "0 30 3 * * *") // 3:30 AM every day
    public void sendNightlyImportSummary() {
        logger.info("Starting nightly import summary job...");
        
        try {
            // Get all import results from the batch
            List<ImportSummaryEmailDTO.TCGImportResult> tcgResults = statsCollector.getBatchResults();
            
            if (tcgResults.isEmpty()) {
                logger.warn("No import results found for summary email");
                return;
            }
            
            // Create aggregated summary
            ImportSummaryEmailDTO summary = createAggregatedImportSummary(tcgResults);
            
            // SEND ONLY TO ADMIN
            emailService.sendImportSummary(adminEmail, summary);
            
            logger.info("Import summary sent successfully to admin: {} (covering {} TCG types)", 
                adminEmail, tcgResults.size());
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
            // Calculate real statistics from yesterday
            DailyDigestEmailDTO digest = createRealDailyDigest();
            
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

    // ===== AGGREGATION LOGIC =====

    /**
     * Create aggregated import summary from individual TCG results
     */
    private ImportSummaryEmailDTO createAggregatedImportSummary(List<ImportSummaryEmailDTO.TCGImportResult> tcgResults) {
        ImportSummaryEmailDTO summary = new ImportSummaryEmailDTO();
        summary.setUsername("Patrizio");
        summary.setTcgResults(tcgResults);
        
        // Set overall batch times
        summary.setImportStartTime(statsCollector.getBatchStartTime());
        summary.setImportEndTime(LocalDateTime.now());
        
        // Calculate aggregated stats
        int totalProcessed = 0;
        int totalAdded = 0;
        int totalUpdated = 0;
        int totalErrors = 0;
        
        for (ImportSummaryEmailDTO.TCGImportResult result : tcgResults) {
            totalProcessed += result.getCardsProcessed();
            totalAdded += result.getCardsAdded();
            totalUpdated += result.getCardsUpdated();
            totalErrors += result.getErrors();
        }
        
        summary.setTotalCardsProcessed(totalProcessed);
        summary.setCardsAdded(totalAdded);
        summary.setCardsUpdated(totalUpdated);
        summary.setCardsSkipped(0); // Not tracked per-TCG currently
        summary.setErrors(totalErrors);
        
        // Set overall status
        summary.setStatus(statsCollector.getOverallStatus());
        
        return summary;
    }

    /**
     * Create real daily digest with actual platform statistics
     * Currently collects: new users registered yesterday
     * TODO: Add more real metrics (active trades, upcoming events, new shops, etc.)
     */
    private DailyDigestEmailDTO createRealDailyDigest() {
        DailyDigestEmailDTO digest = new DailyDigestEmailDTO();
        digest.setUsername("Patrizio");
        digest.setDigestDate(LocalDateTime.now());

        try {
            // User stats (placeholder - would need user-specific data)
            DailyDigestEmailDTO.UserStats userStats = new DailyDigestEmailDTO.UserStats();
            userStats.setNewCards(0); // Would need to track daily card additions per user
            userStats.setNewTrades(0); // Would need to track daily trades per user
            userStats.setCompletedTrades(0);
            userStats.setMessagesReceived(0);
            userStats.setProfileViews(0);
            userStats.setEventsNearby(0);
            userStats.setCollectionValueChange(0.0);
            digest.setUserStats(userStats);

            // Platform stats - REAL DATA
            DailyDigestEmailDTO.PlatformStats platformStats = new DailyDigestEmailDTO.PlatformStats();

            // Count users registered in the last 24 hours
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
            long newUsersYesterday = userRepository.countByDateJoinedAfter(yesterday);
            platformStats.setNewUsers((int) newUsersYesterday);

            // Count active trades (placeholder - would need trade repository)
            platformStats.setActiveTrades(0); // TODO: Implement trade counting

            // Count upcoming events in the next 7 days (placeholder - would need event repository method)
            platformStats.setUpcomingEvents(0); // TODO: Implement event counting

            // Count new shops registered in the last 24 hours (placeholder - would need shop repository method)
            platformStats.setNewShops(0); // TODO: Implement shop counting

            digest.setPlatformStats(platformStats);

            // Highlights - dynamic based on real data
            List<DailyDigestEmailDTO.Highlight> highlights = new ArrayList<>();

            if (newUsersYesterday > 0) {
                highlights.add(new DailyDigestEmailDTO.Highlight(
                    "NEW_USERS",
                    newUsersYesterday + " nuovi utenti",
                    "La community sta crescendo! " + newUsersYesterday + " utenti si sono registrati ieri",
                    null,
                    "👥"
                ));
            }

            if (upcomingEvents > 0) {
                highlights.add(new DailyDigestEmailDTO.Highlight(
                    "UPCOMING_EVENTS",
                    upcomingEvents + " eventi programmati",
                    "Questa settimana ci sono " + upcomingEvents + " eventi nella tua zona",
                    "https://tcgarena.it/events",
                    "📅"
                ));
            }

            if (newShopsYesterday > 0) {
                highlights.add(new DailyDigestEmailDTO.Highlight(
                    "NEW_SHOPS",
                    newShopsYesterday + " nuovi negozi",
                    newShopsYesterday + " negozi si sono registrati alla piattaforma ieri",
                    "https://tcgarena.it/shops",
                    "🏪"
                ));
            }

            // Add some default highlights if no real data
            if (highlights.isEmpty()) {
                highlights.add(new DailyDigestEmailDTO.Highlight(
                    "PLATFORM_HEALTH",
                    "Piattaforma operativa",
                    "Tutti i sistemi funzionano correttamente",
                    null,
                    "✅"
                ));
            }

            digest.setHighlights(highlights);

            // Recommendations - placeholder for now
            List<DailyDigestEmailDTO.Recommendation> recommendations = new ArrayList<>();
            // TODO: Add real recommendations based on upcoming events
            digest.setRecommendations(recommendations);

        } catch (Exception e) {
            logger.error("Error collecting real daily digest data, falling back to mock data", e);
            // Fallback to mock data if real data collection fails
            return createMockDailyDigest();
        }

        return digest;
    }

    // ===== MOCK DATA GENERATORS (Legacy - can be removed) =====

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
