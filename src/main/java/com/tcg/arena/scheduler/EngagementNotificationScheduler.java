package com.tcg.arena.scheduler;

import com.tcg.arena.model.User;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.repository.InventoryCardRepository;
import com.tcg.arena.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EngagementNotificationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EngagementNotificationScheduler.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InventoryCardRepository inventoryCardRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Welcome Message: Runs daily at 10:00 AM
     * targets users registered yesterday (between 24h and 48h ago)
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void sendWelcomeMessage() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.minusDays(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime yesterdayEnd = now.minusDays(1).withHour(23).withMinute(59).withSecond(59);

        List<User> newUsers = userRepository.findByDateJoinedBetween(yesterdayStart, yesterdayEnd);
        logger.info("📅 Sending welcome message to {} new users registered yesterday", newUsers.size());

        for (User user : newUsers) {
            boolean isEnglish = "en".equalsIgnoreCase(user.getLocale());
            String title = isEnglish ? "Welcome to TCG Arena! 🚀" : "Benvenuto nell'Arena! 🚀";
            String message = isEnglish
                    ? "Start your collection, find tournaments nearby, and trade with the community!"
                    : "Inizia la tua collezione, trova tornei vicino a te e scambia con la community!";

            notificationService.sendPushNotification(user.getId(), title, message);
        }
    }

    /**
     * Inactivity Alert: Runs daily at 18:00 (6 PM)
     * Notifications for users inactive for 7 days (last login between 7 and 8 days
     * ago)
     */
    @Scheduled(cron = "0 0 18 * * *")
    public void sendInactivityAlert() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startWindow = now.minusDays(8);
        LocalDateTime endWindow = now.minusDays(7);

        List<User> inactiveUsers = userRepository.findByLastLoginBetween(startWindow, endWindow);
        logger.info("💤 Sending inactivity alert to {} users inactive for ~7 days", inactiveUsers.size());

        for (User user : inactiveUsers) {
            boolean isEnglish = "en".equalsIgnoreCase(user.getLocale());
            String title = isEnglish ? "We miss you! 👋" : "È da un po' che non ti vediamo! 👋";
            String message = isEnglish
                    ? "New tournaments and trades are waiting for you. Come back to the Arena!"
                    : "Nuovi tornei e scambi ti aspettano. Torna nell'Arena!";

            notificationService.sendPushNotification(user.getId(), title, message);
        }
    }

    /**
     * New Cards Digest: Runs daily at 20:00 (8 PM)
     * Checks if significant amount of cards (>100) were added/restocked for a TCG
     * in the last 24h
     */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendNewCardsDigest() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        for (TCGType tcg : TCGType.values()) {
            long count = inventoryCardRepository.countRecentlyAdded(tcg.name(), yesterday);

            if (count > 100) {
                logger.info("📈 High activity detected for {}: {} new cards", tcg, count);

                // Find interested users
                List<User> fans = userRepository.findByFavoriteTCGTypesStringContaining(tcg.name());

                for (User fan : fans) {
                    boolean isEnglish = "en".equalsIgnoreCase(fan.getLocale());
                    String title = isEnglish ? "Market Update 📈" : "Aggiornamento Mercato 📈";
                    String message = isEnglish
                            ? String.format("%d new %s cards added today! Check the deals.", count, tcg.name())
                            : String.format("%d nuove carte %s aggiunte oggi! Controlla le offerte.", count,
                                    tcg.name());

                    notificationService.sendPushNotification(fan.getId(), title, message);
                }
            }
        }
    }
}
