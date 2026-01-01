package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String displayName;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @Column(nullable = false)
    private String password;

    private String profileImageUrl;
    private String bio;

    @Column(nullable = false)
    private LocalDateTime dateJoined;

    @Column(nullable = false)
    private Boolean isPremium = false;

    @Column(nullable = false)
    private Boolean isMerchant = false;

    @Column(nullable = false)
    private Boolean isAdmin = false;

    @Column(nullable = false)
    private Boolean isPrivate = false; // If true, hide from Discover section

    private Long shopId;

    @Column(nullable = false)
    private Integer points = 0;

    @JsonProperty("favorite_game")
    @Enumerated(EnumType.STRING)
    private TCGType favoriteGame; // Deprecated, kept for backward compatibility

    @Column(name = "favorite_tcg_types")
    private String favoriteTCGTypesString; // Comma-separated TCG types: "POKEMON,MAGIC,YUGIOH"

    @Embedded
    private UserLocation location;

    private String deviceToken;

    // Trade Rating System
    @Column(nullable = false)
    private Integer tradeRatingSum = 0; // Sum of all ratings received

    @Column(nullable = false)
    private Integer tradeRatingCount = 0; // Number of ratings received

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public LocalDateTime getDateJoined() {
        return dateJoined;
    }

    public void setDateJoined(LocalDateTime dateJoined) {
        this.dateJoined = dateJoined;
    }

    public Boolean getIsPremium() {
        return isPremium;
    }

    public void setIsPremium(Boolean isPremium) {
        this.isPremium = isPremium;
    }

    public Boolean getIsMerchant() {
        return isMerchant;
    }

    public void setIsMerchant(Boolean isMerchant) {
        this.isMerchant = isMerchant;
    }

    public Boolean getIsAdmin() {
        return isAdmin;
    }

    public void setIsAdmin(Boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public Boolean getIsPrivate() {
        return isPrivate != null ? isPrivate : false;
    }

    public void setIsPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public TCGType getFavoriteGame() {
        return favoriteGame;
    }

    public void setFavoriteGame(TCGType favoriteGame) {
        this.favoriteGame = favoriteGame;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getFavoriteTCGTypesString() {
        return favoriteTCGTypesString;
    }

    public void setFavoriteTCGTypesString(String favoriteTCGTypesString) {
        this.favoriteTCGTypesString = favoriteTCGTypesString;
    }

    // Helper method to get TCG types as a list (for JSON serialization)
    @JsonProperty("favorite_games")
    public List<TCGType> getFavoriteTCGTypes() {
        if (favoriteTCGTypesString == null || favoriteTCGTypesString.isEmpty()) {
            return new ArrayList<>();
        }
        List<TCGType> types = new ArrayList<>();
        for (String typeStr : favoriteTCGTypesString.split(",")) {
            try {
                types.add(TCGType.valueOf(typeStr.trim()));
            } catch (IllegalArgumentException e) {
                // Skip invalid types
            }
        }
        return types;
    }

    // Helper method to set TCG types from a list (for JSON deserialization)
    @JsonProperty("favorite_games")
    public void setFavoriteTCGTypes(List<TCGType> tcgTypes) {
        if (tcgTypes == null || tcgTypes.isEmpty()) {
            favoriteTCGTypesString = null;
        } else {
            favoriteTCGTypesString = tcgTypes.stream()
                    .map(TCGType::name)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(null);
        }
    }

    public Integer getTradeRatingSum() {
        return tradeRatingSum != null ? tradeRatingSum : 0;
    }

    public void setTradeRatingSum(Integer tradeRatingSum) {
        this.tradeRatingSum = tradeRatingSum;
    }

    public Integer getTradeRatingCount() {
        return tradeRatingCount != null ? tradeRatingCount : 0;
    }

    public void setTradeRatingCount(Integer tradeRatingCount) {
        this.tradeRatingCount = tradeRatingCount;
    }

    // Computed average trade rating (1-5 stars)
    @JsonProperty("trade_rating")
    public Double getTradeRating() {
        if (tradeRatingCount == null || tradeRatingCount == 0) {
            return null;
        }
        return (double) tradeRatingSum / tradeRatingCount;
    }

    // Helper method to add a new rating
    public void addTradeRating(int rating) {
        if (this.tradeRatingSum == null)
            this.tradeRatingSum = 0;
        if (this.tradeRatingCount == null)
            this.tradeRatingCount = 0;
        this.tradeRatingSum += rating;
        this.tradeRatingCount += 1;
    }
}