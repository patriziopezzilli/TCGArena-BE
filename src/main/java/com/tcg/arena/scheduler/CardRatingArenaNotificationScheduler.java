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

    @Autowired
    private com.tcg.arena.service.StreakService streakService;

    // Messages for users with an active streak
    private static final String[] STREAK_MESSAGES = {
            "🔥 %d giorni di fila! Continua così, campione!",
            "⚡ La tua streak di %d giorni è on fire! Non fermarti ora!",
            "🎯 %d giorni consecutivi! Sei inarrestabile!",
            "💎 Scatena il tuo potere! %d giorni di voti consecutivi!",
            "🚀 %d giorni di fila! Verso l'infinito e oltre!",
            "🏆 Campione della costanza: %d giorni di streak! Mantienila viva!",
            "⭐ %d giorni! La tua dedizione è leggendaria nell'Arena!",
            "🌪️ Una forza della natura! %d giorni di fila a votare!",
            "🎪 L'Arena ti applaude! %d giorni di presenza consecutiva!",
            "🎨 Il tuo gusto è legge da %d giorni! Continua a votare!"
    };

    // Messages for users without a streak
    private static final String[] NO_STREAK_MESSAGES = {
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

    // English Messages for users with an active streak
    private static final String[] STREAK_MESSAGES_EN = {
            "🔥 %d days in a row! Keep it up, champion!",
            "⚡ Your %d-day streak is on fire! Don't stop now!",
            "🎯 %d consecutive days! You are unstoppable!",
            "💎 Unleash your power! %d consecutive voting days!",
            "🚀 %d days in a row! To infinity and beyond!",
            "🏆 Consistency champion: %d day streak! Keep it alive!",
            "⭐ %d days! Your dedication is legendary in the Arena!",
            "🌪️ A force of nature! %d days of voting in a row!",
            "🎪 The Arena applauds you! %d days of consecutive presence!",
            "🎨 Your taste is law for %d days! Keep voting!"
    };

    // English Messages for users without a streak
    private static final String[] NO_STREAK_MESSAGES_EN = {
            "🌟 Hey champion! Still have energy to vote on some cards today?",
            "🎯 Your opinion matters! What do you think of the new releases?",
            "🔥 You're a card expert! Help us build the voting community!",
            "⭐ Your ratings help everyone! Ready for some votes today?",
            "🎪 Enter the Arena! The cards await your judgment!",
            "⚡ Flash vote time! What do you think of the latest news?",
            "🎨 You are the judge! The cards await your verdict!",
            "🚀 Ready for takeoff? Your votes are always welcome!",
            "💎 Your opinions are precious! Time for a few votes?",
            "🎪 Welcome to the Arena! The cards are ready for your judgment!"
    };

    /**
     * Runs every day at 16:00 (4 PM) to send Card Rating Arena reminder
     * notifications
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

            log.info("📱 Found {} users who have voted. Checking streaks...", activeVoters.size());

            // Send notification to each user
            Random random = new Random();
            int notificationsSent = 0;
            int skippedUsers = 0;

            for (User user : activeVoters) {
                try {
                    // Check user's streak status
                    com.tcg.arena.dto.UserRatingStreakDTO streak = streakService.getStreak(user.getId());

                    // Skip if already voted today
                    if (Boolean.TRUE.equals(streak.getRatedToday())) {
                        skippedUsers++;
                        continue;
                    }

                    // Determine language
                    boolean isEnglish = "en".equalsIgnoreCase(user.getLocale());

                    String title = "Card Rating Arena 🎯";
                    String message;

                    if (streak.getCurrentStreak() != null && streak.getCurrentStreak() > 0) {
                        // User has an active streak - encourage them to keep it!
                        if (isEnglish) {
                            String template = STREAK_MESSAGES_EN[random.nextInt(STREAK_MESSAGES_EN.length)];
                            message = String.format(template, streak.getCurrentStreak());
                            title = "🔥 Streak in danger!";
                        } else {
                            String template = STREAK_MESSAGES[random.nextInt(STREAK_MESSAGES.length)];
                            message = String.format(template, streak.getCurrentStreak());
                            title = "🔥 Streak in pericolo!";
                        }
                    } else {
                        // No active streak - generic motivation
                        if (isEnglish) {
                            message = NO_STREAK_MESSAGES_EN[random.nextInt(NO_STREAK_MESSAGES_EN.length)];
                        } else {
                            message = NO_STREAK_MESSAGES[random.nextInt(NO_STREAK_MESSAGES.length)];
                        }
                    }

                    // Send push notification
                    notificationService.sendPushNotification(
                            user.getId(),
                            title,
                            message);

                    notificationsSent++;

                    // Small delay to avoid overwhelming Firebase
                    Thread.sleep(50);

                } catch (Exception e) {
                    log.error("Failed to process notification for user {}: {}", user.getId(), e.getMessage());
                }
            }

            log.info("✅ Finished Card Rating Arena notifications. Sent: {}, Skipped (Rated Today): {}",
                    notificationsSent, skippedUsers);

        } catch (Exception e) {
            log.error("❌ Error in sendDailyCardRatingArenaNotifications job", e);
        }
    }
}