package com.tcg.arena.controller;

import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.NewsType;
import com.tcg.arena.model.TCGType;
import java.time.LocalDateTime;

public class BroadcastNewsDTO {
    private Long id;
    private String title;
    private String content;
    private NewsType newsType;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String imageUrl;
    private Boolean isPinned;
    private Long createdBy;
    private TCGType tcgType;
    private String externalUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BroadcastNewsDTO() {
    }

    public BroadcastNewsDTO(BroadcastNews news) {
        this.id = news.getId();
        this.title = news.getTitle();
        this.content = news.getContent();
        this.newsType = news.getNewsType();
        this.startDate = news.getStartDate();
        this.expiryDate = news.getExpiryDate();
        this.imageUrl = news.getImageUrl();
        this.isPinned = news.getIsPinned();
        this.createdBy = news.getCreatedBy();
        this.tcgType = news.getTcgType();
        this.externalUrl = news.getExternalUrl();
        this.createdAt = news.getCreatedAt();
        this.updatedAt = news.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public NewsType getNewsType() {
        return newsType;
    }

    public void setNewsType(NewsType newsType) {
        this.newsType = newsType;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Boolean getIsPinned() {
        return isPinned;
    }

    public void setIsPinned(Boolean isPinned) {
        this.isPinned = isPinned;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }
}
