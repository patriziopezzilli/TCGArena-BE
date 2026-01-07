package com.tcg.arena.controller;

import com.tcg.arena.dto.ShopSuggestionDTO;
import com.tcg.arena.model.Achievement;
import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.Reward;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopSuggestion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.ShopSuggestionRepository;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.service.AchievementService;
import com.tcg.arena.service.BatchService;
import com.tcg.arena.service.BroadcastNewsService;
import com.tcg.arena.service.NotificationService;
import com.tcg.arena.service.RewardService;
import com.tcg.arena.service.ShopService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private BroadcastNewsService broadcastNewsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ShopSuggestionRepository shopSuggestionRepository;

    @Autowired
    private com.tcg.arena.service.EmailService emailService;

    @Autowired
    private com.tcg.arena.repository.UserEmailPreferencesRepository emailPreferencesRepository;

    @Autowired
    private com.tcg.arena.repository.CardTemplateRepository cardTemplateRepository;

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

        // Send shop approved email
        if (shop.getOwnerId() != null) {
            try {
                User owner = userService.getUserById(shop.getOwnerId()).orElse(null);
                if (owner != null && shouldSendShopNotification(owner)) {
                    emailService.sendShopApproved(
                            owner.getEmail(),
                            owner.getUsername(),
                            shop.getName(),
                            shop.getId());
                }
            } catch (Exception e) {
                // Log but don't fail activation
            }
        }

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
     * Reject a shop with reason
     */
    @PostMapping("/shops/{id}/reject")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Reject shop", description = "Rejects a shop application with a reason")
    public ResponseEntity<?> rejectShop(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Optional<Shop> shopOpt = shopService.getShopById(id);
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();
        String rejectionReason = body.getOrDefault("reason", "La richiesta non soddisfa i criteri richiesti");

        // Send rejection email
        if (shop.getOwnerId() != null) {
            try {
                User owner = userService.getUserById(shop.getOwnerId()).orElse(null);
                if (owner != null && shouldSendShopNotification(owner)) {
                    emailService.sendShopRejected(
                            owner.getEmail(),
                            owner.getUsername(),
                            shop.getName(),
                            rejectionReason);
                }
            } catch (Exception e) {
                // Log but don't fail
            }
        }

        // Delete the shop
        shopService.deleteShop(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop rejected and deleted");

        return ResponseEntity.ok(response);
    }

    /**
     * Check if user wants shop notifications
     */
    private boolean shouldSendShopNotification(User user) {
        return emailPreferencesRepository.findByUser(user)
                .map(prefs -> prefs.getShopNotifications())
                .orElse(true);
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
            batchService.triggerJustTCGImport(tcgType);
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

    @GetMapping("/card-templates/counts")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get card template counts by TCG type", description = "Returns the count of card templates for each TCG type")
    @ApiResponse(responseCode = "200", description = "Card template counts retrieved successfully")
    public ResponseEntity<Map<String, Long>> getCardTemplateCounts() {
        Map<String, Long> counts = new HashMap<>();
        for (TCGType tcgType : TCGType.values()) {
            long count = cardTemplateRepository.countByTcgType(tcgType.name());
            counts.put(tcgType.name(), count);
        }
        return ResponseEntity.ok(counts);
    }

    // ========== BROADCAST NOTIFICATIONS ENDPOINTS ==========

    /**
     * DTO for broadcast notification request
     */
    public static class BroadcastNotificationRequest {
        private String title;
        private String message;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Get count of users who will receive broadcast notifications
     */
    @GetMapping("/broadcast/recipients-count")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get broadcast recipients count", description = "Returns the number of users with registered device tokens who will receive broadcast notifications")
    public ResponseEntity<?> getBroadcastRecipientsCount() {
        long count = notificationService.getBroadcastRecipientsCount();

        Map<String, Object> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Send broadcast notification to all users
     */
    @PostMapping("/broadcast/send")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Send broadcast notification", description = "Sends a push notification to all users with registered device tokens")
    public ResponseEntity<?> sendBroadcastNotification(@RequestBody BroadcastNotificationRequest request) {
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Title is required");
        }
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }

        int usersNotified = notificationService.sendBroadcastNotification(
                request.getTitle().trim(),
                request.getMessage().trim());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("usersNotified", usersNotified);
        response.put("message", "Broadcast notification sent to " + usersNotified + " users");

        return ResponseEntity.ok(response);
    }

    // ========== BROADCAST NEWS ENDPOINTS ==========

    /**
     * Get all broadcast news
     */
    @GetMapping("/broadcast-news")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get all broadcast news", description = "Returns all broadcast news for admin management")
    public ResponseEntity<?> getAllBroadcastNews(Authentication authentication) {
        List<BroadcastNews> news = broadcastNewsService.getAllNews();
        List<BroadcastNewsDTO> dtos = news.stream()
                .map(BroadcastNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get active broadcast news
     */
    @GetMapping("/broadcast-news/active")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get active broadcast news", description = "Returns currently active broadcast news")
    public ResponseEntity<?> getActiveBroadcastNews(Authentication authentication) {
        List<BroadcastNews> news = broadcastNewsService.getActiveNews();
        List<BroadcastNewsDTO> dtos = news.stream()
                .map(BroadcastNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get future broadcast news
     */
    @GetMapping("/broadcast-news/future")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get future broadcast news", description = "Returns broadcast news scheduled for future")
    public ResponseEntity<?> getFutureBroadcastNews(Authentication authentication) {
        List<BroadcastNews> news = broadcastNewsService.getFutureNews();
        List<BroadcastNewsDTO> dtos = news.stream()
                .map(BroadcastNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get expired broadcast news
     */
    @GetMapping("/broadcast-news/expired")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get expired broadcast news", description = "Returns expired broadcast news")
    public ResponseEntity<?> getExpiredBroadcastNews(Authentication authentication) {
        List<BroadcastNews> news = broadcastNewsService.getExpiredNews();
        List<BroadcastNewsDTO> dtos = news.stream()
                .map(BroadcastNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create new broadcast news
     */
    @PostMapping("/broadcast-news")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Create broadcast news", description = "Creates a new broadcast news item that will be visible to all users. Optionally sends a push notification.")
    public ResponseEntity<?> createBroadcastNews(@RequestBody BroadcastNewsDTO request,
            @RequestParam(required = false, defaultValue = "false") boolean sendPushNotification,
            Authentication authentication) {
        try {
            System.out.println("📰 DEBUG - Creating broadcast news:");
            System.out.println("  Title: " + request.getTitle());
            System.out.println("  Content: " + request.getContent());
            System.out.println("  NewsType: " + request.getNewsType());
            System.out.println("  StartDate: " + request.getStartDate());
            System.out.println("  ExpiryDate: " + request.getExpiryDate());
            System.out.println("  IsPinned: " + request.getIsPinned());

            // Get user ID if authenticated, otherwise use null (system will use default
            // admin ID)
            Long createdBy = null;
            if (authentication != null) {
                User user = getCurrentUser(authentication);
                createdBy = user.getId();
            }

            BroadcastNews news = broadcastNewsService.createNews(
                    request.getTitle(),
                    request.getContent(),
                    request.getNewsType(),
                    request.getStartDate(),
                    request.getExpiryDate(),
                    request.getImageUrl(),
                    request.getIsPinned(),
                    createdBy);

            System.out.println("✅ DEBUG - Broadcast news created with ID: " + news.getId());
            System.out.println("  Saved StartDate: " + news.getStartDate());
            System.out.println("  Saved ExpiryDate: " + news.getExpiryDate());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Broadcast news created successfully");
            response.put("news", new BroadcastNewsDTO(news));

            // Opzionalmente manda anche una push notification
            if (sendPushNotification) {
                int usersNotified = notificationService.sendBroadcastNotification(
                        news.getTitle(),
                        news.getContent());
                response.put("pushNotificationSent", true);
                response.put("usersNotified", usersNotified);
            } else {
                response.put("pushNotificationSent", false);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Update broadcast news
     */
    @PutMapping("/broadcast-news/{newsId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update broadcast news", description = "Updates an existing broadcast news item")
    public ResponseEntity<?> updateBroadcastNews(@PathVariable Long newsId,
            @RequestBody BroadcastNewsDTO request,
            Authentication authentication) {
        try {
            BroadcastNews news = broadcastNewsService.updateNews(
                    newsId,
                    request.getTitle(),
                    request.getContent(),
                    request.getNewsType(),
                    request.getStartDate(),
                    request.getExpiryDate(),
                    request.getImageUrl(),
                    request.getIsPinned());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Broadcast news updated successfully");
            response.put("news", new BroadcastNewsDTO(news));

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete broadcast news
     */
    @DeleteMapping("/broadcast-news/{newsId}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Delete broadcast news", description = "Deletes a broadcast news item")
    public ResponseEntity<?> deleteBroadcastNews(@PathVariable Long newsId,
            Authentication authentication) {
        try {
            broadcastNewsService.deleteNews(newsId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Broadcast news deleted successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete news: " + e.getMessage()));
        }
    }

    // ========== DIAGNOSTIC ENDPOINTS ==========

    /**
     * Check for duplicate data before applying unique constraints
     */
    @GetMapping("/diagnostics/check-duplicates")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Check for duplicate data", description = "Checks for duplicate expansions and card templates that would violate unique constraints")
    public ResponseEntity<?> checkForDuplicates() {
        Map<String, Object> results = new HashMap<>();

        // This would require custom repository queries to find duplicates
        results.put("message", "Duplicate check endpoint - implementation pending");
        results.put("note",
                "Before running V27 migration, manually check for duplicates in expansions and card_templates tables");

        return ResponseEntity.ok(results);
    }

    // ========== SHOP SUGGESTION ENDPOINTS ==========

    @GetMapping("/shop-suggestions")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Get all shop suggestions", description = "Retrieves all shop suggestions from users")
    public ResponseEntity<?> getAllShopSuggestions(@RequestParam(required = false) String status) {
        List<ShopSuggestion> suggestions;

        if (status != null && !status.isEmpty()) {
            try {
                ShopSuggestion.SuggestionStatus statusEnum = ShopSuggestion.SuggestionStatus
                        .valueOf(status.toUpperCase());
                suggestions = shopSuggestionRepository.findByStatusOrderByCreatedAtDesc(statusEnum);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
            }
        } else {
            suggestions = shopSuggestionRepository.findAllByOrderByCreatedAtDesc();
        }

        List<ShopSuggestionDTO> dtos = suggestions.stream().map(s -> {
            ShopSuggestionDTO dto = new ShopSuggestionDTO(s);
            // Add username if needed
            if (s.getUserId() != null) {
                userService.getUserById(s.getUserId()).ifPresent(user -> {
                    dto.setUsername(user.getUsername());
                });
            }
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/shop-suggestions/{id}/status")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Update shop suggestion status", description = "Updates the status of a shop suggestion")
    public ResponseEntity<?> updateSuggestionStatus(
            @PathVariable Long id,
            @RequestParam String status,
            @RequestParam(required = false) String notes) {

        Optional<ShopSuggestion> suggestionOpt = shopSuggestionRepository.findById(id);
        if (suggestionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        try {
            ShopSuggestion.SuggestionStatus statusEnum = ShopSuggestion.SuggestionStatus.valueOf(status.toUpperCase());
            ShopSuggestion suggestion = suggestionOpt.get();
            suggestion.setStatus(statusEnum);
            if (notes != null && !notes.isEmpty()) {
                suggestion.setNotes(notes);
            }

            shopSuggestionRepository.save(suggestion);

            return ResponseEntity.ok(Map.of(
                    "message", "Status updated successfully",
                    "suggestion", new ShopSuggestionDTO(suggestion)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid status value"));
        }
    }

    // ========== HELPER METHODS ==========

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
