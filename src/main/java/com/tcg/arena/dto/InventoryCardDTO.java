package com.tcg.arena.dto;

import com.tcg.arena.model.InventoryCard;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;

import java.util.Objects;

/**
 * DTOs for Inventory Card operations
 */
public class InventoryCardDTO {
    
    public static class CreateInventoryCardRequest {
        
        @NotNull(message = "Card template ID is required")
        @JsonProperty("card_template_id")
        private Long cardTemplateId;
        
        @NotNull(message = "Shop ID is required")
        @JsonProperty("shop_id")
        private Long shopId;
        
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
        
        public CreateInventoryCardRequest() {
        }
        
        public CreateInventoryCardRequest(Long cardTemplateId, Long shopId, InventoryCard.CardCondition condition,
                                         Double price, Integer quantity, String notes) {
            this.cardTemplateId = cardTemplateId;
            this.shopId = shopId;
            this.condition = condition;
            this.price = price;
            this.quantity = quantity;
            this.notes = notes;
        }

        // Getters and setters
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

        public InventoryCard.CardCondition getCondition() {
            return condition;
        }

        public void setCondition(InventoryCard.CardCondition condition) {
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
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CreateInventoryCardRequest that = (CreateInventoryCardRequest) o;
            return Objects.equals(cardTemplateId, that.cardTemplateId) &&
                   Objects.equals(shopId, that.shopId) &&
                   condition == that.condition &&
                   Objects.equals(price, that.price) &&
                   Objects.equals(quantity, that.quantity) &&
                   Objects.equals(notes, that.notes);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(cardTemplateId, shopId, condition, price, quantity, notes);
        }
        
        @Override
        public String toString() {
            return "CreateInventoryCardRequest{" +
                    "cardTemplateId=" + cardTemplateId +
                    ", shopId=" + shopId +
                    ", condition=" + condition +
                    ", price=" + price +
                    ", quantity=" + quantity +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }
    
    public static class UpdateInventoryCardRequest {
        
        private InventoryCard.CardCondition condition;
        
        @DecimalMin(value = "0.0", message = "Price must be positive")
        private Double price;
        
        @Min(value = 0, message = "Quantity cannot be negative")
        private Integer quantity;
        
        @Size(max = 1000, message = "Notes cannot exceed 1000 characters")
        private String notes;
        
        public UpdateInventoryCardRequest() {
        }
        
        public UpdateInventoryCardRequest(InventoryCard.CardCondition condition, Double price, 
                                         Integer quantity, String notes) {
            this.condition = condition;
            this.price = price;
            this.quantity = quantity;
            this.notes = notes;
        }

        // Getters and setters
        public InventoryCard.CardCondition getCondition() {
            return condition;
        }

        public void setCondition(InventoryCard.CardCondition condition) {
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
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UpdateInventoryCardRequest that = (UpdateInventoryCardRequest) o;
            return condition == that.condition &&
                   Objects.equals(price, that.price) &&
                   Objects.equals(quantity, that.quantity) &&
                   Objects.equals(notes, that.notes);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(condition, price, quantity, notes);
        }
        
        @Override
        public String toString() {
            return "UpdateInventoryCardRequest{" +
                    "condition=" + condition +
                    ", price=" + price +
                    ", quantity=" + quantity +
                    ", notes='" + notes + '\'' +
                    '}';
        }
    }
    
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
        
        public InventoryFilters() {
        }
        
        public InventoryFilters(String tcgType, InventoryCard.CardCondition condition, 
                               Double minPrice, Double maxPrice, String searchQuery) {
            this.tcgType = tcgType;
            this.condition = condition;
            this.minPrice = minPrice;
            this.maxPrice = maxPrice;
            this.searchQuery = searchQuery;
        }

        // Getters and setters
        public String getTcgType() {
            return tcgType;
        }

        public void setTcgType(String tcgType) {
            this.tcgType = tcgType;
        }

        public InventoryCard.CardCondition getCondition() {
            return condition;
        }

        public void setCondition(InventoryCard.CardCondition condition) {
            this.condition = condition;
        }

