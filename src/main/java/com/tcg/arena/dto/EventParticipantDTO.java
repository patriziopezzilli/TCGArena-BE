package com.tcg.arena.dto;

import com.tcg.arena.model.CommunityEventParticipant;

import java.time.LocalDateTime;

public class EventParticipantDTO {

    private Long userId;
    private String username;
    private String displayName;
    private String profileImageUrl;
    private LocalDateTime joinedAt;

    // Default constructor
    public EventParticipantDTO() {
    }

    // Factory method from entity
    public static EventParticipantDTO fromEntity(CommunityEventParticipant participant) {
        EventParticipantDTO dto = new EventParticipantDTO();
        dto.setUserId(participant.getUser().getId());
        dto.setUsername(participant.getUser().getUsername());
        dto.setDisplayName(participant.getUser().getDisplayName());
        dto.setProfileImageUrl(participant.getUser().getProfileImageUrl());
        dto.setJoinedAt(participant.getJoinedAt());
        return dto;
    }

    // Getters and Setters
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }
}
