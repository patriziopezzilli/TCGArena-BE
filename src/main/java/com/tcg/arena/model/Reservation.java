package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a card reservation with QR code validation
 */
@Entity
@Table(name = "reservations", indexes = {
    @Index(name = "idx_card_id", columnList = "card_id"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_merchant_id", columnList = "merchant_id"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_qr_code", columnList = "qr_code", unique = true)
})
public class Reservation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "card_id", nullable = false)
    @JsonProperty("card_id")
    private String cardId;
    
    @Column(name = "user_id", nullable = false)
    @JsonProperty("user_id")
    private Long userId;
    
    @Column(name = "merchant_id", nullable = false)
    @JsonProperty("merchant_id")
    private Long merchantId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;
    
    @Column(name = "qr_code", nullable = false, unique = true)
    @JsonProperty("qr_code")
    private String qrCode;
    
    @Column(name = "expires_at", nullable = false)
    @JsonProperty("expires_at")
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "validated_at")
    @JsonProperty("validated_at")
    private LocalDateTime validatedAt;
    
    @Column(name = "picked_up_at")
    @JsonProperty("picked_up_at")
    private LocalDateTime pickedUpAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", insertable = false, updatable = false)
    private InventoryCard card;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false)
    private Shop shop;
    
    // Constructors
    public Reservation() {
    }
    
    public Reservation(String id, String cardId, Long userId, Long merchantId, 
                      ReservationStatus status, String qrCode, LocalDateTime expiresAt, 
                      LocalDateTime createdAt, LocalDateTime validatedAt, 
                      LocalDateTime pickedUpAt, LocalDateTime updatedAt) {
        this.id = id;
        this.cardId = cardId;
        this.userId = userId;
        this.merchantId = merchantId;
        this.status = status;
        this.qrCode = qrCode;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.validatedAt = validatedAt;
        this.pickedUpAt = pickedUpAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCardId() {
        return cardId;
    }

    public void setCardId(String cardId) {
        this.cardId = cardId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(Long merchantId) {
        this.merchantId = merchantId;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(LocalDateTime validatedAt) {
        this.validatedAt = validatedAt;
    }

    public LocalDateTime getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(LocalDateTime pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public InventoryCard getCard() {
        return card;
    }
    
    public void setCard(InventoryCard card) {
        this.card = card;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
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
        Reservation that = (Reservation) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "Reservation{" +
                "id='" + id + '\'' +
                ", cardId='" + cardId + '\'' +
                ", userId=" + userId +
                ", merchantId=" + merchantId +
                ", status=" + status +
                ", qrCode='" + qrCode + '\'' +
                ", expiresAt=" + expiresAt +
                ", createdAt=" + createdAt +
                ", validatedAt=" + validatedAt +
                ", pickedUpAt=" + pickedUpAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
    
    public enum ReservationStatus {
        PENDING,
        VALIDATED,
        PICKED_UP,
        EXPIRED,
        CANCELLED;
        
        public String getDisplayName() {
            return switch (this) {
                case PENDING -> "Pending";
                case VALIDATED -> "Validated";
                case PICKED_UP -> "Picked Up";
                case EXPIRED -> "Expired";
                case CANCELLED -> "Cancelled";
            };
        }
        
        public String getColor() {
            return switch (this) {
                case PENDING -> "orange";
                case VALIDATED -> "blue";
                case PICKED_UP -> "green";
                case EXPIRED -> "gray";
                case CANCELLED -> "red";
            };
        }
        
        public String getIcon() {
            return switch (this) {
                case PENDING -> "clock.fill";
                case VALIDATED -> "checkmark.circle.fill";
                case PICKED_UP -> "bag.fill";
                case EXPIRED -> "xmark.circle.fill";
                case CANCELLED -> "trash.fill";
            };
        }
    }
    
    @PrePersist
    public void generateQRCode() {
        if (this.qrCode == null) {
            this.qrCode = UUID.randomUUID().toString();
        }
    }
    
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) && status == ReservationStatus.PENDING;
    }
    
    public boolean canBeCancelled() {
        return status == ReservationStatus.PENDING || status == ReservationStatus.VALIDATED;
    }
    
    public boolean canBeValidated() {
        return status == ReservationStatus.PENDING && !isExpired();
    }
    
    public boolean canBePickedUp() {
        return status == ReservationStatus.VALIDATED && !isExpired();
    }
}
