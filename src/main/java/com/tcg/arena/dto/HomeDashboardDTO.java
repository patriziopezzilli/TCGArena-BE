package com.tcg.arena.dto;

import java.math.BigDecimal;

public class HomeDashboardDTO {
    private long nearbyShopsCount;
    private long upcomingTournamentsCount;
    private long collectionCount;
    private long deckCount;
    private BigDecimal totalCollectionValue;
    private long unreadNewsCount;
    private long pendingReservationsCount;
    private long activeRequestsCount;

    public HomeDashboardDTO() {
    }

    public HomeDashboardDTO(long nearbyShopsCount, long upcomingTournamentsCount, long collectionCount, long deckCount,
            BigDecimal totalCollectionValue, long unreadNewsCount, long pendingReservationsCount,
            long activeRequestsCount) {
        this.nearbyShopsCount = nearbyShopsCount;
        this.upcomingTournamentsCount = upcomingTournamentsCount;
        this.collectionCount = collectionCount;
        this.deckCount = deckCount;
        this.totalCollectionValue = totalCollectionValue;
        this.unreadNewsCount = unreadNewsCount;
        this.pendingReservationsCount = pendingReservationsCount;
        this.activeRequestsCount = activeRequestsCount;
    }

    public long getNearbyShopsCount() {
        return nearbyShopsCount;
    }

    public void setNearbyShopsCount(long nearbyShopsCount) {
        this.nearbyShopsCount = nearbyShopsCount;
    }

    public long getUpcomingTournamentsCount() {
        return upcomingTournamentsCount;
    }

    public void setUpcomingTournamentsCount(long upcomingTournamentsCount) {
        this.upcomingTournamentsCount = upcomingTournamentsCount;
    }

    public long getCollectionCount() {
        return collectionCount;
    }

    public void setCollectionCount(long collectionCount) {
        this.collectionCount = collectionCount;
    }

    public long getDeckCount() {
        return deckCount;
    }

    public void setDeckCount(long deckCount) {
        this.deckCount = deckCount;
    }

    public BigDecimal getTotalCollectionValue() {
        return totalCollectionValue;
    }

    public void setTotalCollectionValue(BigDecimal totalCollectionValue) {
        this.totalCollectionValue = totalCollectionValue;
    }

    public long getUnreadNewsCount() {
        return unreadNewsCount;
    }

    public void setUnreadNewsCount(long unreadNewsCount) {
        this.unreadNewsCount = unreadNewsCount;
    }

    public long getPendingReservationsCount() {
        return pendingReservationsCount;
    }

    public void setPendingReservationsCount(long pendingReservationsCount) {
        this.pendingReservationsCount = pendingReservationsCount;
    }

    public long getActiveRequestsCount() {
        return activeRequestsCount;
    }

    public void setActiveRequestsCount(long activeRequestsCount) {
        this.activeRequestsCount = activeRequestsCount;
    }
}
