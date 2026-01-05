package com.tcg.arena.service;

import com.tcg.arena.dto.ImportSummaryEmailDTO;
import com.tcg.arena.model.TCGType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects and aggregates import statistics for all TCG types during nightly batch
 * Thread-safe for concurrent imports
 */
@Service
public class ImportStatsCollector {
    
    private static final Logger logger = LoggerFactory.getLogger(ImportStatsCollector.class);
    
    private final ConcurrentHashMap<TCGType, ImportSummaryEmailDTO.TCGImportResult> currentBatchStats = new ConcurrentHashMap<>();
    private LocalDateTime batchStartTime;
    
    /**
     * Reset statistics for a new batch run
     */
    public void resetBatch() {
        logger.info("Resetting import stats for new batch");
        currentBatchStats.clear();
        batchStartTime = LocalDateTime.now();
    }
    
    /**
     * Record the start of an import for a TCG type
     */
    public void recordImportStart(TCGType tcgType) {
        logger.info("Recording import start for {}", tcgType.getDisplayName());
        ImportSummaryEmailDTO.TCGImportResult result = new ImportSummaryEmailDTO.TCGImportResult(tcgType);
        result.setStatus("IN_PROGRESS");
        currentBatchStats.put(tcgType, result);
    }
    
    /**
     * Record successful completion of an import
     */
    public void recordImportSuccess(TCGType tcgType, int cardsProcessed, int cardsAdded, int cardsUpdated) {
        logger.info("Recording import success for {}: {} cards processed, {} added, {} updated", 
            tcgType.getDisplayName(), cardsProcessed, cardsAdded, cardsUpdated);
        
        ImportSummaryEmailDTO.TCGImportResult result = currentBatchStats.get(tcgType);
        if (result != null) {
            result.setEndTime(LocalDateTime.now());
            result.setStatus("SUCCESS");
            result.setCardsProcessed(cardsProcessed);
            result.setCardsAdded(cardsAdded);
            result.setCardsUpdated(cardsUpdated);
            result.setErrors(0);
        } else {
            logger.warn("No import record found for {} when recording success", tcgType.getDisplayName());
        }
    }
    
    /**
     * Record import failure
     */
    public void recordImportFailure(TCGType tcgType, String errorMessage) {
        logger.error("Recording import failure for {}: {}", tcgType.getDisplayName(), errorMessage);
        
        ImportSummaryEmailDTO.TCGImportResult result = currentBatchStats.get(tcgType);
        if (result != null) {
            result.setEndTime(LocalDateTime.now());
            result.setStatus("FAILED");
            result.setErrors(1);
            result.setErrorMessage(sanitizeForEmail(errorMessage));
        } else {
            logger.warn("No import record found for {} when recording failure", tcgType.getDisplayName());
        }
    }
    
    /**
     * Get all results for the current batch
     */
    public List<ImportSummaryEmailDTO.TCGImportResult> getBatchResults() {
        return new ArrayList<>(currentBatchStats.values());
    }
    
    /**
     * Check if batch is complete (all imports finished)
     */
    public boolean isBatchComplete() {
        return currentBatchStats.values().stream()
            .allMatch(result -> "SUCCESS".equals(result.getStatus()) || "FAILED".equals(result.getStatus()));
    }
    
    /**
     * Get batch start time
     */
    public LocalDateTime getBatchStartTime() {
        return batchStartTime;
    }
    
    /**
     * Calculate overall batch status
     */
    public String getOverallStatus() {
        List<ImportSummaryEmailDTO.TCGImportResult> results = getBatchResults();
        
        if (results.isEmpty()) {
            return "NO_IMPORTS";
        }
        
        long successCount = results.stream()
            .filter(r -> "SUCCESS".equals(r.getStatus()))
            .count();
        
        long failedCount = results.stream()
            .filter(r -> "FAILED".equals(r.getStatus()))
            .count();
        
        if (failedCount == 0) {
            return "SUCCESS";
        } else if (successCount > 0) {
            return "PARTIAL_SUCCESS";
        } else {
            return "FAILED";
        }
    }
    
    /**
     * Sanitize error messages to prevent SMTP issues
     * Removes special characters and truncates long messages
     */
    private String sanitizeForEmail(String message) {
        if (message == null) {
            return null;
        }
        
        // Remove non-printable characters and control characters
        String sanitized = message.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        // Remove potentially problematic characters
        sanitized = sanitized.replaceAll("[<>]", "");
        
        // Truncate if too long
        if (sanitized.length() > 500) {
            sanitized = sanitized.substring(0, 497) + "...";
        }
        
        return sanitized;
    }
}
