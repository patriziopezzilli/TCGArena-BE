package com.tcg.arena.dto;

import com.tcg.arena.model.InventoryImportRequest.ImportStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for inventory bulk import operations
 */
public class InventoryBulkImportDTO {

    /**
     * Response for bulk import from standard template
     */
    public static class BulkImportResponse {
        private int totalRows;
        private int successCount;
        private int errorCount;
        private List<String> errors;
        private String message;

        public BulkImportResponse() {
        }

        public BulkImportResponse(int totalRows, int successCount, int errorCount, List<String> errors,
                String message) {
            this.totalRows = totalRows;
            this.successCount = successCount;
            this.errorCount = errorCount;
            this.errors = errors;
            this.message = message;
        }

        // Getters and Setters
        public int getTotalRows() {
            return totalRows;
        }

        public void setTotalRows(int totalRows) {
            this.totalRows = totalRows;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(int successCount) {
            this.successCount = successCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void setErrorCount(int errorCount) {
            this.errorCount = errorCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public void setErrors(List<String> errors) {
            this.errors = errors;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Response for custom import request submission
     */
    public static class ImportRequestResponse {
        private Long requestId;
        private String message;
        private String estimatedProcessingTime;

        public ImportRequestResponse() {
        }

        public ImportRequestResponse(Long requestId, String message, String estimatedProcessingTime) {
            this.requestId = requestId;
            this.message = message;
            this.estimatedProcessingTime = estimatedProcessingTime;
        }

        // Getters and Setters
        public Long getRequestId() {
            return requestId;
        }

        public void setRequestId(Long requestId) {
            this.requestId = requestId;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getEstimatedProcessingTime() {
            return estimatedProcessingTime;
        }

        public void setEstimatedProcessingTime(String estimatedProcessingTime) {
            this.estimatedProcessingTime = estimatedProcessingTime;
        }
    }

    /**
     * DTO for viewing import request status
     */
    public static class ImportRequestStatusDTO {
        private Long id;
        private Long shopId;
        private String fileName;
        private ImportStatus status;
        private String notes;
        private Integer processedCount;
        private String errorMessage;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;

        // Getters and Setters
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public ImportStatus getStatus() {
            return status;
        }

        public void setStatus(ImportStatus status) {
            this.status = status;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        public Integer getProcessedCount() {
            return processedCount;
        }

        public void setProcessedCount(Integer processedCount) {
            this.processedCount = processedCount;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
        }

        public LocalDateTime getProcessedAt() {
            return processedAt;
        }

        public void setProcessedAt(LocalDateTime processedAt) {
            this.processedAt = processedAt;
        }
    }

    /**
     * Row from CSV template for bulk import - uses human-readable identifiers
     */
    public static class BulkImportRow {
        @NotNull
        private String cardName; // e.g. "Charizard" or "char" (LIKE search)
        @NotNull
        private String setCode; // e.g. "SV01" (exact match)
        @NotNull
        private String tcgType; // e.g. "POKEMON" (exact match)
        @NotNull
        private String condition;
        @NotNull
        private Integer quantity;
        @NotNull
        private Double price;
        private String nationality;
        private String notes;

        // Resolved card template ID (set during processing)
        private Long cardTemplateId;

        // Getters and Setters
        public String getCardName() {
            return cardName;
        }

        public void setCardName(String cardName) {
            this.cardName = cardName;
        }

        public String getSetCode() {
            return setCode;
        }

        public void setSetCode(String setCode) {
            this.setCode = setCode;
        }

        public String getTcgType() {
            return tcgType;
        }

        public void setTcgType(String tcgType) {
            this.tcgType = tcgType;
        }

        public Long getCardTemplateId() {
            return cardTemplateId;
        }

        public void setCardTemplateId(Long cardTemplateId) {
            this.cardTemplateId = cardTemplateId;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }

    /**
     * Request for bulk adding all cards from a set
     */
    public static class BulkAddBySetRequest {
        @NotNull
        private Long shopId;
        @NotNull
        private String setCode;
        @NotNull
        private String condition;
        @NotNull
        private Integer quantity;
        @NotNull
        private Double price;
        private String nationality;

        // Getters and Setters
        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public String getSetCode() {
            return setCode;
        }

        public void setSetCode(String setCode) {
            this.setCode = setCode;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }
    }

    /**
     * Request for bulk adding all cards from an expansion
     */
    public static class BulkAddByExpansionRequest {
        @NotNull
        private Long shopId;
        @NotNull
        private Long expansionId;
        @NotNull
        private String condition;
        @NotNull
        private Integer quantity;
        @NotNull
        private Double price;
        private String nationality;

        // Getters and Setters
        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public Long getExpansionId() {
            return expansionId;
        }

        public void setExpansionId(Long expansionId) {
            this.expansionId = expansionId;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }
    }

    /**
     * Request for bulk adding specific card templates by IDs
     */
    public static class BulkAddByTemplateIdsRequest {
        @NotNull
        private Long shopId;
        @NotNull
        private List<Long> templateIds;
        @NotNull
        private String condition;
        @NotNull
        private Integer quantity;
        @NotNull
        private Double price;
        private String nationality;

        // Getters and Setters
        public Long getShopId() {
            return shopId;
        }

        public void setShopId(Long shopId) {
            this.shopId = shopId;
        }

        public List<Long> getTemplateIds() {
            return templateIds;
        }

        public void setTemplateIds(List<Long> templateIds) {
            this.templateIds = templateIds;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public String getNationality() {
            return nationality;
        }

        public void setNationality(String nationality) {
            this.nationality = nationality;
        }
    }
}
