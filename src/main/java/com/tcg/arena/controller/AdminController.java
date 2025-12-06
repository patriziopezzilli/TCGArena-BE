package com.tcg.arena.controller;

import com.tcg.arena.model.Achievement;
import com.tcg.arena.model.Reward;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.AchievementService;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.RewardService;
import com.tcg.arena.service.ShopService;
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

import java.time.LocalDateTime;
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

    @Autowired
    private RewardService rewardService;

    @Autowired
    private AchievementService achievementService;

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

    /**
     * Update any shop (Super Admin capability)
     */
    @PutMapping("/shops/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update shop (Super Admin)", description = "Allows admin to modify any shop's information")
    public ResponseEntity<?> updateShop(@PathVariable Long id, @RequestBody Shop updatedShop) {
        Optional<Shop> shopOpt = shopService.getShopById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop existingShop = shopOpt.get();

        // Update all fields (admin has full control)
        if (updatedShop.getName() != null)
            existingShop.setName(updatedShop.getName());
        if (updatedShop.getDescription() != null)
            existingShop.setDescription(updatedShop.getDescription());
        if (updatedShop.getAddress() != null)
            existingShop.setAddress(updatedShop.getAddress());
        if (updatedShop.getLatitude() != null)
            existingShop.setLatitude(updatedShop.getLatitude());
        if (updatedShop.getLongitude() != null)
            existingShop.setLongitude(updatedShop.getLongitude());
        if (updatedShop.getPhoneNumber() != null)
            existingShop.setPhoneNumber(updatedShop.getPhoneNumber());
        if (updatedShop.getWebsiteUrl() != null)
            existingShop.setWebsiteUrl(updatedShop.getWebsiteUrl());
        if (updatedShop.getOpeningHours() != null)
            existingShop.setOpeningHours(updatedShop.getOpeningHours());
        if (updatedShop.getOpeningDays() != null)
            existingShop.setOpeningDays(updatedShop.getOpeningDays());
        if (updatedShop.getInstagramUrl() != null)
            existingShop.setInstagramUrl(updatedShop.getInstagramUrl());
        if (updatedShop.getFacebookUrl() != null)
            existingShop.setFacebookUrl(updatedShop.getFacebookUrl());
        if (updatedShop.getTwitterUrl() != null)
            existingShop.setTwitterUrl(updatedShop.getTwitterUrl());
        if (updatedShop.getEmail() != null)
            existingShop.setEmail(updatedShop.getEmail());
        if (updatedShop.getType() != null)
            existingShop.setType(updatedShop.getType());
        if (updatedShop.getActive() != null)
            existingShop.setActive(updatedShop.getActive());
        if (updatedShop.getIsVerified() != null)
            existingShop.setIsVerified(updatedShop.getIsVerified());

        Shop saved = shopService.saveShop(existingShop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop updated successfully");
        response.put("shop", saved);

        return ResponseEntity.ok(response);
    }

    // ========== REWARDS MANAGEMENT ENDPOINTS ==========

    /**
     * Create a new reward
     */
    @PostMapping("/rewards")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create reward", description = "Creates a new reward in the system")
    public ResponseEntity<?> createReward(@RequestBody Reward reward) {
        if (reward.getCreatedAt() == null) {
            reward.setCreatedAt(LocalDateTime.now());
        }
        if (reward.getIsActive() == null) {
            reward.setIsActive(true);
        }
        Reward saved = rewardService.saveReward(reward);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reward created successfully");
        response.put("reward", saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Update existing reward
     */
    @PutMapping("/rewards/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update reward", description = "Updates an existing reward")
    public ResponseEntity<?> updateReward(@PathVariable Long id, @RequestBody Reward updatedReward) {
        Optional<Reward> rewardOpt = rewardService.getRewardById(id);
        if (rewardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Reward not found");
        }

        Reward existingReward = rewardOpt.get();
        if (updatedReward.getName() != null)
            existingReward.setName(updatedReward.getName());
        if (updatedReward.getDescription() != null)
            existingReward.setDescription(updatedReward.getDescription());
        if (updatedReward.getCostPoints() != null)
            existingReward.setCostPoints(updatedReward.getCostPoints());
        if (updatedReward.getImageUrl() != null)
            existingReward.setImageUrl(updatedReward.getImageUrl());
        if (updatedReward.getIsActive() != null)
            existingReward.setIsActive(updatedReward.getIsActive());
        // Update partner - allow setting to null to remove association
        existingReward.setPartner(updatedReward.getPartner());
        if (updatedReward.getType() != null)
            existingReward.setType(updatedReward.getType());

        Reward saved = rewardService.saveReward(existingReward);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reward updated successfully");
        response.put("reward", saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete reward (soft delete)
     */
    @DeleteMapping("/rewards/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Delete reward", description = "Soft deletes a reward by setting isActive to false")
    public ResponseEntity<?> deleteReward(@PathVariable Long id) {
        Optional<Reward> rewardOpt = rewardService.getRewardById(id);
        if (rewardOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Reward not found");
        }

        Reward reward = rewardOpt.get();
        reward.setIsActive(false);
        rewardService.saveReward(reward);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Reward deleted successfully");

        return ResponseEntity.ok(response);
    }

    // ========== ACHIEVEMENTS MANAGEMENT ENDPOINTS ==========

    /**
     * Create a new achievement
     */
    @PostMapping("/achievements")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create achievement", description = "Creates a new achievement in the system")
    public ResponseEntity<?> createAchievement(@RequestBody Achievement achievement) {
        if (achievement.getCreatedAt() == null) {
            achievement.setCreatedAt(LocalDateTime.now());
        }
        if (achievement.getIsActive() == null) {
            achievement.setIsActive(true);
        }
        Achievement saved = achievementService.saveAchievement(achievement);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Achievement created successfully");
        response.put("achievement", saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Update existing achievement
     */
    @PutMapping("/achievements/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update achievement", description = "Updates an existing achievement")
    public ResponseEntity<?> updateAchievement(@PathVariable Long id, @RequestBody Achievement updatedAchievement) {
        Optional<Achievement> achievementOpt = achievementService.getAchievementById(id);
        if (achievementOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Achievement not found");
        }

        Achievement existingAchievement = achievementOpt.get();
        if (updatedAchievement.getName() != null)
            existingAchievement.setName(updatedAchievement.getName());
        if (updatedAchievement.getDescription() != null)
            existingAchievement.setDescription(updatedAchievement.getDescription());
        if (updatedAchievement.getIconUrl() != null)
            existingAchievement.setIconUrl(updatedAchievement.getIconUrl());
        if (updatedAchievement.getCriteria() != null)
            existingAchievement.setCriteria(updatedAchievement.getCriteria());
        if (updatedAchievement.getPointsReward() != null)
            existingAchievement.setPointsReward(updatedAchievement.getPointsReward());
        if (updatedAchievement.getIsActive() != null)
            existingAchievement.setIsActive(updatedAchievement.getIsActive());

        Achievement saved = achievementService.saveAchievement(existingAchievement);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Achievement updated successfully");
        response.put("achievement", saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete achievement (soft delete)
     */
    @DeleteMapping("/achievements/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Delete achievement", description = "Soft deletes an achievement by setting isActive to false")
    public ResponseEntity<?> deleteAchievement(@PathVariable Long id) {
        Optional<Achievement> achievementOpt = achievementService.getAchievementById(id);
        if (achievementOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Achievement not found");
        }

        Achievement achievement = achievementOpt.get();
        achievement.setIsActive(false);
        achievementService.saveAchievement(achievement);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Achievement deleted successfully");

        return ResponseEntity.ok(response);
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
                message = "Batch import triggered successfully for " + tcgType + " from index " + startIndex + " to "
                        + endIndex;
            }
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to trigger batch import: " + e.getMessage());
        }
    }
}