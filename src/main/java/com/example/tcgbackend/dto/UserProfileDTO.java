package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.model.UserLocation;

import java.time.LocalDateTime;
import java.util.List;

public class UserProfileDTO {
    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private LocalDateTime joinDate;
    private LocalDateTime lastActiveDate;
    private Boolean isVerified;
    private Integer level;
    private Integer experience;
    private UserStatsDTO stats;
    private List<UserBadgeDTO> badges;
    private String favoriteCard;
    private TCGType preferredTCG;
    private UserLocation location;
    private Integer followersCount;
    private Integer followingCount;
    private Boolean isFollowedByCurrentUser;

    // Constructors, Getters, Setters
    public UserProfileDTO() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public LocalDateTime getJoinDate() { return joinDate; }
    public void setJoinDate(LocalDateTime joinDate) { this.joinDate = joinDate; }

    public LocalDateTime getLastActiveDate() { return lastActiveDate; }
    public void setLastActiveDate(LocalDateTime lastActiveDate) { this.lastActiveDate = lastActiveDate; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Integer getExperience() { return experience; }
    public void setExperience(Integer experience) { this.experience = experience; }

    public UserStatsDTO getStats() { return stats; }
    public void setStats(UserStatsDTO stats) { this.stats = stats; }

    public List<UserBadgeDTO> getBadges() { return badges; }
    public void setBadges(List<UserBadgeDTO> badges) { this.badges = badges; }

    public String getFavoriteCard() { return favoriteCard; }
    public void setFavoriteCard(String favoriteCard) { this.favoriteCard = favoriteCard; }

    public TCGType getPreferredTCG() { return preferredTCG; }
    public void setPreferredTCG(TCGType preferredTCG) { this.preferredTCG = preferredTCG; }

    public UserLocation getLocation() { return location; }
    public void setLocation(UserLocation location) { this.location = location; }

    public Integer getFollowersCount() { return followersCount; }
    public void setFollowersCount(Integer followersCount) { this.followersCount = followersCount; }

    public Integer getFollowingCount() { return followingCount; }
    public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }

    public Boolean getIsFollowedByCurrentUser() { return isFollowedByCurrentUser; }
    public void setIsFollowedByCurrentUser(Boolean isFollowedByCurrentUser) { this.isFollowedByCurrentUser = isFollowedByCurrentUser; }
}