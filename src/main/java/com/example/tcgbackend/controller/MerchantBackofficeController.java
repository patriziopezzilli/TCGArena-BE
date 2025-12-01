package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Shop;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.service.ShopService;
import com.example.tcgbackend.service.UserService;
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
        if (updatedShop.getName() != null) existingShop.setName(updatedShop.getName());
        if (updatedShop.getDescription() != null) existingShop.setDescription(updatedShop.getDescription());
        if (updatedShop.getAddress() != null) existingShop.setAddress(updatedShop.getAddress());
        if (updatedShop.getLatitude() != null) existingShop.setLatitude(updatedShop.getLatitude());
        if (updatedShop.getLongitude() != null) existingShop.setLongitude(updatedShop.getLongitude());
        if (updatedShop.getPhoneNumber() != null) existingShop.setPhoneNumber(updatedShop.getPhoneNumber());
        if (updatedShop.getWebsiteUrl() != null) existingShop.setWebsiteUrl(updatedShop.getWebsiteUrl());
        if (updatedShop.getOpeningHours() != null) existingShop.setOpeningHours(updatedShop.getOpeningHours());
        if (updatedShop.getOpeningDays() != null) existingShop.setOpeningDays(updatedShop.getOpeningDays());
        if (updatedShop.getInstagramUrl() != null) existingShop.setInstagramUrl(updatedShop.getInstagramUrl());
        if (updatedShop.getFacebookUrl() != null) existingShop.setFacebookUrl(updatedShop.getFacebookUrl());
        if (updatedShop.getTwitterUrl() != null) existingShop.setTwitterUrl(updatedShop.getTwitterUrl());
        if (updatedShop.getEmail() != null) existingShop.setEmail(updatedShop.getEmail());
        if (updatedShop.getType() != null) existingShop.setType(updatedShop.getType());

        Shop saved = shopService.saveShop(existingShop);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Shop updated successfully");
        response.put("shop", saved);

        return ResponseEntity.ok(response);
    }
}
