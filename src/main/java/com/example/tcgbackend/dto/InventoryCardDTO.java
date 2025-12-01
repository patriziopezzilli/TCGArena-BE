package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.InventoryCard;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTOs for Inventory Card operations
 */
public class InventoryCardDTO {
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInventoryCardRequest {
        
        @NotBlank(message = "Card template ID is required")
        @JsonProperty("card_template_id")
        private String cardTemplateId;
        
        @NotBlank(message = "Shop ID is required")
        @JsonProperty("shop_id")
        private String shopId;
        
        @NotNull(message = "Condition is required")
        private InventoryCard.CardCondition condition;
        
        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", message = "Price must be positive")
        private Double price;
        
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
        
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        private String notes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateInventoryCardRequest {
        
        private InventoryCard.CardCondition condition;
        
        @DecimalMin(value = "0.0", message = "Price must be positive")
        private Double price;
        
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
        
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        private String notes;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryFilters {
        
        @JsonProperty("tcg_type")
        private String tcgType;
        
        private InventoryCard.CardCondition condition;
        
        @JsonProperty("min_price")
        private Double minPrice;
        
        @JsonProperty("max_price")
        private Double maxPrice;
        
        @JsonProperty("search_query")
        private String searchQuery;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryListResponse {
        private java.util.List<InventoryCard> inventory;
        private int total;
        private int page;
        private int pageSize;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryStatsResponse {
        
        @JsonProperty("total_items")
        private long totalItems;
        
        @JsonProperty("total_quantity")
        private long totalQuantity;
        
        @JsonProperty("total_value")
        private double totalValue;
        
        @JsonProperty("low_stock_count")
        private long lowStockCount;
    }
}
