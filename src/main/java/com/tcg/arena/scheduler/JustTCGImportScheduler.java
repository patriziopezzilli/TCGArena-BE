package com.tcg.arena.scheduler;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.ImportStatsCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for JustTCG API imports
 * Runs nightly at 3 AM for all TCG types
 * Import progress (offset) is automatically retrieved from the database
 */
@Component
public class JustTCGImportScheduler {

    private static final Logger logger = LoggerFactory.getLogger(JustTCGImportScheduler.class);

    @Autowired
    private BatchService batchService;
    
    @Autowired
    private ImportStatsCollector statsCollector;

    /**
     * Run JustTCG import for all TCG types at 3 AM every night
     * The offset for each TCG is automatically retrieved from the import_progress table
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 3 AM
    public void runNightlyJustTCGImport() {
        logger.info("Starting nightly JustTCG import for all TCG types at 3 AM");
        
        // Reset batch statistics
        statsCollector.resetBatch();

        // Import for all supported TCG types
        TCGType[] tcgTypes = {
            TCGType.MAGIC,
            TCGType.POKEMON,
            TCGType.YUGIOH,
            TCGType.LORCANA,
            TCGType.ONE_PIECE,
            TCGType.DIGIMON
        };

        for (TCGType tcgType : tcgTypes) {
            try {
                logger.info("Triggering JustTCG import for {}", tcgType.getDisplayName());
                batchService.triggerJustTCGImport(tcgType);
                logger.info("Successfully triggered JustTCG import for {}", tcgType.getDisplayName());
                
                // Add a small delay between imports to avoid overloading
                Thread.sleep(5000); // 5 seconds delay
            } catch (Exception e) {
                logger.error("Error triggering JustTCG import for {}: {}", 
                    tcgType.getDisplayName(), e.getMessage(), e);
                // Continue with next TCG type even if one fails
            }
        }

        logger.info("Completed nightly JustTCG import trigger for all TCG types");
    }
}
