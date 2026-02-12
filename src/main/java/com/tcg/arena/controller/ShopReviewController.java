package com.tcg.arena.controller;

import com.tcg.arena.model.ShopReview;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.ShopReviewRepository;
import com.tcg.arena.repository.ShopRepository;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/shops")
@Tag(name = "Shop Reviews", description = "API for managing shop reviews")
public class ShopReviewController {

    @Autowired
    private ShopReviewRepository shopReviewRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @GetMapping("/{id}/reviews")
    @Operation(summary = "Get shop reviews", description = "Retrieves a list of reviews for a specific shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved reviews")
    })
    public List<ShopReview> getShopReviews(
            @Parameter(description = "Shop ID") @PathVariable Long id) {

        List<ShopReview> reviews = shopReviewRepository.findByShopIdOrderByCreatedAtDesc(id);

        // Populate user details for each review
        reviews.forEach(review -> {
            userRepository.findById(review.getUserId()).ifPresent(user -> {
                review.setUsername(user.getUsername());
                review.setUserAvatarUrl(user.getProfileImageUrl());
            });
        });

        return reviews;
    }

    @PostMapping("/{id}/reviews")
    @Operation(summary = "Add a review", description = "Adds a new review for a shop")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Review added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid data or user already reviewed"),
            @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<?> addReview(
            @Parameter(description = "Shop ID") @PathVariable Long id,
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        User user = userOpt.get();
        Long userId = user.getId();

        // Check if user already reviewed this shop
        if (shopReviewRepository.findByShopIdAndUserId(id, userId).isPresent()) {
            return ResponseEntity.badRequest().body("You have already reviewed this shop.");
        }

        try {
            Integer rating = (Integer) payload.get("rating");
            String comment = (String) payload.get("comment");

            if (rating == null || rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body("Rating must be between 1 and 5.");
            }

            ShopReview review = new ShopReview();
            review.setShopId(id);
            review.setUserId(userId);
            review.setRating(rating);
            review.setComment(comment);
            review.setCreatedAt(LocalDateTime.now());

            ShopReview savedReview = shopReviewRepository.save(review);

            // Update shop's averageRating and reviewCount
            shopRepository.findById(id).ifPresent(shop -> {
                Double avgRating = shopReviewRepository.getAverageRating(id);
                Long count = shopReviewRepository.countByShopId(id);
                shop.setAverageRating(avgRating);
                shop.setReviewCount(count != null ? count.intValue() : 0);
                shopRepository.save(shop);
            });

            // Populate transient fields for immediate return
            savedReview.setUsername(user.getUsername());
            savedReview.setUserAvatarUrl(user.getProfileImageUrl());

            // Log activity
            userActivityService.logActivity(userId, com.tcg.arena.model.ActivityType.SHOP_REVIEWED,
                    "Recensito un negozio");

            return ResponseEntity.ok(savedReview);

        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid request data");
        }
    }

    @GetMapping("/{id}/reviews/me")
    @Operation(summary = "Get my review", description = "Checks if the current user has reviewed this shop")
    public ResponseEntity<?> getMyReview(
            @Parameter(description = "Shop ID") @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).body("Not authenticated");
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body("User not found");
        }

        Optional<ShopReview> review = shopReviewRepository.findByShopIdAndUserId(id, userOpt.get().getId());

        if (review.isPresent()) {
            return ResponseEntity.ok(review.get());
        } else {
            return ResponseEntity.noContent().build();
        }
    }
}