        public Double getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(Double minPrice) {
            this.minPrice = minPrice;
        }

        public Double getMaxPrice() {
            return maxPrice;
        }

        public void setMaxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
        }

        public String getSearchQuery() {
            return searchQuery;
        }

        public void setSearchQuery(String searchQuery) {
            this.searchQuery = searchQuery;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InventoryFilters that = (InventoryFilters) o;
            return Objects.equals(tcgType, that.tcgType) &&
                   condition == that.condition &&
                   Objects.equals(minPrice, that.minPrice) &&
                   Objects.equals(maxPrice, that.maxPrice) &&
                   Objects.equals(searchQuery, that.searchQuery);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(tcgType, condition, minPrice, maxPrice, searchQuery);
        }
        
        @Override
        public String toString() {
            return "InventoryFilters{" +
                    "tcgType='" + tcgType + '\'' +
                    ", condition=" + condition +
                    ", minPrice=" + minPrice +
                    ", maxPrice=" + maxPrice +
                    ", searchQuery='" + searchQuery + '\'' +
                    '}';
        }
    }
    
    public static class InventoryListResponse {
        private java.util.List<InventoryCard> inventory;
        private int total;
        private int page;
        private int pageSize;
        
        public InventoryListResponse() {
        }

        // Constructor with arguments
        public InventoryListResponse(java.util.List<InventoryCard> inventory, int total, int page, int pageSize) {
            this.inventory = inventory;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
        }
        
        public java.util.List<InventoryCard> getInventory() {
            return inventory;
        }
        
        public void setInventory(java.util.List<InventoryCard> inventory) {
            this.inventory = inventory;
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
            InventoryListResponse that = (InventoryListResponse) o;
            return total == that.total &&
                   page == that.page &&
                   pageSize == that.pageSize &&
                   Objects.equals(inventory, that.inventory);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(inventory, total, page, pageSize);
        }
        
        @Override
        public String toString() {
            return "InventoryListResponse{" +
                    "inventory=" + inventory +
                    ", total=" + total +
                    ", page=" + page +
                    ", pageSize=" + pageSize +
                    '}';
        }
    }
    
    public static class InventoryStatsResponse {
        
        @JsonProperty("total_items")
        private long totalItems;
        
        @JsonProperty("total_quantity")
        private long totalQuantity;
        
        @JsonProperty("total_value")
        private double totalValue;
        
        @JsonProperty("low_stock_count")
        private long lowStockCount;
        
        public InventoryStatsResponse() {
        }

        // Constructor with arguments
        public InventoryStatsResponse(long totalItems, long totalQuantity, double totalValue, long lowStockCount) {
            this.totalItems = totalItems;
            this.totalQuantity = totalQuantity;
            this.totalValue = totalValue;
            this.lowStockCount = lowStockCount;
        }
        
        public long getTotalItems() {
            return totalItems;
        }
        
        public void setTotalItems(long totalItems) {
            this.totalItems = totalItems;
        }
        
        public long getTotalQuantity() {
            return totalQuantity;
        }
        
        public void setTotalQuantity(long totalQuantity) {
            this.totalQuantity = totalQuantity;
        }
        
        public double getTotalValue() {
            return totalValue;
        }
        
        public void setTotalValue(double totalValue) {
            this.totalValue = totalValue;
        }
        
        public long getLowStockCount() {
            return lowStockCount;
        }
        
        public void setLowStockCount(long lowStockCount) {
            this.lowStockCount = lowStockCount;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InventoryStatsResponse that = (InventoryStatsResponse) o;
            return totalItems == that.totalItems &&
                   totalQuantity == that.totalQuantity &&
                   Double.compare(that.totalValue, totalValue) == 0 &&
                   lowStockCount == that.lowStockCount;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(totalItems, totalQuantity, totalValue, lowStockCount);
        }
        
        @Override
        public String toString() {
            return "InventoryStatsResponse{" +
                    "totalItems=" + totalItems +
                    ", totalQuantity=" + totalQuantity +
                    ", totalValue=" + totalValue +
                    ", lowStockCount=" + lowStockCount +
                    '}';
        }
    }
}
