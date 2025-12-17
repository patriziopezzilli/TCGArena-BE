package com.tcg.arena.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class ImportJob {
    private String id;
    private TCGType tcgType;
    private JobStatus status;
    private int progressPercent;
    private int totalItems;
    private int processedItems;
    private String message;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public ImportJob(TCGType tcgType) {
        this.id = UUID.randomUUID().toString();
        this.tcgType = tcgType;
        this.status = JobStatus.PENDING;
        this.progressPercent = 0;
        this.message = "Job initialized";
        this.startTime = LocalDateTime.now();
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
        if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
            this.endTime = LocalDateTime.now();
        }
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getProcessedItems() {
        return processedItems;
    }

    public void setProcessedItems(int processedItems) {
        this.processedItems = processedItems;
        // Auto-calculate percent if total is known
        if (totalItems > 0) {
            this.progressPercent = (int) ((double) processedItems / totalItems * 100);
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}
