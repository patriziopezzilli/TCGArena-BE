package com.example.tcgbackend.dto;

import java.time.LocalDateTime;

public class UserBadgeDTO {
    private String name;
    private String description;
    private String iconUrl;
    private LocalDateTime earnedDate;

    // Constructors, Getters, Setters
    public UserBadgeDTO() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIconUrl() { return iconUrl; }
    public void setIconUrl(String iconUrl) { this.iconUrl = iconUrl; }

    public LocalDateTime getEarnedDate() { return earnedDate; }
    public void setEarnedDate(LocalDateTime earnedDate) { this.earnedDate = earnedDate; }
}