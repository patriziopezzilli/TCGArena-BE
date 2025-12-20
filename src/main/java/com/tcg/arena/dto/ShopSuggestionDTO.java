package com.tcg.arena.dto;

import com.tcg.arena.model.ShopSuggestion;
import com.tcg.arena.model.ShopSuggestion.SuggestionStatus;
import java.time.LocalDateTime;

public class ShopSuggestionDTO {
    private Long id;
    private String shopName;
    private String city;
    private Double latitude;
    private Double longitude;
    private Long userId;
    private String username;
    private SuggestionStatus status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ShopSuggestionDTO() {
    }

    public ShopSuggestionDTO(ShopSuggestion suggestion) {
        this.id = suggestion.getId();
        this.shopName = suggestion.getShopName();
        this.city = suggestion.getCity();
        this.latitude = suggestion.getLatitude();
        this.longitude = suggestion.getLongitude();
        this.userId = suggestion.getUserId();
        this.status = suggestion.getStatus();
        this.notes = suggestion.getNotes();
        this.createdAt = suggestion.getCreatedAt();
        this.updatedAt = suggestion.getUpdatedAt();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
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

    public SuggestionStatus getStatus() {
        return status;
    }

    public void setStatus(SuggestionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
}
