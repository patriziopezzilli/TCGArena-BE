package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.Reservation;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs for Reservation operations
 */
public class ReservationDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateReservationRequest {
        
        @NotBlank(message = "Card ID is required")
        @JsonProperty("card_id")
        private String cardId;
        
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 10, message = "Quantity cannot exceed 10")
        private Integer quantity = 1;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidateReservationRequest {
        
        @NotBlank(message = "QR code is required")
        @JsonProperty("qr_code")
        private String qrCode;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationResponse {
        private Reservation reservation;
        private String message;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationListResponse {
        private java.util.List<Reservation> reservations;
        private int total;
        private int page;
        private int pageSize;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReservationStatsResponse {
        
        @JsonProperty("pending_count")
        private long pendingCount;
        
        @JsonProperty("validated_count")
        private long validatedCount;
        
        @JsonProperty("picked_up_count")
        private long pickedUpCount;
        
        @JsonProperty("expiring_soon_count")
        private long expiringSoonCount;
    }
}
