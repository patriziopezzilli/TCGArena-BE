package com.tcg.arena.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for daily digest email
 */
public class DailyDigestEmailDTO {
    private String username;
    private LocalDateTime digestDate;
    
    // User Stats
    private UserStats userStats;
    
    // Platform Stats
    private PlatformStats platformStats;
    
    // Highlights
    private List<Highlight> highlights;
    
    // Recommendations
    private List<Recommendation> recommendations;

    public static class UserStats {
        private int newCards;
        private int newTrades;
        private int completedTrades;
        private int messagesReceived;
        private int profileViews;
        private int eventsNearby;
        private double collectionValueChange;

        // Getters and Setters
        public int getNewCards() { return newCards; }
        public void setNewCards(int newCards) { this.newCards = newCards; }

        public int getNewTrades() { return newTrades; }
        public void setNewTrades(int newTrades) { this.newTrades = newTrades; }

        public int getCompletedTrades() { return completedTrades; }
        public void setCompletedTrades(int completedTrades) { this.completedTrades = completedTrades; }

        public int getMessagesReceived() { return messagesReceived; }
        public void setMessagesReceived(int messagesReceived) { this.messagesReceived = messagesReceived; }

        public int getProfileViews() { return profileViews; }
        public void setProfileViews(int profileViews) { this.profileViews = profileViews; }

        public int getEventsNearby() { return eventsNearby; }
        public void setEventsNearby(int eventsNearby) { this.eventsNearby = eventsNearby; }

        public double getCollectionValueChange() { return collectionValueChange; }
        public void setCollectionValueChange(double collectionValueChange) { this.collectionValueChange = collectionValueChange; }
    }

    public static class PlatformStats {
        private int newUsers;
        private int activeTrades;
        private int upcomingEvents;
        private int newShops;

        // Getters and Setters
        public int getNewUsers() { return newUsers; }
        public void setNewUsers(int newUsers) { this.newUsers = newUsers; }

        public int getActiveTrades() { return activeTrades; }
        public void setActiveTrades(int activeTrades) { this.activeTrades = activeTrades; }

        public int getUpcomingEvents() { return upcomingEvents; }
        public void setUpcomingEvents(int upcomingEvents) { this.upcomingEvents = upcomingEvents; }

        public int getNewShops() { return newShops; }
        public void setNewShops(int newShops) { this.newShops = newShops; }
    }

    public static class Highlight {
        private String type; // TRADE_MATCH, NEW_EVENT, PRICE_ALERT, etc.
        private String title;
        private String description;
        private String actionUrl;
        private String icon;

        public Highlight() {}

        public Highlight(String type, String title, String description, String actionUrl, String icon) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.actionUrl = actionUrl;
            this.icon = icon;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
    }

    public static class Recommendation {
        private String type; // TRADE, EVENT, CARD
        private String title;
        private String description;
        private String imageUrl;
        private String actionUrl;

        public Recommendation() {}

        public Recommendation(String type, String title, String description, String imageUrl, String actionUrl) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.imageUrl = imageUrl;
            this.actionUrl = actionUrl;
        }

        // Getters and Setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

        public String getActionUrl() { return actionUrl; }
        public void setActionUrl(String actionUrl) { this.actionUrl = actionUrl; }
    }

    // Constructors
    public DailyDigestEmailDTO() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getDigestDate() { return digestDate; }
    public void setDigestDate(LocalDateTime digestDate) { this.digestDate = digestDate; }

    public UserStats getUserStats() { return userStats; }
    public void setUserStats(UserStats userStats) { this.userStats = userStats; }

    public PlatformStats getPlatformStats() { return platformStats; }
    public void setPlatformStats(PlatformStats platformStats) { this.platformStats = platformStats; }

    public List<Highlight> getHighlights() { return highlights; }
    public void setHighlights(List<Highlight> highlights) { this.highlights = highlights; }

    public List<Recommendation> getRecommendations() { return recommendations; }
    public void setRecommendations(List<Recommendation> recommendations) { this.recommendations = recommendations; }
}
