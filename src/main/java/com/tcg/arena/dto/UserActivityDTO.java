package com.tcg.arena.dto;

import com.tcg.arena.model.ActivityType;

import java.time.LocalDateTime;

public class UserActivityDTO {
    private Long id;
    private ActivityType activityType;
    private String description;
    private LocalDateTime timestamp;
    private String metadata;

    public UserActivityDTO() {}

    public UserActivityDTO(Long id, ActivityType activityType, String description,
                          LocalDateTime timestamp, String metadata) {
        this.id = id;
        this.activityType = activityType;
        this.description = description;
        this.timestamp = timestamp;
        this.metadata = metadata;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}