package com.tcg.arena.dto;

import com.tcg.arena.model.GlobalChatMessage;
import java.time.LocalDateTime;

/**
 * DTO for global chat messages sent over WebSocket.
 */
public class GlobalChatMessageDto {
    private Long id;
    private Long userId;
    private String username;
    private String displayName;
    private String content;
    private LocalDateTime timestamp;
    private String userAvatarUrl;

    // Default constructor
    public GlobalChatMessageDto() {
    }

    // Constructor from entity
    public GlobalChatMessageDto(GlobalChatMessage message) {
        this.id = message.getId();
        this.userId = message.getUserId();
        this.username = message.getUsername();
        this.displayName = message.getDisplayName();
        this.content = message.getContent();
        this.timestamp = message.getTimestamp();
        this.userAvatarUrl = message.getProfileImageUrl();
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }
}
