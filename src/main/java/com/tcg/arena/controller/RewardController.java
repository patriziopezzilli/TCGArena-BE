package com.tcg.arena.controller;

import com.tcg.arena.model.Reward;
import com.tcg.arena.model.RewardTransaction;
import com.tcg.arena.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.tcg.arena.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@Tag(name = "Rewards", description = "API for managing rewards and points system")
public class RewardController {

    @Autowired
    private RewardService rewardService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all active rewards", description = "Retrieves a list of all active rewards available for redemption")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of rewards")
    })
    public List<Reward> getAllActiveRewards() {
        return rewardService.getAllActiveRewards();
    }

    @GetMapping("/partner/{partnerId}")
    @Operation(summary = "Get rewards by partner", description = "Retrieves a list of active rewards for a specific partner")
    public List<Reward> getRewardsByPartner(@PathVariable Long partnerId) {
        // We need to add this method to RewardService/Repository
        return rewardService.getRewardsByPartner(partnerId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reward by ID", description = "Retrieves a specific reward by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reward found and returned"),
            @ApiResponse(responseCode = "404", description = "Reward not found")
    })
    public ResponseEntity<Reward> getRewardById(
            @Parameter(description = "Unique identifier of the reward") @PathVariable Long id) {
        return rewardService.getRewardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/redeem")
    @Operation(summary = "Redeem a reward", description = "Redeems a reward for the authenticated user if they have enough points")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reward redeemed successfully"),
            @ApiResponse(responseCode = "400", description = "Insufficient points or reward not available"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<Map<String, String>> redeemReward(
            Authentication authentication,
            @Parameter(description = "ID of the reward to redeem") @PathVariable Long id) {

        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(user -> {
                    boolean success = rewardService.redeemReward(user.getId(), id);
                    if (success) {
                        return ResponseEntity.ok(Map.of("message", "Reward redeemed successfully"));
                    } else {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Insufficient points or reward not available"));
                    }
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/points")
    @Operation(summary = "Get user points", description = "Retrieves the current points balance for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Points balance retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<Map<String, Integer>> getUserPoints(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).build();
        }

        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(user -> {
                    Integer points = rewardService.getUserPoints(user.getId());
                    return ResponseEntity.ok(Map.of("points", points));
                })
                .orElse(ResponseEntity.status(401).build());
    }

    @GetMapping("/history")
    @Operation(summary = "Get transaction history", description = "Retrieves the points transaction history for the authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public List<RewardTransaction> getTransactionHistory(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }

        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .map(user -> rewardService.getUserTransactionHistory(user.getId()))
                .orElse(List.of());
    }

    // Admin endpoints
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new reward", description = "Creates a new reward item (Admin only)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reward created successfully")
    })
    public Reward createReward(@RequestBody Reward reward) {
        return rewardService.saveReward(reward);
    }

    // ========== ADMIN: Reward Transactions (Fulfillment) Management ==========

    @GetMapping("/admin/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reward transactions", description = "Retrieves all reward transactions for admin management")
    public List<RewardTransaction> getAllTransactions() {
        return rewardService.getAllRewardTransactions();
    }

    @GetMapping("/admin/transactions/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get pending reward fulfillments", description = "Retrieves reward transactions that need fulfillment (pending/processing)")
    public List<RewardTransaction> getPendingFulfillments() {
        return rewardService.getPendingFulfillments();
    }

    @PutMapping("/admin/transactions/{transactionId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update reward transaction", description = "Updates status, voucher code, or tracking number for a reward transaction")
    public ResponseEntity<RewardTransaction> updateTransaction(
            @PathVariable Long transactionId,
            @RequestBody Map<String, Object> updates) {
        try {
            RewardTransaction updated = rewardService.updateTransaction(
                    transactionId,
                    (String) updates.get("status"),
                    (String) updates.get("voucherCode"),
                    (String) updates.get("trackingNumber"));
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}