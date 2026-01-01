package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "pending_reviews")
public class PendingReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer; // The user who needs to leave the review

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id", nullable = false)
    private User reviewee; // The user who will receive the review

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "rating")
    private Integer rating; // 1-5 stars, null until submitted

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment; // Optional comment

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // null until submitted

    @Column(name = "trade_context", columnDefinition = "TEXT")
    private String tradeContext; // JSON with card info for display

    public PendingReview() {
    }

    public PendingReview(Long conversationId, User reviewer, User reviewee, String tradeContext) {
        this.conversationId = conversationId;
        this.reviewer = reviewer;
        this.reviewee = reviewee;
        this.tradeContext = tradeContext;
        this.createdAt = LocalDateTime.now();
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

    public User getReviewer() {
        return reviewer;
    }

    public void setReviewer(User reviewer) {
        this.reviewer = reviewer;
    }

    public User getReviewee() {
        return reviewee;
    }

    public void setReviewee(User reviewee) {
        this.reviewee = reviewee;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public String getTradeContext() {
        return tradeContext;
    }

    public void setTradeContext(String tradeContext) {
        this.tradeContext = tradeContext;
    }

    public boolean isPending() {
        return completedAt == null;
    }
}
