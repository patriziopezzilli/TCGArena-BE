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

/**
 * Scheduler for TCG API imports
 * Runs nightly at 3 AM for ONE TCG type per day (rotating schedule)
 * 
 * Schedule:
 * - Monday: Pokemon
 * - Tuesday: One Piece
 * - Wednesday: Magic (delta import)
 * - Thursday: Yu-Gi-Oh!
 * - Friday: Digimon
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
     * Get the TCG type to import based on the day of the week
     */
    private TCGType getTCGTypeForToday() {
        DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
        return switch (dayOfWeek) {
            case MONDAY -> TCGType.POKEMON;
            case TUESDAY -> TCGType.ONE_PIECE;
            case WEDNESDAY -> TCGType.MAGIC;
            case THURSDAY -> TCGType.YUGIOH;
            case FRIDAY -> TCGType.DIGIMON;
            case SATURDAY -> TCGType.LORCANA;
            case SUNDAY -> TCGType.RIFTBOUND;
        };
    }

    /**
     * Run TCG import for ONE TCG type at 3 AM every night
     * The TCG to import is determined by the day of the week
     */
    // @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM - TEMPORARILY DISABLED
    public void runNightlyTCGImport() {
        TCGType tcgType = getTCGTypeForToday();
        String lockKey = "nightly-tcg-import-" + tcgType.name();
        
        // Try to acquire lock
        if (!schedulerLockService.acquireLock(lockKey, Duration.ofHours(6))) {
            logger.warn("Nightly TCG import for {} is already running or was recently executed. Skipping.", tcgType);
            return;
        }
        
        try {
            DayOfWeek dayOfWeek = LocalDate.now().getDayOfWeek();
            logger.info("╔══════════════════════════════════════════════════════════════");
            logger.info("║ NIGHTLY IMPORT: {} ({})        ", tcgType.getDisplayName(), dayOfWeek);
            logger.info("╚══════════════════════════════════════════════════════════════");
            
            // Reset batch statistics
            statsCollector.resetBatch();

            try {
                if (tcgType == TCGType.MAGIC) {
                    // Use delta import for Magic (fast, only new cards)
                    logger.info("Starting nightly delta import for {}", tcgType.getDisplayName());
                    Integer imported = tcgApiClient.importMagicDelta().block(Duration.ofMinutes(15));
                    logger.info("Nightly delta import completed for {}: {} cards processed", 
                               tcgType.getDisplayName(), imported);
                } else {
                    // Use full import for other TCGs (new sets, empty sets, and delta sets)
                    logger.info("Triggering TCG import for {}", tcgType.getDisplayName());
                    batchService.triggerTCGImport(tcgType);
                    logger.info("Successfully triggered TCG import for {}", tcgType.getDisplayName());
                }
            } catch (Exception e) {
                logger.error("Error during nightly import for {}: {}", 
                    tcgType.getDisplayName(), e.getMessage(), e);
            }

            logger.info("Completed nightly TCG import for {}", tcgType.getDisplayName());
        } finally {
            // Always release the lock
            schedulerLockService.releaseLock(lockKey);
        }
    }
}
