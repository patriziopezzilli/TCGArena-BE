package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO for JustTCG import summary email
 * Supports aggregating multiple TCG import results
 */
public class ImportSummaryEmailDTO {
    private String username;
    private LocalDateTime importStartTime;
    private LocalDateTime importEndTime;
    private String status; // SUCCESS, PARTIAL_SUCCESS, FAILED
    private int totalCardsProcessed;
    private int cardsAdded;
    private int cardsUpdated;
    private int cardsSkipped;
    private int errors;
    private List<CardDelta> deltas;
    private String errorMessage;
    
    // Multi-TCG aggregation
    private List<TCGImportResult> tcgResults = new ArrayList<>();

    public static class CardDelta {
        private String cardName;
        private String setName;
        private int quantityBefore;
        private int quantityAfter;
        private int delta;
        private String changeType; // ADDED, INCREASED, DECREASED

        // Constructors
        public CardDelta() {}

        public CardDelta(String cardName, String setName, int quantityBefore, int quantityAfter, String changeType) {
            this.cardName = cardName;
            this.setName = setName;
            this.quantityBefore = quantityBefore;
            this.quantityAfter = quantityAfter;
            this.delta = quantityAfter - quantityBefore;
            this.changeType = changeType;
        }

        // Getters and Setters
        public String getCardName() { return cardName; }
        public void setCardName(String cardName) { this.cardName = cardName; }

        public String getSetName() { return setName; }
        public void setSetName(String setName) { this.setName = setName; }

        public int getQuantityBefore() { return quantityBefore; }
        public void setQuantityBefore(int quantityBefore) { this.quantityBefore = quantityBefore; }

        public int getQuantityAfter() { return quantityAfter; }
        public void setQuantityAfter(int quantityAfter) { this.quantityAfter = quantityAfter; }

        public int getDelta() { return delta; }
        public void setDelta(int delta) { this.delta = delta; }

        public String getChangeType() { return changeType; }
        public void setChangeType(String changeType) { this.changeType = changeType; }
    }

    // Constructors
    public ImportSummaryEmailDTO() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getImportStartTime() { return importStartTime; }
    public void setImportStartTime(LocalDateTime importStartTime) { this.importStartTime = importStartTime; }

    public LocalDateTime getImportEndTime() { return importEndTime; }
    public void setImportEndTime(LocalDateTime importEndTime) { this.importEndTime = importEndTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getTotalCardsProcessed() { return totalCardsProcessed; }
    public void setTotalCardsProcessed(int totalCardsProcessed) { this.totalCardsProcessed = totalCardsProcessed; }

    public int getCardsAdded() { return cardsAdded; }
    public void setCardsAdded(int cardsAdded) { this.cardsAdded = cardsAdded; }

    public int getCardsUpdated() { return cardsUpdated; }
    public void setCardsUpdated(int cardsUpdated) { this.cardsUpdated = cardsUpdated; }

    public int getCardsSkipped() { return cardsSkipped; }
    public void setCardsSkipped(int cardsSkipped) { this.cardsSkipped = cardsSkipped; }

    public int getErrors() { return errors; }
    public void setErrors(int errors) { this.errors = errors; }

    public List<CardDelta> getDeltas() { return deltas; }
    public void setDeltas(List<CardDelta> deltas) { this.deltas = deltas; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    
    public List<TCGImportResult> getTcgResults() { return tcgResults; }
    public void setTcgResults(List<TCGImportResult> tcgResults) { this.tcgResults = tcgResults; }

    public long getDurationMinutes() {
        if (importStartTime != null && importEndTime != null) {
            return java.time.Duration.between(importStartTime, importEndTime).toMinutes();
        }
        return 0;
    }
    
    /**
     * Inner class for individual TCG import results
     */
    public static class TCGImportResult {
        private TCGType tcgType;
        private String status;
        private int cardsProcessed;
        private int cardsAdded;
        private int cardsUpdated;
        private int errors;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String errorMessage;
        
        public TCGImportResult() {}
        
        public TCGImportResult(TCGType tcgType) {
            this.tcgType = tcgType;
            this.startTime = LocalDateTime.now();
        }
        
        // Getters and Setters
        public TCGType getTcgType() { return tcgType; }
        public void setTcgType(TCGType tcgType) { this.tcgType = tcgType; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public int getCardsProcessed() { return cardsProcessed; }
        public void setCardsProcessed(int cardsProcessed) { this.cardsProcessed = cardsProcessed; }
        
        public int getCardsAdded() { return cardsAdded; }
        public void setCardsAdded(int cardsAdded) { this.cardsAdded = cardsAdded; }
        
        public int getCardsUpdated() { return cardsUpdated; }
        public void setCardsUpdated(int cardsUpdated) { this.cardsUpdated = cardsUpdated; }
        
        public int getErrors() { return errors; }
        public void setErrors(int errors) { this.errors = errors; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        
        public long getDurationMinutes() {
            if (startTime != null && endTime != null) {
                return java.time.Duration.between(startTime, endTime).toMinutes();
            }
            return 0;
        }
        
        public String getTcgDisplayName() {
            return tcgType != null ? tcgType.getDisplayName() : "Unknown";
        }
    }
}
