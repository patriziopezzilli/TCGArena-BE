package com.tcg.arena.controller;

import com.tcg.arena.model.*;
import com.tcg.arena.repository.ShopRepository;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.ShopRewardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ShopRewardController {

    @Autowired
    private ShopRewardService shopRewardService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    // ==================== PUBLIC ENDPOINTS (App) ====================

    @GetMapping("/shops/{shopId}/rewards")
    public ResponseEntity<List<ShopReward>> getShopRewards(@PathVariable Long shopId) {
        List<ShopReward> rewards = shopRewardService.getActiveShopRewards(shopId);
        return ResponseEntity.ok(rewards);
    }

    @GetMapping("/shop-rewards/available")
    public ResponseEntity<List<ShopReward>> getAllAvailableRewards() {
        List<ShopReward> rewards = shopRewardService.getAllAvailableRewards();
        return ResponseEntity.ok(rewards);
    }

    @PostMapping("/shop-rewards/{rewardId}/redeem")
    public ResponseEntity<?> redeemReward(
            @PathVariable Long rewardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            ShopRewardRedemption redemption = shopRewardService.redeemReward(rewardId, userOpt.get().getId());
            return ResponseEntity.ok(redemption);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/shop-rewards/my-redemptions")
    public ResponseEntity<List<ShopRewardRedemption>> getMyRedemptions(
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ShopRewardRedemption> redemptions = shopRewardService.getUserRedemptions(userOpt.get().getId());
        return ResponseEntity.ok(redemptions);
    }

    // ==================== MERCHANT ENDPOINTS ====================

    @GetMapping("/merchant/rewards")
    public ResponseEntity<?> getMerchantRewards(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Long shopId = userOpt.get().getShopId();
        if (shopId == null) {
            return ResponseEntity.badRequest().body("No shop associated with this merchant");
        }

        List<ShopReward> rewards = shopRewardService.getShopRewards(shopId);
        return ResponseEntity.ok(rewards);
    }

    @PostMapping("/merchant/rewards")
    public ResponseEntity<?> createReward(
            @RequestBody ShopReward reward,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated with this merchant");
            }

            ShopReward created = shopRewardService.createReward(shopId, reward);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/merchant/rewards/{rewardId}")
    public ResponseEntity<?> updateReward(
            @PathVariable Long rewardId,
            @RequestBody ShopReward reward,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated with this merchant");
            }

            ShopReward updated = shopRewardService.updateReward(rewardId, shopId, reward);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/merchant/rewards/{rewardId}")
    public ResponseEntity<?> deleteReward(
            @PathVariable Long rewardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated");
            }

            shopRewardService.deleteReward(rewardId, shopId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/merchant/rewards/{rewardId}/toggle")
    public ResponseEntity<?> toggleRewardActive(
            @PathVariable Long rewardId,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated");
            }

            Boolean active = body.get("active");
            if (active == null) {
                return ResponseEntity.badRequest().body("Missing 'active' field");
            }

            shopRewardService.toggleRewardActive(rewardId, shopId, active);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== MERCHANT REDEMPTION MANAGEMENT ====================

    @GetMapping("/merchant/redemptions")
    public ResponseEntity<?> getMerchantRedemptions(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Long shopId = userOpt.get().getShopId();
        if (shopId == null) {
            return ResponseEntity.badRequest().body("No shop associated");
        }

        List<ShopRewardRedemption> redemptions = shopRewardService.getShopRedemptions(shopId);
        return ResponseEntity.ok(redemptions);
    }

    @GetMapping("/merchant/redemptions/pending")
    public ResponseEntity<?> getPendingRedemptions(@AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Long shopId = userOpt.get().getShopId();
        if (shopId == null) {
            return ResponseEntity.badRequest().body("No shop associated");
        }

        List<ShopRewardRedemption> pending = shopRewardService.getPendingRedemptions(shopId);
        return ResponseEntity.ok(pending);
    }

    @PostMapping("/merchant/redemptions/{redemptionId}/fulfill")
    public ResponseEntity<?> fulfillRedemption(
            @PathVariable Long redemptionId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated");
            }

            String voucherCode = body.get("voucherCode");
            String trackingCode = body.get("trackingCode");
            String notes = body.get("notes");

            ShopRewardRedemption fulfilled = shopRewardService.fulfillRedemption(
                    redemptionId, shopId, voucherCode, trackingCode, notes);
            return ResponseEntity.ok(fulfilled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/merchant/redemptions/{redemptionId}/cancel")
    public ResponseEntity<?> cancelRedemption(
            @PathVariable Long redemptionId,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            Long shopId = userOpt.get().getShopId();
            if (shopId == null) {
                return ResponseEntity.badRequest().body("No shop associated");
            }

            ShopRewardRedemption cancelled = shopRewardService.cancelRedemption(redemptionId, shopId);
            return ResponseEntity.ok(cancelled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== LOOKUP BY CODE (For in-store verification) ====================

    @GetMapping("/merchant/redemptions/code/{code}")
    public ResponseEntity<?> findByCode(
            @PathVariable String code,
            @AuthenticationPrincipal UserDetails userDetails) {
        Optional<User> userOpt = userRepository.findByEmail(userDetails.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        Long shopId = userOpt.get().getShopId();
        if (shopId == null) {
            return ResponseEntity.badRequest().body("No shop associated");
        }

        Optional<ShopRewardRedemption> redemption = shopRewardService.findByRedemptionCode(code);
        if (redemption.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Verify the redemption belongs to this shop
        if (!redemption.get().getShopReward().getShop().getId().equals(shopId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This redemption belongs to another shop");
        }

        return ResponseEntity.ok(redemption.get());
    }

    // ==================== POINTS CRITERIA INFO ====================

    @GetMapping("/rewards/points-criteria")
    public ResponseEntity<Map<String, Object>> getPointsCriteria() {
        // Return the points criteria shown in the app
        Map<String, Object> criteria = Map.of(
            "tournaments", Map.of(
                "firstPlace", 100,
                "secondPlace", 50,
                "thirdPlace", 25,
                "checkIn", 25,
                "registration", 15,
                "withdrawal", -10
            ),
            "collection", Map.of(
                "firstDeck", 50,
                "newDeck", 10
            ),
            "shops", Map.of(
                "reservation", 10
            ),
            "trades", Map.of(
                "completedTrade", 50
            )
        );
        return ResponseEntity.ok(criteria);
    }
}
