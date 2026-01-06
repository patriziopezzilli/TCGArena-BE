package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "shop_rewards")
public class ShopReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "shop_id", nullable = false)
    @JsonIgnore // Prevent full object serialization, use getShopId() instead
    private Shop shop;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private Integer costPoints;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopRewardType type = ShopRewardType.DISCOUNT;

    // For DISCOUNT type: percentage or fixed amount
    private Double discountValue;
    private Boolean isPercentage = true;

    // For PRODUCT type: product name/description
    private String productName;

    // Stock management
    private Integer stockQuantity; // null = unlimited
    private Integer claimedCount = 0;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    // Expose shopId directly for API serialization
    public Long getShopId() {
        return shop != null ? shop.getId() : null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getCostPoints() {
        return costPoints;
    }

    public void setCostPoints(Integer costPoints) {
        this.costPoints = costPoints;
    }

    public ShopRewardType getType() {
        return type;
    }

    public void setType(ShopRewardType type) {
        this.type = type;
    }

    public Double getDiscountValue() {
        return discountValue;
    }

    public void setDiscountValue(Double discountValue) {
        this.discountValue = discountValue;
    }

    public Boolean getIsPercentage() {
        return isPercentage;
    }

    public void setIsPercentage(Boolean isPercentage) {
        this.isPercentage = isPercentage;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public Integer getClaimedCount() {
        return claimedCount;
    }

    public void setClaimedCount(Integer claimedCount) {
        this.claimedCount = claimedCount;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isAvailable() {
        if (!isActive) return false;
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) return false;
        if (stockQuantity != null && claimedCount >= stockQuantity) return false;
        return true;
    }
}
