package com.tcg.arena.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcg.arena.model.TCGType;

public class RadarUserDto {
    private Long id;
    private String username;
    private String displayName;
    private Double latitude;
    private Double longitude;
    private TCGType favoriteTCG;
    private String profileImageUrl;

    @JsonProperty("isOnline")
    private boolean isOnline; // For "Live" status

    private java.util.List<RadarTradeEntry> wantList;
    private java.util.List<RadarTradeEntry> haveList;
    private java.util.List<RadarUserCard> cards;

    public RadarUserDto() {
    }

    public RadarUserDto(Long id, String username, String displayName, Double latitude, Double longitude,
            TCGType favoriteTCG, String profileImageUrl, boolean isOnline) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.favoriteTCG = favoriteTCG;
        this.profileImageUrl = profileImageUrl;
        this.isOnline = isOnline;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TCGType getFavoriteTCG() {
        return favoriteTCG;
    }

    public void setFavoriteTCG(TCGType favoriteTCG) {
        this.favoriteTCG = favoriteTCG;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public java.util.List<RadarTradeEntry> getWantList() {
        return wantList;
    }

    public void setWantList(java.util.List<RadarTradeEntry> wantList) {
        this.wantList = wantList;
    }

    public java.util.List<RadarTradeEntry> getHaveList() {
        return haveList;
    }

    public void setHaveList(java.util.List<RadarTradeEntry> haveList) {
        this.haveList = haveList;
    }

    public java.util.List<RadarUserCard> getCards() {
        return cards;
    }

    public void setCards(java.util.List<RadarUserCard> cards) {
        this.cards = cards;
    }
}
