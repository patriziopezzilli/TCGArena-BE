package com.tcg.arena.model;

/**
 * API subscription plans for Arena API.
 * Each plan has different rate limits and batch sizes.
 */
public enum ArenaApiPlan {
    FREE(100, 100), // 100 requests/day, 100 cards/batch
    STARTER(1000, 100), // 1,000 requests/day, 100 cards/batch
    PRO(10000, 200), // 10,000 requests/day, 200 cards/batch
    ENTERPRISE(-1, 200); // Unlimited requests, 200 cards/batch

    private final int dailyRequestLimit;
    private final int maxBatchSize;

    ArenaApiPlan(int dailyRequestLimit, int maxBatchSize) {
        this.dailyRequestLimit = dailyRequestLimit;
        this.maxBatchSize = maxBatchSize;
    }

    public int getDailyRequestLimit() {
        return dailyRequestLimit;
    }

    public int getMaxBatchSize() {
        return maxBatchSize;
    }

    /**
     * Returns true if this plan has unlimited requests.
     */
    public boolean isUnlimited() {
        return dailyRequestLimit < 0;
    }

    /**
     * Returns the display name for UI.
     */
    public String getDisplayName() {
        return switch (this) {
            case FREE -> "Free";
            case STARTER -> "Starter";
            case PRO -> "Pro";
            case ENTERPRISE -> "Enterprise";
        };
    }

    /**
     * Returns formatted request limit for display.
     */
    public String getFormattedLimit() {
        if (isUnlimited()) {
            return "Unlimited";
        }
        return String.format("%,d/day", dailyRequestLimit);
    }
}
