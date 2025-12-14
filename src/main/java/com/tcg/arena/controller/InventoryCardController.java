package com.tcg.arena.controller;

import com.tcg.arena.dto.InventoryCardDTO.*;
import com.tcg.arena.dto.InventoryBulkImportDTO.*;
import com.tcg.arena.model.InventoryCard;
import com.tcg.arena.service.InventoryCardService;
import com.tcg.arena.service.InventoryBulkImportService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryCardController {

    private static final Logger log = LoggerFactory.getLogger(InventoryCardController.class);

    private final InventoryCardService inventoryCardService;
    private final InventoryBulkImportService bulkImportService;

    public InventoryCardController(InventoryCardService inventoryCardService,
            InventoryBulkImportService bulkImportService) {
        this.inventoryCardService = inventoryCardService;
        this.bulkImportService = bulkImportService;
    }

    /**
     * Get inventory for a shop
     * GET /api/inventory?shopId={shopId}&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<InventoryListResponse> getInventory(
            @RequestParam String shopId,
            @RequestParam(required = false) String tcgType,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String searchQuery,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/inventory - shopId: {}", shopId);

        InventoryFilters filters = new InventoryFilters();
        filters.setTcgType(tcgType);
        filters.setCondition(condition != null ? InventoryCard.CardCondition.valueOf(condition) : null);
        filters.setMinPrice(minPrice);
        filters.setMaxPrice(maxPrice);
        filters.setSearchQuery(searchQuery);

        InventoryListResponse response = inventoryCardService.getInventory(Long.valueOf(shopId), filters, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Download CSV template for bulk import
     * GET /api/inventory/template
     */
    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        log.info("GET /api/inventory/template - Downloading bulk import template");

        byte[] template = bulkImportService.generateCSVTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", "inventory_import_template.csv");
        headers.setContentLength(template.length);

        return new ResponseEntity<>(template, headers, HttpStatus.OK);
    }

    /**
     * Bulk import from standard CSV template
     * POST /api/inventory/bulk-import?shopId={shopId}
     */
    @PostMapping("/bulk-import")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<BulkImportResponse> bulkImport(
            @RequestParam String shopId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("POST /api/inventory/bulk-import - shopId: {}, file: {}", shopId, file.getOriginalFilename());

        BulkImportResponse response = bulkImportService.bulkImportFromCSV(Long.valueOf(shopId), file);
        return ResponseEntity.ok(response);
    }

    /**
     * Submit custom file for AI processing
     * POST /api/inventory/import-request?shopId={shopId}
     */
    @PostMapping("/import-request")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<ImportRequestResponse> submitCustomImport(
            @RequestParam String shopId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes) throws IOException {
        log.info("POST /api/inventory/import-request - shopId: {}, file: {}", shopId, file.getOriginalFilename());

        ImportRequestResponse response = bulkImportService.saveCustomImportRequest(
                Long.valueOf(shopId), file, notes);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get single inventory card
     * GET /api/inventory/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<InventoryCard> getInventoryCard(@PathVariable String id) {
        log.info("GET /api/inventory/{}", id);

        InventoryCard inventoryCard = inventoryCardService.getInventoryCard(id);
        return ResponseEntity.ok(inventoryCard);
    }

    /**
     * Create inventory card
     * POST /api/inventory
     */
    @PostMapping
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<InventoryCard> createInventoryCard(
            @Valid @RequestBody CreateInventoryCardRequest request) {
        log.info("POST /api/inventory - Creating card for shop: {}", request.getShopId());

        InventoryCard created = inventoryCardService.createInventoryCard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update inventory card
     * PUT /api/inventory/{id}
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<InventoryCard> updateInventoryCard(
            @PathVariable String id,
            @Valid @RequestBody UpdateInventoryCardRequest request) {
        log.info("PUT /api/inventory/{}", id);

        InventoryCard updated = inventoryCardService.updateInventoryCard(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete inventory card
     * DELETE /api/inventory/{id}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<Void> deleteInventoryCard(@PathVariable String id) {
        log.info("DELETE /api/inventory/{}", id);

        inventoryCardService.deleteInventoryCard(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get inventory statistics
     * GET /api/inventory/stats?shopId={shopId}
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<InventoryStatsResponse> getInventoryStats(
            @RequestParam String shopId) {
        log.info("GET /api/inventory/stats - shopId: {}", shopId);

        InventoryStatsResponse stats = inventoryCardService.getInventoryStats(Long.valueOf(shopId));
        return ResponseEntity.ok(stats);
    }

    /**
     * Get low stock items
     * GET /api/inventory/low-stock?shopId={shopId}&threshold=5
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<List<InventoryCard>> getLowStockItems(
            @RequestParam String shopId,
            @RequestParam(defaultValue = "5") int threshold) {
        log.info("GET /api/inventory/low-stock - shopId: {}, threshold: {}", shopId, threshold);

        List<InventoryCard> lowStock = inventoryCardService.getLowStockItems(Long.valueOf(shopId), threshold);
        return ResponseEntity.ok(lowStock);
    }

    /**
     * Bulk add all cards from a specific set
     * POST /api/inventory/bulk-add-by-set
     */
    @PostMapping("/bulk-add-by-set")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<BulkImportResponse> bulkAddBySet(
            @Valid @RequestBody BulkAddBySetRequest request) {
        log.info("POST /api/inventory/bulk-add-by-set - shopId: {}, setCode: {}",
                request.getShopId(), request.getSetCode());

        BulkImportResponse response = bulkImportService.bulkAddBySetCode(
                request.getShopId(),
                request.getSetCode(),
                request.getCondition(),
                request.getQuantity(),
                request.getPrice(),
                request.getNationality());
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk add all cards from a specific expansion
     * POST /api/inventory/bulk-add-by-expansion
     */
    @PostMapping("/bulk-add-by-expansion")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<BulkImportResponse> bulkAddByExpansion(
            @Valid @RequestBody BulkAddByExpansionRequest request) {
        log.info("POST /api/inventory/bulk-add-by-expansion - shopId: {}, expansionId: {}",
                request.getShopId(), request.getExpansionId());

        BulkImportResponse response = bulkImportService.bulkAddByExpansionId(
                request.getShopId(),
                request.getExpansionId(),
                request.getCondition(),
                request.getQuantity(),
                request.getPrice(),
                request.getNationality());
        return ResponseEntity.ok(response);
    }

    /**
     * Bulk add specific card templates by IDs
     * POST /api/inventory/bulk-add-by-templates
     */
    @PostMapping("/bulk-add-by-templates")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<BulkImportResponse> bulkAddByTemplates(
            @Valid @RequestBody BulkAddByTemplateIdsRequest request) {
        log.info("POST /api/inventory/bulk-add-by-templates - shopId: {}, count: {}",
                request.getShopId(), request.getTemplateIds().size());

        BulkImportResponse response = bulkImportService.bulkAddByTemplateIds(
                request.getShopId(),
                request.getTemplateIds(),
                request.getCondition(),
                request.getQuantity(),
                request.getPrice(),
                request.getNationality());
        return ResponseEntity.ok(response);
    }
}
