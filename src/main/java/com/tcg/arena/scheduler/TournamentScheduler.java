package com.tcg.arena.scheduler;

import com.tcg.arena.service.TournamentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to automatically update tournament statuses.
 * Marks IN_PROGRESS tournaments as COMPLETED if their scheduled end time has
 * passed.
 */
@Component
public class TournamentScheduler {

    private static final Logger log = LoggerFactory.getLogger(TournamentScheduler.class);

    @Autowired
    private TournamentService tournamentService;

    /**
     * Runs every hour to check for expired tournaments and mark them as COMPLETED.
     * A tournament is considered expired if:
     * - Status is IN_PROGRESS or UPCOMING/REGISTRATION_*
     * - The scheduled date + estimated duration has passed
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour at minute 0
    public void autoCompleteExpiredTournaments() {
        log.info("Running scheduled job: autoCompleteExpiredTournaments");
        try {
            int count = tournamentService.autoCompleteExpiredTournaments();
            if (count > 0) {
                log.info("Auto-completed {} expired tournaments", count);
            }
        } catch (Exception e) {
            log.error("Error in autoCompleteExpiredTournaments job", e);
        }
    }
}
