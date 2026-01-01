package com.tcg.arena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class CommunityThreadDTO {

    private Long id;
    private Long creatorId;
    private String creatorUsername;
    private String creatorDisplayName;
    private String creatorAvatarUrl;
    private String tcgType;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private int responseCount;

    @JsonProperty("isCreatedByCurrentUser")
    private boolean isCreatedByCurrentUser;

    @JsonProperty("hasCurrentUserResponded")
    private boolean hasCurrentUserResponded;

    private List<ThreadResponseDTO> responses;

    // Constructors
    public CommunityThreadDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public String getCreatorDisplayName() {
        return creatorDisplayName;
    }

    public void setCreatorDisplayName(String creatorDisplayName) {
        this.creatorDisplayName = creatorDisplayName;
    }

    public String getCreatorAvatarUrl() {
        return creatorAvatarUrl;
    }

    public void setCreatorAvatarUrl(String creatorAvatarUrl) {
        this.creatorAvatarUrl = creatorAvatarUrl;
    }

    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public int getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    public boolean isCreatedByCurrentUser() {
        return isCreatedByCurrentUser;
    }

    public void setCreatedByCurrentUser(boolean createdByCurrentUser) {
        isCreatedByCurrentUser = createdByCurrentUser;
    }

    public boolean isHasCurrentUserResponded() {
        return hasCurrentUserResponded;
    }

    public void setHasCurrentUserResponded(boolean hasCurrentUserResponded) {
        this.hasCurrentUserResponded = hasCurrentUserResponded;
    }

    public List<ThreadResponseDTO> getResponses() {
        return responses;
    }

    public void setResponses(List<ThreadResponseDTO> responses) {
        this.responses = responses;
    }
}
