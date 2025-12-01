package com.tcg.arena.dto;

import com.tcg.arena.model.Reservation;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

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
        private java.util.List<Reservation> reservations;
        private int total;
        private int page;
        private int pageSize;
        
        public ReservationListResponse() {
        }

        // Constructor with arguments
        public ReservationListResponse(java.util.List<Reservation> reservations, int total, int page, int pageSize) {
            this.reservations = reservations;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
        
        public java.util.List<Reservation> getReservations() {
            return reservations;
        }
        
        public void setReservations(java.util.List<Reservation> reservations) {
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
