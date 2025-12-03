package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a customer request to a shop
 */
@Entity
@Table(name = "customer_requests", indexes = {
    @Index(name = "idx_shop_id", columnList = "shop_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_type", columnList = "type")
})
public class CustomerRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "shop_id", nullable = false)
    @JsonProperty("shop_id")
    private Long shopId;
    
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType type;
    
    @Column(nullable = false, length = 200)
    private String title;
    
    @Column(nullable = false, length = 2000)
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;
    
    @Column(name = "has_unread_messages", nullable = false)
    @JsonProperty("has_unread_messages")
    private Boolean hasUnreadMessages = false;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "resolved_at")
    @JsonProperty("resolved_at")
    private LocalDateTime resolvedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    private Shop shop;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    // Constructors
    public CustomerRequest() {
    }
    
    public CustomerRequest(String id, Long shopId, Long userId, RequestType type, String title, 
                          String description, RequestStatus status, Boolean hasUnreadMessages, 
                          LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.shopId = shopId;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.description = description;
        this.status = status;
        this.hasUnreadMessages = hasUnreadMessages;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.resolvedAt = resolvedAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Boolean getHasUnreadMessages() {
        return hasUnreadMessages;
    }

    public void setHasUnreadMessages(Boolean hasUnreadMessages) {
        this.hasUnreadMessages = hasUnreadMessages;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
    
    public Shop getShop() {
        return shop;
    }
    
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerRequest that = (CustomerRequest) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "CustomerRequest{" +
                "id='" + id + '\'' +
                ", shopId=" + shopId +
                ", userId=" + userId +
                ", type=" + type +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", hasUnreadMessages=" + hasUnreadMessages +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", resolvedAt=" + resolvedAt +
                '}';
    }
    
    public enum RequestType {
        AVAILABILITY,
        EVALUATION,
        SELL,
        BUY,
        TRADE,
        GENERAL;
        
        public String getDisplayName() {
            return switch (this) {
                case AVAILABILITY -> "Card Availability";
                case EVALUATION -> "Card Evaluation";
                case SELL -> "Sell Cards";
                case BUY -> "Buy Request";
                case TRADE -> "Trade Proposal";
                case GENERAL -> "General Inquiry";
            };
        }
        
        public String getIcon() {
            return switch (this) {
                case AVAILABILITY -> "magnifyingglass";
                case EVALUATION -> "dollarsign.circle";
                case SELL -> "arrow.up.circle";
                case BUY -> "cart";
                case TRADE -> "arrow.left.arrow.right";
                case GENERAL -> "questionmark.circle";
            };
        }
    }
    
    public enum RequestStatus {
        PENDING,
        ACCEPTED,
        COMPLETED,
        REJECTED,
        CANCELLED;
        
        public String getDisplayName() {
            return switch (this) {
                case PENDING -> "Pending";
                case ACCEPTED -> "Accepted";
                case COMPLETED -> "Completed";
                case REJECTED -> "Rejected";
                case CANCELLED -> "Cancelled";
            };
        }
        
        public String getColor() {
            return switch (this) {
                case PENDING -> "orange";
                case ACCEPTED -> "blue";
                case COMPLETED -> "green";
                case REJECTED -> "red";
                case CANCELLED -> "gray";
            };
        }
    }
}
