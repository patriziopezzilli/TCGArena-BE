package com.tcg.arena.controller;

import com.tcg.arena.model.Reward;
import com.tcg.arena.model.RewardTransaction;
import com.tcg.arena.service.RewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rewards")
@Tag(name = "Rewards", description = "API for managing rewards and points system")
public class RewardController {

    @Autowired
    private RewardService rewardService;

    @GetMapping
    @Operation(summary = "Get all active rewards", description = "Retrieves a list of all active rewards available for redemption")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of rewards")
    })
    public List<Reward> getAllActiveRewards() {
        return rewardService.getAllActiveRewards();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get reward by ID", description = "Retrieves a specific reward by its unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward found and returned"),
        @ApiResponse(responseCode = "404", description = "Reward not found")
    })
    public ResponseEntity<Reward> getRewardById(@Parameter(description = "Unique identifier of the reward") @PathVariable Long id) {
        return rewardService.getRewardById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/redeem")
    @Operation(summary = "Redeem a reward", description = "Redeems a reward for the authenticated user if they have enough points")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward redeemed successfully"),
        @ApiResponse(responseCode = "400", description = "Insufficient points or reward not available")
    })
    public ResponseEntity<Map<String, String>> redeemReward(@Parameter(description = "ID of the reward to redeem") @PathVariable Long id) {
        // For now, assume user ID from auth, but need to implement properly
        // This is a placeholder - in real implementation, get from JWT
        Long userId = 1L; // TODO: Get from authentication
        boolean success = rewardService.redeemReward(userId, id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Reward redeemed successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient points or reward not available"));
        }
    }

    @GetMapping("/points")
    @Operation(summary = "Get user points", description = "Retrieves the current points balance for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Points balance retrieved successfully")
    })
    public ResponseEntity<Map<String, Integer>> getUserPoints() {
        // TODO: Get from authentication
        Long userId = 1L;
        Integer points = rewardService.getUserPoints(userId);
        return ResponseEntity.ok(Map.of("points", points));
    }

    @GetMapping("/history")
    @Operation(summary = "Get transaction history", description = "Retrieves the points transaction history for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully")
    })
    public List<RewardTransaction> getTransactionHistory() {
        // TODO: Get from authentication
        Long userId = 1L;
        return rewardService.getUserTransactionHistory(userId);
    }

    // Admin endpoints
    @PostMapping
    @Operation(summary = "Create a new reward", description = "Creates a new reward item (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Reward created successfully")
    })
    public Reward createReward(@RequestBody Reward reward) {
        return rewardService.saveReward(reward);
    }
}