package com.tcg.arena.controller;

import com.tcg.arena.dto.MerchantDashboardStatsDTO;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.User;
import com.tcg.arena.service.MerchantDashboardService;
import com.tcg.arena.service.ShopService;
import com.tcg.arena.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/merchant")
public class MerchantBackofficeController {

    @Autowired
    private UserService userService;

    @Autowired
    private ShopService shopService;

    @Autowired
    private MerchantDashboardService merchantDashboardService;

    /**
     * Get merchant shop status
     * Returns shop info and active status for the logged merchant
     */
    @GetMapping("/shop/status")
    public ResponseEntity<?> getShopStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        Optional<Shop> shopOpt = shopService.getShopById(user.getShopId());
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("shop", shop);
        response.put("active", shop.getActive());
        response.put("verified", shop.getIsVerified());
        response.put("user", user);

        return ResponseEntity.ok(response);
    }

    /**
     * Get merchant dashboard statistics
     * Returns aggregated stats for inventory, reservations, tournaments, requests,
     * and subscribers
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getDashboardStats() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        try {
            MerchantDashboardStatsDTO stats = merchantDashboardService.getDashboardStats(user.getShopId());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Error retrieving dashboard statistics: " + e.getMessage());
        }
    }

    /**
     * Get merchant dashboard notifications
     * Returns actionable notifications for tournaments today, pending requests, and
     * active reservations
     */
    @GetMapping("/dashboard/notifications")
    public ResponseEntity<?> getDashboardNotifications() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        try {
            var notifications = merchantDashboardService.getMerchantNotifications(user.getShopId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error retrieving notifications: " + e.getMessage());
        }
    }

    /**
     * Get merchant profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getMerchantProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        return ResponseEntity.ok(user);
    }

    /**
     * Update merchant shop information
     */
    @PutMapping("/shop")
    public ResponseEntity<?> updateShop(@RequestBody Shop updatedShop) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        Optional<Shop> shopOpt = shopService.getShopById(user.getShopId());
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop existingShop = shopOpt.get();

        // Update allowed fields
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
        if (updatedShop.getTcgTypes() != null)
            existingShop.setTcgTypes(updatedShop.getTcgTypes());
        if (updatedShop.getServices() != null)
            existingShop.setServices(updatedShop.getServices());
        if (updatedShop.getPhotoBase64() != null)
            existingShop.setPhotoBase64(updatedShop.getPhotoBase64());

        Shop saved = shopService.saveShop(existingShop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop updated successfully");
        response.put("shop", saved);

        return ResponseEntity.ok(response);
    }

    /**
     * Upload shop photo (base64)
     */
    @PostMapping("/shop/photo")
    public ResponseEntity<?> uploadShopPhoto(@RequestBody Map<String, String> request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        Optional<User> userOpt = userService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        User user = userOpt.get();
        if (!user.getIsMerchant()) {
            return ResponseEntity.badRequest().body("User is not a merchant");
        }

        if (user.getShopId() == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        Optional<Shop> shopOpt = shopService.getShopById(user.getShopId());
        if (shopOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Shop not found");
        }

        Shop shop = shopOpt.get();

        String photoBase64 = request.get("photoBase64");
        if (photoBase64 == null || photoBase64.isEmpty()) {
            return ResponseEntity.badRequest().body("Photo base64 is required");
        }

        // Validate base64 format (basic check)
        if (!photoBase64.startsWith("data:image/")) {
            return ResponseEntity.badRequest().body("Invalid base64 image format");
        }

        // Update shop with photo base64
        shop.setPhotoBase64(photoBase64);
        shopService.saveShop(shop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Photo uploaded successfully");

        return ResponseEntity.ok(response);
    }
}
