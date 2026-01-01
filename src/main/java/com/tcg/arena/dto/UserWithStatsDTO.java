package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserLocation;
import com.tcg.arena.model.UserStats;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO combining User data with their stats for list endpoints.
 * Used in /api/users and /api/users/leaderboard to return complete user data.
 */
public class UserWithStatsDTO {

    // User fields
    private Long id;
    private String email;
    private String username;
    private String displayName;
    private String profileImageUrl;
    private String bio;
    private LocalDateTime dateJoined;
    private Boolean isPremium;
    private Boolean isMerchant;
    private Long shopId;
    private Integer points;
    private TCGType favoriteGame;
    private List<TCGType> favoriteGames;
    private UserLocation location;
    private Double tradeRating;
    private Integer tradeRatingCount;

    // Embedded stats
    private UserStatsDTO stats;

    // Constructors
    public UserWithStatsDTO() {
    }

    /**
     * Create from User entity and UserStats entity
     */
    public static UserWithStatsDTO fromUserAndStats(User user, UserStats stats) {
        UserWithStatsDTO dto = new UserWithStatsDTO();

        // Copy user fields
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.username = user.getUsername();
        dto.displayName = user.getDisplayName();
        dto.profileImageUrl = user.getProfileImageUrl();
        dto.dateJoined = user.getDateJoined();
        dto.isPremium = user.getIsPremium();
        dto.isMerchant = user.getIsMerchant();
        dto.shopId = user.getShopId();
        dto.points = user.getPoints();
        dto.favoriteGame = user.getFavoriteGame();
        dto.favoriteGames = user.getFavoriteTCGTypes();
        dto.location = user.getLocation();
        dto.bio = user.getBio();
        dto.tradeRating = user.getTradeRating();
        dto.tradeRatingCount = user.getTradeRatingCount();

        // Build stats DTO
        if (stats != null) {
            UserStatsDTO statsDTO = new UserStatsDTO();
            statsDTO.setTotalCards(stats.getTotalCards());
            statsDTO.setTotalDecks(stats.getTotalDecks());
            statsDTO.setTournamentsPlayed(stats.getTotalTournaments());
            statsDTO.setTournamentsWon(stats.getTotalWins());
            statsDTO.setWinRate(stats.getWinRate());
            dto.stats = statsDTO;
        } else {
            // Return empty stats if none found
            UserStatsDTO statsDTO = new UserStatsDTO();
            statsDTO.setTotalCards(0);
            statsDTO.setTotalDecks(0);
            statsDTO.setTournamentsPlayed(0);
            statsDTO.setTournamentsWon(0);
            statsDTO.setWinRate(0.0);
            dto.stats = statsDTO;
        }

        return dto;
    }

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

    public List<TCGType> getFavoriteGames() {
        return favoriteGames;
    }

    public void setFavoriteGames(List<TCGType> favoriteGames) {
        this.favoriteGames = favoriteGames;
    }

    public UserLocation getLocation() {
        return location;
    }

    public void setLocation(UserLocation location) {
        this.location = location;
    }

    public Double getTradeRating() {
        return tradeRating;
    }

    public void setTradeRating(Double tradeRating) {
        this.tradeRating = tradeRating;
    }

    public Integer getTradeRatingCount() {
        return tradeRatingCount;
    }

    public void setTradeRatingCount(Integer tradeRatingCount) {
        this.tradeRatingCount = tradeRatingCount;
    }

    public UserStatsDTO getStats() {
        return stats;
    }

    public void setStats(UserStatsDTO stats) {
        this.stats = stats;
    }
}
