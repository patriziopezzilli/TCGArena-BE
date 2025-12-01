package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Shop;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.BatchService;
import com.example.tcgbackend.service.ShopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin", description = "Administrative API for managing batch operations and system maintenance")
public class AdminController {

    @Autowired
    private BatchService batchService;

    @Autowired
    private ShopService shopService;

    // ========== SHOP MANAGEMENT ENDPOINTS ==========

    /**
     * Get all shops including inactive (for admin panel)
     */
    @GetMapping("/shops")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get all shops", description = "Returns all shops including inactive ones for admin management")
    public ResponseEntity<?> getAllShops() {
        List<Shop> shops = shopService.getAllShopsIncludingInactive();
        return ResponseEntity.ok(shops);
    }

    /**
     * Get only pending shops (active = false)
     */
    @GetMapping("/shops/pending")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get pending shops", description = "Returns shops awaiting activation")
    public ResponseEntity<?> getPendingShops() {
        List<Shop> allShops = shopService.getAllShopsIncludingInactive();
        List<Shop> pendingShops = allShops.stream()
                .filter(shop -> !shop.getActive())
                .toList();
        return ResponseEntity.ok(pendingShops);
    }

    /**
     * Activate a shop
     */
    @PostMapping("/shops/{id}/activate")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Activate shop", description = "Activates a pending shop making it visible in the app")
    public ResponseEntity<?> activateShop(@PathVariable Long id) {
        Optional<Shop> shopOpt = shopService.getShopById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();
        shop.setActive(true);
        shop.setIsVerified(true);
        Shop updatedShop = shopService.saveShop(shop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop activated successfully");
        response.put("shop", updatedShop);

        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate a shop
     */
    @PostMapping("/shops/{id}/deactivate")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Deactivate shop", description = "Deactivates a shop removing it from public visibility")
    public ResponseEntity<?> deactivateShop(@PathVariable Long id) {
        Optional<Shop> shopOpt = shopService.getShopById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();
        shop.setActive(false);
        Shop updatedShop = shopService.saveShop(shop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop deactivated successfully");
        response.put("shop", updatedShop);

        return ResponseEntity.ok(response);
    }

    /**
     * Get shop statistics
     */
    @GetMapping("/shops/stats")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get shop statistics", description = "Returns statistics about shops (total, active, pending, verified)")
    public ResponseEntity<?> getShopStats() {
        List<Shop> allShops = shopService.getAllShopsIncludingInactive();
        
        long total = allShops.size();
        long active = allShops.stream().filter(Shop::getActive).count();
        long pending = allShops.stream().filter(shop -> !shop.getActive()).count();
        long verified = allShops.stream().filter(Shop::getIsVerified).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("active", active);
        stats.put("pending", pending);
        stats.put("verified", verified);

        return ResponseEntity.ok(stats);
    }

    // ========== BATCH IMPORT ENDPOINTS ==========

    @PostMapping("/import/{tcgType}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Trigger batch import for specific TCG type", description = "Starts a batch job to import cards for the specified TCG type")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Batch import triggered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid TCG type provided")
    })
    public ResponseEntity<String> triggerBatchImport(
            @Parameter(description = "TCG type to import (POKEMON, MAGIC, YUGIOH, etc.)") @PathVariable TCGType tcgType,
            @Parameter(description = "Starting index for import (-99 to import all)") @RequestParam(defaultValue = "-99") int startIndex,
            @Parameter(description = "Ending index for import (-99 to import until end)") @RequestParam(defaultValue = "-99") int endIndex) {
        try {
            batchService.triggerBatchImport(tcgType, startIndex, endIndex);
            String message;
            if (startIndex == -99 && endIndex == -99) {
                message = "Batch import triggered successfully for " + tcgType;
            } else if (endIndex == -99) {
                message = "Batch import triggered successfully for " + tcgType + " starting from index " + startIndex;
            } else {
                message = "Batch import triggered successfully for " + tcgType + " from index " + startIndex + " to " + endIndex;
            }
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to trigger batch import: " + e.getMessage());
        }
    }
}