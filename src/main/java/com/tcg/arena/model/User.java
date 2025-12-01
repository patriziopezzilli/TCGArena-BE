package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
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

    @Column(nullable = false)
    private String password;

    private String profileImageUrl;

    @Column(nullable = false)
    private LocalDateTime dateJoined;

    @Column(nullable = false)
    private Boolean isPremium = false;

    @Column(nullable = false)
    private Boolean isMerchant = false;

    private Long shopId;

    @Column(nullable = false)
    private Integer points = 0;

    @JsonProperty("favorite_game")
    @Enumerated(EnumType.STRING)
    private TCGType favoriteGame; // Deprecated, kept for backward compatibility

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonProperty("favorite_games")
    private List<UserFavoriteTCG> favoriteTCGs = new ArrayList<>();

    @Embedded
    private UserLocation location;

    private String deviceToken;

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

    public List<UserFavoriteTCG> getFavoriteTCGs() {
        return favoriteTCGs;
    }

    public void setFavoriteTCGs(List<UserFavoriteTCG> favoriteTCGs) {
        this.favoriteTCGs = favoriteTCGs;
    }

    // Helper method to get TCG types as a list
    public List<TCGType> getFavoriteTCGTypes() {
        return favoriteTCGs.stream()
                .map(UserFavoriteTCG::getTcgType)
                .toList();
    }

    // Helper method to set TCG types from a list
    public void setFavoriteTCGTypes(List<TCGType> tcgTypes) {
        favoriteTCGs.clear();
        if (tcgTypes != null) {
            for (TCGType tcgType : tcgTypes) {
                favoriteTCGs.add(new UserFavoriteTCG(this, tcgType));
            }
        }
    }
}