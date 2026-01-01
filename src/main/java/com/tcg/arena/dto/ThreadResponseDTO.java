package com.tcg.arena.dto;

import java.time.LocalDateTime;

public class ThreadResponseDTO {

    private Long id;
    private Long responderId;
    private String responderUsername;
    private String responderDisplayName;
    private String responderAvatarUrl;
    private String content;
    private LocalDateTime createdAt;

    // Constructors
    public ThreadResponseDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResponderId() {
        return responderId;
    }

    public void setResponderId(Long responderId) {
        this.responderId = responderId;
    }

    public String getResponderUsername() {
        return responderUsername;
    }

    public void setResponderUsername(String responderUsername) {
        this.responderUsername = responderUsername;
    }

    public String getResponderDisplayName() {
        return responderDisplayName;
    }

    public void setResponderDisplayName(String responderDisplayName) {
        this.responderDisplayName = responderDisplayName;
    }

    public String getResponderAvatarUrl() {
        return responderAvatarUrl;
    }

    public void setResponderAvatarUrl(String responderAvatarUrl) {
        this.responderAvatarUrl = responderAvatarUrl;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
