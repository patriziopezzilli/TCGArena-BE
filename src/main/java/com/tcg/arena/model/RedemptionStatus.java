package com.tcg.arena.model;

public enum RedemptionStatus {
    PENDING,    // User redeemed, waiting for merchant fulfillment
    FULFILLED,  // Merchant confirmed the reward was given
    CANCELLED,  // Cancelled (refunded points)
    EXPIRED     // Expired (not redeemed within time limit)
}
