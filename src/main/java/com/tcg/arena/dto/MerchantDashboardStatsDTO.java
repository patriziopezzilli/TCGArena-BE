package com.tcg.arena.dto;

public class MerchantDashboardStatsDTO {
    private long inventoryCount;
    private long activeReservations;
    private long upcomingTournaments;
    private long pendingRequests;
    private long subscriberCount;

    public MerchantDashboardStatsDTO() {}

    public MerchantDashboardStatsDTO(long inventoryCount, long activeReservations,
                                   long upcomingTournaments, long pendingRequests,
                                   long subscriberCount) {
        this.inventoryCount = inventoryCount;
        this.activeReservations = activeReservations;
        this.upcomingTournaments = upcomingTournaments;
        this.pendingRequests = pendingRequests;
        this.subscriberCount = subscriberCount;
    }

    // Getters and setters
    public long getInventoryCount() { return inventoryCount; }
    public void setInventoryCount(long inventoryCount) { this.inventoryCount = inventoryCount; }

    public long getActiveReservations() { return activeReservations; }
    public void setActiveReservations(long activeReservations) { this.activeReservations = activeReservations; }

    public long getUpcomingTournaments() { return upcomingTournaments; }
    public void setUpcomingTournaments(long upcomingTournaments) { this.upcomingTournaments = upcomingTournaments; }

    public long getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(long pendingRequests) { this.pendingRequests = pendingRequests; }

    public long getSubscriberCount() { return subscriberCount; }
    public void setSubscriberCount(long subscriberCount) { this.subscriberCount = subscriberCount; }
}