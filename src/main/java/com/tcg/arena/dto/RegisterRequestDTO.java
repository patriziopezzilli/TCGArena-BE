package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class RegisterRequestDTO {
    private String email;
    private String username;

    @JsonProperty("display_name")
    private String displayName;

    private String password;

    @JsonProperty("favorite_games")
    private List<TCGType> favoriteGames;

    // Location fields
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    private String locale;

    // Getters and Setters
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

    public List<TCGType> getFavoriteGames() {
        return favoriteGames;
    }

    public void setFavoriteGames(List<TCGType> favoriteGames) {
        this.favoriteGames = favoriteGames;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
