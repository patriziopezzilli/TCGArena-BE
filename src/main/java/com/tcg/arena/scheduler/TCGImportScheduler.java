package com.tcg.arena.scheduler;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.ImportStatsCollector;
import com.tcg.arena.service.TCGApiClient;
import com.tcg.arena.service.SchedulerLockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler for TCG API imports
 * Runs nightly at 3 AM — covers all 11 TCG types across 7 days
 *
 * Schedule:
 * - Monday: Pokemon, Union Arena
 * - Tuesday: One Piece
 * - Wednesday: Magic (delta import only — lightweight), Dragon Ball Super
 * Fusion World
 * - Thursday: Yu-Gi-Oh!, Flesh and Blood
 * - Friday: Digimon, Pokemon Japan
 * - Saturday: Lorcana
 * - Sunday: Riftbound
 */
@Component
public class TCGImportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(TCGImportScheduler.class);

    @Autowired
    private BatchService batchService;

    @Autowired
    private ImportStatsCollector statsCollector;

    @Autowired
    private TCGApiClient tcgApiClient;

    @Autowired
    private SchedulerLockService schedulerLockService;

    /**
     * Returns the list of TCG types to import today based on the day of the week.
     */
    private List<TCGType> getTCGTypesForToday() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> List.of(TCGType.POKEMON, TCGType.UNION_ARENA);
            case TUESDAY -> List.of(TCGType.ONE_PIECE);
            case WEDNESDAY -> List.of(TCGType.MAGIC, TCGType.DRAGON_BALL_SUPER_FUSION_WORLD); // delta + Dragon Ball
            case THURSDAY -> List.of(TCGType.YUGIOH, TCGType.FLESH_AND_BLOOD);
            case FRIDAY -> List.of(TCGType.DIGIMON, TCGType.POKEMON_JAPAN);
            case SATURDAY -> List.of(TCGType.LORCANA);
            case SUNDAY -> List.of(TCGType.RIFTBOUND);
        };
    }

    /**
     * Run TCG import(s) at 3 AM every night.
     * Each day imports 1–2 TCG types according to the rotating schedule.
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void runNightlyTCGImport() {
        List<TCGType> tcgTypes = getTCGTypesForToday();
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();

        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ NIGHTLY IMPORT: {} — {} TCG(s) scheduled", dayOfWeek, tcgTypes.size());
        logger.info("╚══════════════════════════════════════════════════════════════");

        for (TCGType tcgType : tcgTypes) {
            String lockKey = "nightly-tcg-import-" + tcgType.name();

            if (!schedulerLockService.acquireLock(lockKey, Duration.ofHours(6))) {
                logger.warn("Nightly TCG import for {} is already running or was recently executed. Skipping.",
                        tcgType);
                continue;
            }

            try {
                statsCollector.resetBatch();

                if (tcgType == TCGType.MAGIC) {
                    // Use delta import for Magic (fast, only new cards)
                    logger.info("Starting nightly delta import for {}", tcgType.getDisplayName());
                    Integer imported = tcgApiClient.importMagicDelta().block(Duration.ofMinutes(15));
                    logger.info("Nightly delta import completed for {}: {} cards processed",
                            tcgType.getDisplayName(), imported);
                } else {
                    logger.info("Triggering nightly import for {}", tcgType.getDisplayName());
                    batchService.triggerTCGImport(tcgType);
                    logger.info("Successfully triggered import for {}", tcgType.getDisplayName());
                }
            } catch (Exception e) {
                logger.error("Error during nightly import for {}: {}",
                        tcgType.getDisplayName(), e.getMessage(), e);
            } finally {
                schedulerLockService.releaseLock(lockKey);
            }
        }

        logger.info("Completed nightly TCG import cycle for {}", dayOfWeek);
    }
}
