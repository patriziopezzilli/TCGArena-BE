package com.tcg.arena.controller;

import com.tcg.arena.dto.ShopDTO;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopSubscription;
import com.tcg.arena.model.User;
import com.tcg.arena.service.ShopService;
import com.tcg.arena.service.ShopSubscriptionService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/shops")
@Tag(name = "Shops", description = "API for managing shops and subscriptions")
public class ShopController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private ShopSubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all shops", description = "Retrieves all shops")
    public List<ShopDTO> getAllShops() {
        return shopService.getAllShops().stream()
                .map(ShopDTO::new)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get shop by ID", description = "Retrieves a specific shop by its ID")
    public ResponseEntity<ShopDTO> getShopById(@Parameter(description = "ID of the shop") @PathVariable Long id) {
        return shopService.getShopById(id)
            .map(ShopDTO::new)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create shop", description = "Creates a new shop")
    public ShopDTO createShop(@RequestBody ShopDTO shopDTO) {
        Shop shop = new Shop();
        updateShopFromDTO(shop, shopDTO);
        Shop savedShop = shopService.saveShop(shop);
        return new ShopDTO(savedShop);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update shop", description = "Updates an existing shop")
    public ResponseEntity<ShopDTO> updateShop(@Parameter(description = "ID of the shop") @PathVariable Long id, @RequestBody ShopDTO shopDTO) {
        return shopService.getShopById(id)
            .map(shop -> {
                updateShopFromDTO(shop, shopDTO);
                Shop updatedShop = shopService.saveShop(shop);
                return ResponseEntity.ok(new ShopDTO(updatedShop));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    private void updateShopFromDTO(Shop shop, ShopDTO dto) {
        shop.setName(dto.getName());
        shop.setDescription(dto.getDescription());
        shop.setAddress(dto.getAddress());
        shop.setLatitude(dto.getLatitude());
        shop.setLongitude(dto.getLongitude());
        shop.setPhoneNumber(dto.getPhoneNumber());
        shop.setWebsiteUrl(dto.getWebsiteUrl());
        shop.setType(dto.getType());
        shop.setIsVerified(dto.getIsVerified());
        shop.setActive(dto.getActive());
        shop.setOwnerId(dto.getOwnerId());
        
        // Handle opening hours - prefer structured over legacy
        if (dto.getOpeningHoursStructured() != null) {
            shop.setOpeningHoursStructured(dto.getOpeningHoursStructured());
        } else if (dto.getOpeningHours() != null) {
            // Legacy support
            shop.setOpeningHours(dto.getOpeningHours());
            shop.setOpeningDays(dto.getOpeningDays());
        }
        
        shop.setInstagramUrl(dto.getInstagramUrl());
        shop.setFacebookUrl(dto.getFacebookUrl());
        shop.setTwitterUrl(dto.getTwitterUrl());
        shop.setEmail(dto.getEmail());
        shop.setPhotoBase64(dto.getPhotoBase64());
        shop.setTcgTypesList(dto.getTcgTypes());
        shop.setServicesList(dto.getServices());
        shop.setReservationDurationMinutes(dto.getReservationDurationMinutes());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete shop", description = "Deletes a shop")
    public ResponseEntity<Void> deleteShop(@Parameter(description = "ID of the shop") @PathVariable Long id) {
        if (shopService.deleteShop(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    // Shop Subscription Endpoints

    @PostMapping("/{shopId}/subscribe")
    @Operation(summary = "Subscribe to shop", description = "Subscribes the authenticated user to a shop for notifications")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully subscribed to shop"),
        @ApiResponse(responseCode = "400", description = "User already subscribed")
    })
    public ResponseEntity<Map<String, String>> subscribeToShop(@Parameter(description = "ID of the shop") @PathVariable Long shopId,
                                                                 Authentication authentication) {
        User user = getCurrentUser(authentication);
        ShopSubscription subscription = subscriptionService.subscribeToShop(user.getId(), shopId);
        return ResponseEntity.ok(Map.of("message", "Successfully subscribed to shop", "subscriptionId", subscription.getId().toString()));
    }

    @DeleteMapping("/{shopId}/subscribe")
    @Operation(summary = "Unsubscribe from shop", description = "Unsubscribes the authenticated user from a shop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully unsubscribed from shop"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    public ResponseEntity<Map<String, String>> unsubscribeFromShop(@Parameter(description = "ID of the shop") @PathVariable Long shopId,
                                                                     Authentication authentication) {
        User user = getCurrentUser(authentication);
        boolean unsubscribed = subscriptionService.unsubscribeFromShop(user.getId(), shopId);
        if (unsubscribed) {
            return ResponseEntity.ok(Map.of("message", "Successfully unsubscribed from shop"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{shopId}/subscription")
    @Operation(summary = "Check subscription status", description = "Checks if the authenticated user is subscribed to a shop")
    public ResponseEntity<Map<String, Boolean>> checkSubscription(@Parameter(description = "ID of the shop") @PathVariable Long shopId,
                                                                    Authentication authentication) {
        User user = getCurrentUser(authentication);
        boolean isSubscribed = subscriptionService.isUserSubscribedToShop(user.getId(), shopId);
        return ResponseEntity.ok(Map.of("subscribed", isSubscribed));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "Get user subscriptions", description = "Retrieves all shops the authenticated user is subscribed to")
    public List<ShopSubscription> getUserSubscriptions(Authentication authentication) {
        User user = getCurrentUser(authentication);
        Long userId = user.getId();
        return subscriptionService.getUserSubscriptions(userId);
    }

    @GetMapping("/{shopId}/subscribers")
    @Operation(summary = "Get shop subscribers", description = "Retrieves all users subscribed to a shop (merchant only)")
    public ResponseEntity<List<User>> getShopSubscribers(@Parameter(description = "ID of the shop") @PathVariable Long shopId) {
        // TODO: Check if user is merchant/owner of the shop
        List<User> subscribers = subscriptionService.getShopSubscriberUsers(shopId);
        return ResponseEntity.ok(subscribers);
    }

    @GetMapping("/{shopId}/subscriber-count")
    @Operation(summary = "Get subscriber count", description = "Gets the number of active subscribers for a shop")
    public ResponseEntity<Map<String, Long>> getSubscriberCount(@Parameter(description = "ID of the shop") @PathVariable Long shopId) {
        Long count = subscriptionService.getSubscriberCount(shopId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // Reservation Settings Endpoints

    @GetMapping("/{shopId}/reservation-settings")
    @Operation(summary = "Get reservation settings", description = "Gets the reservation settings for a shop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved reservation settings"),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<Map<String, Object>> getReservationSettings(@Parameter(description = "ID of the shop") @PathVariable Long shopId) {
        Integer duration = shopService.getReservationDuration(shopId);
        return ResponseEntity.ok(Map.of(
            "reservationDurationMinutes", duration,
            "defaultDurationMinutes", 30
        ));
    }

    @PutMapping("/{shopId}/reservation-settings")
    @Operation(summary = "Update reservation settings", description = "Updates the reservation settings for a shop")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated reservation settings"),
        @ApiResponse(responseCode = "400", description = "Invalid duration value"),
        @ApiResponse(responseCode = "404", description = "Shop not found")
    })
    public ResponseEntity<Map<String, Object>> updateReservationSettings(
            @Parameter(description = "ID of the shop") @PathVariable Long shopId,
            @RequestBody Map<String, Integer> settings) {
        
        Integer durationMinutes = settings.get("reservationDurationMinutes");
        if (durationMinutes == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "reservationDurationMinutes is required"));
        }
        
        try {
            Shop updatedShop = shopService.updateReservationDuration(shopId, durationMinutes)
                .orElse(null);
            
            if (updatedShop == null) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Reservation settings updated successfully",
                "reservationDurationMinutes", updatedShop.getReservationDurationMinutes()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Helper method to get current authenticated user
    private User getCurrentUser(Authentication authentication) {
        if (authentication == null) {
            throw new RuntimeException("User not authenticated");
        }
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}