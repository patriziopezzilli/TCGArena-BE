package com.tcg.arena.dto;

import com.tcg.arena.model.Reservation;
import com.tcg.arena.model.Reservation.ReservationStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTOs for Reservation operations
 */
public class ReservationDTO {
    
    public static class CreateReservationRequest {
        
        @NotBlank(message = "Card ID is required")
        @JsonProperty("card_id")
        private String cardId;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10, message = "Quantity cannot exceed 10")
        private Integer quantity = 1;
        
        public CreateReservationRequest() {
        }
        
        public CreateReservationRequest(String cardId, Integer quantity) {
            this.cardId = cardId;
            this.quantity = quantity;
        }

        // Getters and setters
        public String getCardId() {
            return cardId;
        }

        public void setCardId(String cardId) {
            this.cardId = cardId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CreateReservationRequest that = (CreateReservationRequest) o;
            return Objects.equals(cardId, that.cardId) &&
                   Objects.equals(quantity, that.quantity);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(cardId, quantity);
        }
        
        @Override
        public String toString() {
            return "CreateReservationRequest{" +
                    "cardId='" + cardId + '\'' +
                    ", quantity=" + quantity +
                    '}';
        }
    }
    
    public static class ValidateReservationRequest {
        
        @NotBlank(message = "QR code is required")
        @JsonProperty("qr_code")
        private String qrCode;
        
        public ValidateReservationRequest() {
        }
        
        public ValidateReservationRequest(String qrCode) {
            this.qrCode = qrCode;
        }

        // Getters and setters
        public String getQrCode() {
            return qrCode;
        }

        public void setQrCode(String qrCode) {
            this.qrCode = qrCode;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ValidateReservationRequest that = (ValidateReservationRequest) o;
            return Objects.equals(qrCode, that.qrCode);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(qrCode);
        }
        
        @Override
        public String toString() {
            return "ValidateReservationRequest{" +
                    "qrCode='" + qrCode + '\'' +
                    '}';
        }
    }
    
    public static class ReservationResponse {
        private Reservation reservation;
        private String message;
        
        public ReservationResponse() {
        }

        // Constructor with arguments
        public ReservationResponse(Reservation reservation, String message) {
            this.reservation = reservation;
            this.message = message;
        }
        
        public Reservation getReservation() {
            return reservation;
        }
        
        public void setReservation(Reservation reservation) {
            this.reservation = reservation;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReservationResponse that = (ReservationResponse) o;
            return Objects.equals(reservation, that.reservation) &&
                   Objects.equals(message, that.message);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(reservation, message);
        }
        
        @Override
        public String toString() {
            return "ReservationResponse{" +
                    "reservation=" + reservation +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
    
    public static class ReservationListResponse {
        private java.util.List<ReservationSummaryDTO> reservations;
        private int total;
        private int page;
        private int pageSize;
        
        public ReservationListResponse() {
        }

        // Constructor with arguments
        public ReservationListResponse(java.util.List<ReservationSummaryDTO> reservations, int total, int page, int pageSize) {
            this.reservations = reservations;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
        
        public java.util.List<ReservationSummaryDTO> getReservations() {
            return reservations;
        }
        
        public void setReservations(java.util.List<ReservationSummaryDTO> reservations) {
            this.reservations = reservations;
        }
        
        public int getTotal() {
            return total;
        }
        
        public void setTotal(int total) {
            this.total = total;
        }
        
        public int getPage() {
            return page;
        }
        
        public void setPage(int page) {
            this.page = page;
        }
        
        public int getPageSize() {
            return pageSize;
        }
        
        public void setPageSize(int pageSize) {
            this.pageSize = pageSize;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReservationListResponse that = (ReservationListResponse) o;
            return total == that.total &&
                   page == that.page &&
                   pageSize == that.pageSize &&
                   Objects.equals(reservations, that.reservations);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(reservations, total, page, pageSize);
        }
        
        @Override
        public String toString() {
            return "ReservationListResponse{" +
                    "reservations=" + reservations +
                    ", total=" + total +
                    ", page=" + page +
                    ", pageSize=" + pageSize +
                    '}';
        }
    }
    
    public static class ReservationSummaryDTO {
        private String id;
        
        @JsonProperty("card_id")
        private String cardId;
        
        @JsonProperty("user_id")
        private Long userId;
        
        @JsonProperty("merchant_id")
        private Long merchantId;
        
        private ReservationStatus status;
        
        @JsonProperty("qr_code")
        private String qrCode;
        
        @JsonProperty("expires_at")
        private LocalDateTime expiresAt;
        
        @JsonProperty("created_at")
        private LocalDateTime createdAt;
        
        @JsonProperty("validated_at")
        private LocalDateTime validatedAt;
        
        @JsonProperty("picked_up_at")
        private LocalDateTime pickedUpAt;
        
        @JsonProperty("updated_at")
        private LocalDateTime updatedAt;
        
        // Card information (from InventoryCard)
        @JsonProperty("card_name")
        private String cardName;
        
        @JsonProperty("card_rarity")
        private String cardRarity;
        
        @JsonProperty("card_set")
        private String cardSet;
        
        @JsonProperty("card_image_url")
        private String cardImageUrl;
        
        // Shop information
        @JsonProperty("shop_name")
        private String shopName;
        
        @JsonProperty("shop_location")
        private String shopLocation;
        
        // User information
        @JsonProperty("user_name")
        private String userName;
        
        public ReservationSummaryDTO() {
        }
        
        public ReservationSummaryDTO(Reservation reservation) {
            this.id = reservation.getId();
            this.cardId = reservation.getCardId();
            this.userId = reservation.getUserId();
            this.merchantId = reservation.getMerchantId();
            this.status = reservation.getStatus();
            this.qrCode = reservation.getQrCode();
            this.expiresAt = reservation.getExpiresAt();
            this.createdAt = reservation.getCreatedAt();
            this.validatedAt = reservation.getValidatedAt();
            this.pickedUpAt = reservation.getPickedUpAt();
            this.updatedAt = reservation.getUpdatedAt();
            
            // Populate card information if available
            if (reservation.getCard() != null) {
                this.cardName = reservation.getCard().getCardTemplate() != null ? 
                    reservation.getCard().getCardTemplate().getName() : null;
                this.cardRarity = reservation.getCard().getCardTemplate() != null ? 
                    reservation.getCard().getCardTemplate().getRarity().name() : null;
                this.cardSet = reservation.getCard().getCardTemplate() != null ? 
                    (reservation.getCard().getCardTemplate().getExpansion() != null ? 
                        reservation.getCard().getCardTemplate().getExpansion().getTitle() : 
                        reservation.getCard().getCardTemplate().getSetCode()) : null;
                this.cardImageUrl = reservation.getCard().getCardTemplate() != null ? 
                    reservation.getCard().getCardTemplate().getImageUrl() : null;
            }
            
            // Populate shop information if available
            if (reservation.getShop() != null) {
                this.shopName = reservation.getShop().getName();
                this.shopLocation = reservation.getShop().getAddress();
            }
            
            // Populate user information if available
            if (reservation.getUser() != null) {
                this.userName = reservation.getUser().getUsername();
            }
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
        
        public String getCardName() {
            return cardName;
        }
        
        public void setCardName(String cardName) {
            this.cardName = cardName;
        }
        
        public String getCardRarity() {
            return cardRarity;
        }
        
        public void setCardRarity(String cardRarity) {
            this.cardRarity = cardRarity;
        }
        
        public String getCardSet() {
            return cardSet;
        }
        
        public void setCardSet(String cardSet) {
            this.cardSet = cardSet;
        }
        
        public String getCardImageUrl() {
            return cardImageUrl;
        }
        
        public void setCardImageUrl(String cardImageUrl) {
            this.cardImageUrl = cardImageUrl;
        }
        
        public String getShopName() {
            return shopName;
        }
        
        public void setShopName(String shopName) {
            this.shopName = shopName;
        }
        
        public String getShopLocation() {
            return shopLocation;
        }
        
        public void setShopLocation(String shopLocation) {
            this.shopLocation = shopLocation;
        }
        
        public String getUserName() {
            return userName;
        }
        
        public void setUserName(String userName) {
            this.userName = userName;
        }
    }
    
    public static class ReservationStatsResponse {
        
        @JsonProperty("pending_count")
        private long pendingCount;
        
        @JsonProperty("validated_count")
        private long validatedCount;
        
        @JsonProperty("picked_up_count")
        private long pickedUpCount;
        
        @JsonProperty("expiring_soon_count")
        private long expiringSoonCount;
        
        public ReservationStatsResponse() {
        }

        // Constructor with arguments
        public ReservationStatsResponse(long pendingCount, long validatedCount, long pickedUpCount, long expiringSoonCount) {
            this.pendingCount = pendingCount;
            this.validatedCount = validatedCount;
            this.pickedUpCount = pickedUpCount;
            this.expiringSoonCount = expiringSoonCount;
        }
        
        public long getPendingCount() {
            return pendingCount;
        }
        
        public void setPendingCount(long pendingCount) {
            this.pendingCount = pendingCount;
        }
        
        public long getValidatedCount() {
            return validatedCount;
        }
        
        public void setValidatedCount(long validatedCount) {
            this.validatedCount = validatedCount;
        }
        
        public long getPickedUpCount() {
            return pickedUpCount;
        }
        
        public void setPickedUpCount(long pickedUpCount) {
            this.pickedUpCount = pickedUpCount;
        }
        
        public long getExpiringSoonCount() {
            return expiringSoonCount;
        }
        
        public void setExpiringSoonCount(long expiringSoonCount) {
            this.expiringSoonCount = expiringSoonCount;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReservationStatsResponse that = (ReservationStatsResponse) o;
            return pendingCount == that.pendingCount &&
                   validatedCount == that.validatedCount &&
                   pickedUpCount == that.pickedUpCount &&
                   expiringSoonCount == that.expiringSoonCount;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(pendingCount, validatedCount, pickedUpCount, expiringSoonCount);
        }
        
        @Override
        public String toString() {
            return "ReservationStatsResponse{" +
                    "pendingCount=" + pendingCount +
                    ", validatedCount=" + validatedCount +
                    ", pickedUpCount=" + pickedUpCount +
                    ", expiringSoonCount=" + expiringSoonCount +
                    '}';
        }
    }
}
