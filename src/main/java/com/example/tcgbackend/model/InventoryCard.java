package com.example.tcgbackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

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
public class InventoryCard {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "card_template_id", nullable = false)
    @JsonProperty("card_template_id")
    private Long cardTemplateId;
    
    @Column(name = "shop_id", nullable = false)
    @JsonProperty("shop_id")
    private Long shopId;
    
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
    
    // Constructors
    public InventoryCard() {
    }
    
    public InventoryCard(String id, Long cardTemplateId, Long shopId, CardCondition condition, 
                        Double price, Integer quantity, String notes, LocalDateTime createdAt, 
                        LocalDateTime updatedAt) {
        this.id = id;
        this.cardTemplateId = cardTemplateId;
        this.shopId = shopId;
        this.condition = condition;
        this.price = price;
        this.quantity = quantity;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getCardTemplateId() {
        return cardTemplateId;
    }

    public void setCardTemplateId(Long cardTemplateId) {
        this.cardTemplateId = cardTemplateId;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
    
    public Card getCardTemplate() {
        return cardTemplate;
    }
    
    public void setCardTemplate(Card cardTemplate) {
        this.cardTemplate = cardTemplate;
    }
    
    public Shop getShop() {
        return shop;
    }
    
    public void setShop(Shop shop) {
        this.shop = shop;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InventoryCard that = (InventoryCard) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "InventoryCard{" +
                "id='" + id + '\'' +
                ", cardTemplateId=" + cardTemplateId +
                ", shopId=" + shopId +
                ", condition=" + condition +
                ", price=" + price +
                ", quantity=" + quantity +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
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
