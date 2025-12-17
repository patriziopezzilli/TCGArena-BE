package com.tcg.arena.dto;

import com.tcg.arena.model.ActivityType;

import java.time.LocalDateTime;

public class UserActivityDTO {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private ActivityType activityType;
    private String description;
    private LocalDateTime timestamp;
    private String metadata;

    public UserActivityDTO() {
    }

    public UserActivityDTO(Long id, Long userId, String username, String displayName,
            ActivityType activityType, String description,
            LocalDateTime timestamp, String metadata) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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