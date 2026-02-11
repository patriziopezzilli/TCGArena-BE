package com.tcg.arena.dto;

import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.NewsType;
import com.tcg.arena.model.ShopNews;
import com.tcg.arena.model.TCGType;

import java.time.LocalDateTime;

/**
 * Unified DTO for news that can come from shops or broadcast
 */
public class NewsItemDTO {
    private Long id;
    private String title;
    private String content;
    private NewsType newsType;
    private LocalDateTime startDate;
    private LocalDateTime expiryDate;
    private String imageUrl;
    private Boolean isPinned;
    private String source; // "SHOP" or "BROADCAST"
    private Long shopId; // Only for shop news
    private String shopName; // Only for shop news
    private TCGType tcgType;
    private String externalUrl;
    private LocalDateTime createdAt;

    public NewsItemDTO() {
    }

    // Constructor from ShopNews
    public NewsItemDTO(ShopNews shopNews, String shopName) {
        this.id = shopNews.getId();
        this.title = shopNews.getTitle();
        this.content = shopNews.getContent();
        this.newsType = shopNews.getNewsType();
        this.startDate = shopNews.getStartDate();
        this.expiryDate = shopNews.getExpiryDate();
        this.imageUrl = shopNews.getImageUrl();
        this.isPinned = shopNews.getIsPinned();
        this.source = "SHOP";
        this.shopId = shopNews.getShopId();
        this.shopName = shopName;
        this.tcgType = shopNews.getTcgType();
        this.externalUrl = shopNews.getExternalUrl();
        this.createdAt = shopNews.getCreatedAt();
    }

    // Constructor from BroadcastNews
    public NewsItemDTO(BroadcastNews broadcastNews) {
        this.id = broadcastNews.getId();
        this.title = broadcastNews.getTitle();
        this.content = broadcastNews.getContent();
        this.newsType = broadcastNews.getNewsType();
        this.startDate = broadcastNews.getStartDate();
        this.expiryDate = broadcastNews.getExpiryDate();
        this.imageUrl = broadcastNews.getImageUrl();
        this.isPinned = broadcastNews.getIsPinned();
        this.source = "BROADCAST";
        this.shopId = null;
        this.shopName = null;
        this.tcgType = broadcastNews.getTcgType();
        this.externalUrl = broadcastNews.getExternalUrl();
        this.createdAt = broadcastNews.getCreatedAt();
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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
