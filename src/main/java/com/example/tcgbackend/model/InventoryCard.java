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
 * Represents a card in a merchant's inventory
 * Links to CardTemplate for card details, stores shop-specific data
 */
@Entity
@Table(name = "inventory_cards", indexes = {
    @Index(name = "idx_shop_id", columnList = "shop_id"),
    @Index(name = "idx_card_template_id", columnList = "card_template_id"),
    @Index(name = "idx_condition", columnList = "condition")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventoryCard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "card_template_id", nullable = false)
    @JsonProperty("card_template_id")
    private String cardTemplateId;
    
    @Column(name = "shop_id", nullable = false)
    @JsonProperty("shop_id")
    private String shopId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardCondition condition;
    
    @Column(nullable = false)
    private Double price;
    
    @Column(nullable = false)
    private Integer quantity = 0;
    
    @Column(length = 1000)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships - will be populated via joins
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_template_id", insertable = false, updatable = false)
    @JsonProperty("card_template")
    private Card cardTemplate;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    private Shop shop;
    
    public enum CardCondition {
        MINT,
        NEAR_MINT,
        EXCELLENT,
        GOOD,
        LIGHT_PLAYED,
        PLAYED,
        POOR;
        
        public String getDisplayName() {
            return switch (this) {
                case MINT -> "Mint (M)";
                case NEAR_MINT -> "Near Mint (NM)";
                case EXCELLENT -> "Excellent (EX)";
                case GOOD -> "Good (GD)";
                case LIGHT_PLAYED -> "Light Played (LP)";
                case PLAYED -> "Played (PL)";
                case POOR -> "Poor (P)";
            };
        }
        
        public String getColor() {
            return switch (this) {
                case MINT, NEAR_MINT -> "green";
                case EXCELLENT, GOOD -> "blue";
                case LIGHT_PLAYED -> "yellow";
                case PLAYED -> "orange";
                case POOR -> "red";
            };
        }
    }
}
