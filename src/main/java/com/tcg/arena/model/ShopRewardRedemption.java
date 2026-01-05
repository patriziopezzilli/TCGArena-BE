package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_reward_redemptions")
public class ShopRewardRedemption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shop_reward_id", nullable = false)
    private ShopReward shopReward;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer pointsSpent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RedemptionStatus status = RedemptionStatus.PENDING;

    // Unique code for the user to present at the shop
    @Column(unique = true, nullable = false)
    private String redemptionCode;

    // Tracking/voucher code provided by merchant (like admin fulfillment)
    private String trackingCode;
    private String voucherCode;

    // Notes from merchant
    @Column(length = 500)
    private String merchantNotes;

    @Column(nullable = false)
    private LocalDateTime redeemedAt;

    private LocalDateTime fulfilledAt;

    @PrePersist
    protected void onCreate() {
        redeemedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ShopReward getShopReward() {
        return shopReward;
    }

    public void setShopReward(ShopReward shopReward) {
        this.shopReward = shopReward;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getPointsSpent() {
        return pointsSpent;
    }

    public void setPointsSpent(Integer pointsSpent) {
        this.pointsSpent = pointsSpent;
    }

    public RedemptionStatus getStatus() {
        return status;
    }

    public void setStatus(RedemptionStatus status) {
        this.status = status;
    }

    public String getRedemptionCode() {
        return redemptionCode;
    }

    public void setRedemptionCode(String redemptionCode) {
        this.redemptionCode = redemptionCode;
    }

    public String getTrackingCode() {
        return trackingCode;
    }

    public void setTrackingCode(String trackingCode) {
        this.trackingCode = trackingCode;
    }

    public String getVoucherCode() {
        return voucherCode;
    }

    public void setVoucherCode(String voucherCode) {
        this.voucherCode = voucherCode;
    }

    public String getMerchantNotes() {
        return merchantNotes;
    }

    public void setMerchantNotes(String merchantNotes) {
        this.merchantNotes = merchantNotes;
    }

    public LocalDateTime getRedeemedAt() {
        return redeemedAt;
    }

    public void setRedeemedAt(LocalDateTime redeemedAt) {
        this.redeemedAt = redeemedAt;
    }

    public LocalDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(LocalDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }
}
