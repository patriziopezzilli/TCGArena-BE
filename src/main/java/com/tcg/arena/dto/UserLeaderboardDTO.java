package com.tcg.arena.dto;

public class UserLeaderboardDTO {
    private int rank;
    private Long userId;
    private String username;
    private String profileImageUrl;
    private int score;
    private boolean isPremium;

    public UserLeaderboardDTO() {
    }

    public UserLeaderboardDTO(int rank, Long userId, String username, String profileImageUrl, int score,
            boolean isPremium) {
        this.rank = rank;
        this.userId = userId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
        this.score = score;
        this.isPremium = isPremium;
    }

    // Getters and Setters
    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
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

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public boolean isPremium() {
        return isPremium;
    }

    public void setPremium(boolean premium) {
        isPremium = premium;
    }
}
