package com.tcg.arena.dto;

public class CommunityStatsDTO {

    private int unreadMessages;
    private int newUsersToday;
    private int activeTradeListings;
    private int totalUsers;
    private int upcomingEvents;
    private int myEventsCount;

    // Default constructor
    public CommunityStatsDTO() {
    }

    // Getters and Setters
    public int getUnreadMessages() {
        return unreadMessages;
    }

    public void setUnreadMessages(int unreadMessages) {
        this.unreadMessages = unreadMessages;
    }

    public int getNewUsersToday() {
        return newUsersToday;
    }

    public void setNewUsersToday(int newUsersToday) {
        this.newUsersToday = newUsersToday;
    }

    public int getActiveTradeListings() {
        return activeTradeListings;
    }

    public void setActiveTradeListings(int activeTradeListings) {
        this.activeTradeListings = activeTradeListings;
    }

    public int getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(int totalUsers) {
        this.totalUsers = totalUsers;
    }

    public int getUpcomingEvents() {
        return upcomingEvents;
    }

    public void setUpcomingEvents(int upcomingEvents) {
        this.upcomingEvents = upcomingEvents;
    }

    public int getMyEventsCount() {
        return myEventsCount;
    }

    public void setMyEventsCount(int myEventsCount) {
        this.myEventsCount = myEventsCount;
    }
}
