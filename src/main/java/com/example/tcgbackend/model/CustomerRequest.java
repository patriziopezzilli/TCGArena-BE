package com.example.tcgbackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "shop_id", nullable = false)
    @JsonProperty("shop_id")
    private String shopId;
    
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private String userId;
    
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
    
    public enum RequestType {
        CARD_SEARCH,
        PRICE_CHECK,
        BULK_SALE,
        TRADE_IN,
        REPAIR_SERVICE,
        CUSTOM_ORDER,
        GENERAL;
        
        public String getDisplayName() {
            return switch (this) {
                case CARD_SEARCH -> "Card Search";
                case PRICE_CHECK -> "Price Check";
                case BULK_SALE -> "Bulk Sale";
                case TRADE_IN -> "Trade In";
                case REPAIR_SERVICE -> "Repair Service";
                case CUSTOM_ORDER -> "Custom Order";
                case GENERAL -> "General Question";
            };
        }
        
        public String getIcon() {
            return switch (this) {
                case CARD_SEARCH -> "magnifyingglass";
                case PRICE_CHECK -> "dollarsign.circle";
                case BULK_SALE -> "cart.fill";
                case TRADE_IN -> "arrow.triangle.2.circlepath";
                case REPAIR_SERVICE -> "wrench.and.screwdriver";
                case CUSTOM_ORDER -> "star.fill";
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
