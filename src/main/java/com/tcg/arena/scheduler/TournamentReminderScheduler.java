package com.tcg.arena.scheduler;

import com.tcg.arena.model.Tournament;
import com.tcg.arena.model.TournamentParticipant;
import com.tcg.arena.repository.TournamentParticipantRepository;
import com.tcg.arena.repository.TournamentRepository;
import com.tcg.arena.service.NotificationService;
import com.tcg.arena.service.SchedulerLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Scheduled job to send push notifications to tournament participants
 * when their registered tournaments are about to start.
 */
@Component
public class TournamentReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(TournamentReminderScheduler.class);

    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SchedulerLockService schedulerLockService;

    /**
     * Runs every 10 minutes to check for tournaments starting within 30 minutes
     * and send reminder notifications to all registered participants.
     */
    @Scheduled(fixedRate = 600000) // Every 10 minutes (10 * 60 * 1000 ms)
    @Transactional
    public void sendTournamentStartReminders() {
        // Try to acquire lock to prevent duplicate executions
        if (!schedulerLockService.acquireLock("tournament_reminders", Duration.ofMinutes(15))) {
            log.info("⏳ Tournament reminder job already running, skipping execution");
            return;
        }

        log.info("🔔 Running scheduled job: sendTournamentStartReminders");

        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime within30Minutes = now.plusMinutes(30);
            LocalDateTime twoHoursAgo = now.minusHours(2);

            // Find tournaments starting within the next 30 minutes (not reminded in last 2 hours)
            List<Tournament> upcomingTournaments = tournamentRepository.findTournamentsStartingWithinMinutes(now, within30Minutes, twoHoursAgo);

            if (upcomingTournaments.isEmpty()) {
                log.info("📭 No tournaments starting within 30 minutes (or already reminded recently). Skipping notifications.");
                return;
            }

            log.info("🏆 Found {} tournaments starting within 30 minutes", upcomingTournaments.size());

            // Get tournament IDs
            List<Long> tournamentIds = upcomingTournaments.stream()
                    .map(Tournament::getId)
                    .collect(Collectors.toList());

            // Get all participants for these tournaments
            List<TournamentParticipant> participants = tournamentParticipantRepository.findByTournamentIdsWithUserDetails(tournamentIds);

            if (participants.isEmpty()) {
                log.info("👥 No participants found for upcoming tournaments. Skipping notifications.");
                return;
            }

            log.info("👥 Found {} participants across {} tournaments. Sending notifications...", participants.size(), upcomingTournaments.size());

            // Group participants by tournament for better logging
            var participantsByTournament = participants.stream()
                    .collect(Collectors.groupingBy(TournamentParticipant::getTournamentId));

            int totalNotificationsSent = 0;

            // Send notifications for each tournament
            for (Tournament tournament : upcomingTournaments) {
                List<TournamentParticipant> tournamentParticipants = participantsByTournament.get(tournament.getId());

                if (tournamentParticipants == null || tournamentParticipants.isEmpty()) {
                    continue;
                }

                // Calculate minutes until start
                long minutesUntilStart = java.time.Duration.between(now, tournament.getStartDate()).toMinutes();

                // Format start time
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
                String startTime = tournament.getStartDate().format(timeFormatter);

                // Send notification to each participant
                for (TournamentParticipant participant : tournamentParticipants) {
                    try {
                        String message = createReminderMessage(tournament, minutesUntilStart, startTime);

                        // Send push notification
                        notificationService.sendPushNotification(
                            participant.getUserId(),
                            "🏆 Torneo in arrivo!",
                            message
                        );

                        totalNotificationsSent++;

                        // Small delay to avoid overwhelming Firebase
                        Thread.sleep(50);

                    } catch (Exception e) {
                        log.error("Failed to send tournament reminder to user {} for tournament {}: {}",
                                participant.getUserId(), tournament.getId(), e.getMessage());
                    }
                }

                // Update last reminder sent timestamp
                tournament.setLastReminderSentAt(now);
                tournamentRepository.save(tournament);

                log.info("✅ Sent {} notifications for tournament '{}' and updated reminder timestamp",
                        tournamentParticipants.size(), tournament.getTitle());
            }

            log.info("🎯 Successfully sent {} tournament reminder notifications", totalNotificationsSent);

        } catch (Exception e) {
            log.error("❌ Error in sendTournamentStartReminders job", e);
        }
    }

    /**
     * Creates a personalized reminder message based on time until tournament starts
     */
    private String createReminderMessage(Tournament tournament, long minutesUntilStart, String startTime) {
        String tournamentName = tournament.getTitle();

        if (minutesUntilStart <= 5) {
            return String.format("⚡ Il torneo '%s' inizia tra %d minuti alle %s! Preparati!",
                    tournamentName, minutesUntilStart, startTime);
        } else if (minutesUntilStart <= 15) {
            return String.format("⏰ Il torneo '%s' inizia tra %d minuti alle %s. È ora di prepararsi!",
                    tournamentName, minutesUntilStart, startTime);
        } else {
            return String.format("🏆 Il torneo '%s' a cui sei iscritto inizia tra %d minuti alle %s.",
                    tournamentName, minutesUntilStart, startTime);
        }
    }
}