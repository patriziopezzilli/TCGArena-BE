package com.tcg.arena.dto;

import com.tcg.arena.model.PendingReview;
import java.time.LocalDateTime;

public class PendingReviewDTO {

    private Long id;
    private Long conversationId;
    private Long revieweeId;
    private String revieweeUsername;
    private String revieweeDisplayName;
    private String revieweeAvatarUrl;
    private LocalDateTime createdAt;
    private String tradeContext; // JSON with card info
    private Integer rating; // null if pending
    private String comment;
    private Boolean isPending;

    public PendingReviewDTO() {
    }

    public static PendingReviewDTO fromEntity(PendingReview review) {
        PendingReviewDTO dto = new PendingReviewDTO();
        dto.setId(review.getId());
        dto.setConversationId(review.getConversationId());
        dto.setRevieweeId(review.getReviewee().getId());
        dto.setRevieweeUsername(review.getReviewee().getUsername());
        dto.setRevieweeDisplayName(review.getReviewee().getDisplayName());
        dto.setRevieweeAvatarUrl(review.getReviewee().getProfileImageUrl());
        dto.setCreatedAt(review.getCreatedAt());
        dto.setTradeContext(review.getTradeContext());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setIsPending(review.isPending());
        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public void setConversationId(Long conversationId) {
        this.conversationId = conversationId;
    }

    public Long getRevieweeId() {
        return revieweeId;
    }

    public void setRevieweeId(Long revieweeId) {
        this.revieweeId = revieweeId;
    }

    public String getRevieweeUsername() {
        return revieweeUsername;
    }

    public void setRevieweeUsername(String revieweeUsername) {
        this.revieweeUsername = revieweeUsername;
    }

    public String getRevieweeDisplayName() {
        return revieweeDisplayName;
    }

    public void setRevieweeDisplayName(String revieweeDisplayName) {
        this.revieweeDisplayName = revieweeDisplayName;
    }

    public String getRevieweeAvatarUrl() {
        return revieweeAvatarUrl;
    }

    public void setRevieweeAvatarUrl(String revieweeAvatarUrl) {
        this.revieweeAvatarUrl = revieweeAvatarUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getTradeContext() {
        return tradeContext;
    }

    public void setTradeContext(String tradeContext) {
        this.tradeContext = tradeContext;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Boolean getIsPending() {
        return isPending;
    }

    public void setIsPending(Boolean isPending) {
        this.isPending = isPending;
    }
}
