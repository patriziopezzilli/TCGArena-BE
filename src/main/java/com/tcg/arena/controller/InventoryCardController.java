package com.tcg.arena.controller;

import com.tcg.arena.dto.InventoryCardDTO.*;
import com.tcg.arena.model.InventoryCard;
import com.tcg.arena.service.InventoryCardService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
public class InventoryCardController {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryCardController.class);
    
    private final InventoryCardService inventoryCardService;
    
    public InventoryCardController(InventoryCardService inventoryCardService) {
        this.inventoryCardService = inventoryCardService;
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
        @RequestParam(defaultValue = "20") int size
    ) {
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
        @Valid @RequestBody CreateInventoryCardRequest request
    ) {
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
        @Valid @RequestBody UpdateInventoryCardRequest request
    ) {
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
        @RequestParam String shopId
    ) {
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
        @RequestParam(defaultValue = "5") int threshold
    ) {
        log.info("GET /api/inventory/low-stock - shopId: {}, threshold: {}", shopId, threshold);
        
        List<InventoryCard> lowStock = inventoryCardService.getLowStockItems(Long.valueOf(shopId), threshold);
        return ResponseEntity.ok(lowStock);
    }
}
