package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity to store custom inventory import requests for AI processing.
 * Stores uploaded files that need manual/AI processing before being imported.
 */
@Entity
@Table(name = "inventory_import_requests", indexes = {
        @Index(name = "idx_import_request_shop_id", columnList = "shop_id"),
        @Index(name = "idx_import_request_status", columnList = "status")
})
public class InventoryImportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "shop_id", nullable = false)
    @JsonProperty("shop_id")
    private Long shopId;

    @Column(name = "file_name", nullable = false)
    @JsonProperty("file_name")
    private String fileName;

    @Column(name = "file_content_type")
    @JsonProperty("file_content_type")
    private String fileContentType;

    @Lob
    @Column(name = "file_content", nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private byte[] fileContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status = ImportStatus.PENDING;

    @Column(length = 1000)
    private String notes;

    @Column(name = "processed_count")
    @JsonProperty("processed_count")
    private Integer processedCount;

    @Column(name = "error_message", length = 2000)
    @JsonProperty("error_message")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    @JsonProperty("processed_at")
    private LocalDateTime processedAt;

    // Relationship to shop
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id", insertable = false, updatable = false)
    private Shop shop;

    public enum ImportStatus {
        PENDING, // Waiting to be processed
        PROCESSING, // Currently being processed
        COMPLETED, // Successfully processed
        PARTIALLY_COMPLETED, // Some items imported, some failed
        FAILED // Processing failed
    }

    // Constructors
    public InventoryImportRequest() {
    }

    public InventoryImportRequest(Long shopId, String fileName, String fileContentType, byte[] fileContent,
            String notes) {
        this.shopId = shopId;
        this.fileName = fileName;
        this.fileContentType = fileContentType;
        this.fileContent = fileContent;
        this.notes = notes;
        this.status = ImportStatus.PENDING;
    }

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

    public String getFileContentType() {
        return fileContentType;
    }

    public void setFileContentType(String fileContentType) {
        this.fileContentType = fileContentType;
    }

    public byte[] getFileContent() {
        return fileContent;
    }

    public void setFileContent(byte[] fileContent) {
        this.fileContent = fileContent;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Shop getShop() {
        return shop;
    }

    public void setShop(Shop shop) {
        this.shop = shop;
    }
}
