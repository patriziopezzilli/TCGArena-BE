package com.tcg.arena.dto;

/**
 * DTO for partial profile updates
 */
public class UpdateProfileRequest {
    private String displayName;
    private String bio;
    private String favoriteGame;

    public UpdateProfileRequest() {
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getFavoriteGame() {
        return favoriteGame;
    }

    public void setFavoriteGame(String favoriteGame) {
        this.favoriteGame = favoriteGame;
    }
}
