package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reward_transactions")
public class RewardTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer pointsChange; // positive for earning, negative for spending

    @Column(length = 500)
    private String description;

    private Long rewardId; // null if not redeeming a reward

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // New fields for reward fulfillment tracking
    private String voucherCode; // For digital rewards (voucher/coupon code)

    private String trackingNumber; // For physical rewards (shipping tracking)

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private RewardFulfillmentStatus status = RewardFulfillmentStatus.PENDING;

    // Reward fulfillment status enum
    public enum RewardFulfillmentStatus {
        PENDING, // Reward redeemed, awaiting processing
        PROCESSING, // Being prepared
        SHIPPED, // Physical item shipped
        DELIVERED, // Item received/code sent
        COMPLETED // Fully fulfilled
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getPointsChange() {
        return pointsChange;
    }

    public void setPointsChange(Integer pointsChange) {
        this.pointsChange = pointsChange;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getRewardId() {
        return rewardId;
    }

    public void setRewardId(Long rewardId) {
        this.rewardId = rewardId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public RewardFulfillmentStatus getStatus() {
        return status;
    }

    public void setStatus(RewardFulfillmentStatus status) {
        this.status = status;
    }
}