package com.tcg.arena.scheduler;

import com.tcg.arena.model.User;
import com.tcg.arena.repository.CardVoteRepository;
import com.tcg.arena.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Scheduled job to send daily push notifications for Card Rating Arena
 * to users who have voted at least once.
 */
@Component
public class CardRatingArenaNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(CardRatingArenaNotificationScheduler.class);

    @Autowired
    private CardVoteRepository cardVoteRepository;

    @Autowired
    private NotificationService notificationService;

    // Array of fun messages to rotate through
    private static final String[] MESSAGES = {
        "🌟 Ehi campione! Hai ancora energia per votare qualche carta oggi?",
        "🎯 Il tuo parere conta! Cosa ne pensi delle nuove carte uscite?",
        "🔥 Sei un esperto di carte! Aiutaci a costruire la community dei voti!",
        "⭐ Le tue valutazioni aiutano tutti! Pronto per qualche voto oggi?",
        "🎪 Entra nell'arena! Le carte aspettano il tuo giudizio!",
        "⚡ Flash vote time! Cosa ne pensi delle ultime novità?",
        "🎨 Tu sei il giudice! Le carte attendono il tuo verdetto!",
        "🚀 Pronti per decollare? I tuoi voti sono sempre benvenuti!",
        "💎 Le tue opinioni sono preziose! Hai tempo per qualche voto?",
        "🎪 Benvenuto nell'arena! Le carte sono pronte per il tuo giudizio!"
    };

    /**
     * Runs every day at 16:00 (4 PM) to send Card Rating Arena reminder notifications
     * to users who have voted at least once.
     */
    @Scheduled(cron = "0 0 16 * * ?") // Every day at 16:00
    public void sendDailyCardRatingArenaNotifications() {
        log.info("🔔 Running scheduled job: sendDailyCardRatingArenaNotifications");

        try {
            // Get all users who have voted at least once
            List<User> activeVoters = cardVoteRepository.findUsersWhoHaveVoted();

            if (activeVoters.isEmpty()) {
                log.info("📭 No users found who have voted. Skipping notifications.");
                return;
            }

            log.info("📱 Found {} users who have voted. Sending notifications...", activeVoters.size());

            // Send notification to each user
            Random random = new Random();
            int notificationsSent = 0;

            for (User user : activeVoters) {
                try {
                    // Rotate through different messages
                    String message = MESSAGES[random.nextInt(MESSAGES.length)];

                    // Send push notification
                    notificationService.sendPushNotification(
                        user.getId(),
                        "Card Rating Arena 🎯",
                        message
                    );

                    notificationsSent++;

                    // Small delay to avoid overwhelming Firebase
                    Thread.sleep(100);

                } catch (Exception e) {
                    log.error("Failed to send notification to user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("✅ Successfully sent {} Card Rating Arena notifications", notificationsSent);

        } catch (Exception e) {
            log.error("❌ Error in sendDailyCardRatingArenaNotifications job", e);
        }
    }
}