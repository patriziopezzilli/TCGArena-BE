package com.tcg.arena.controller;

import com.tcg.arena.model.Achievement;
import com.tcg.arena.model.UserAchievement;
import com.tcg.arena.service.AchievementService;
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
@RequestMapping("/api/achievements")
@Tag(name = "Achievements", description = "API for managing achievements system")
public class AchievementController {

    @Autowired
    private AchievementService achievementService;

    @GetMapping
    @Operation(summary = "Get all active achievements", description = "Retrieves a list of all active achievements")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of achievements")
    })
    public List<Achievement> getAllActiveAchievements() {
        return achievementService.getAllActiveAchievements();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get achievement by ID", description = "Retrieves a specific achievement by its unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Achievement found and returned"),
        @ApiResponse(responseCode = "404", description = "Achievement not found")
    })
    public ResponseEntity<Achievement> getAchievementById(@Parameter(description = "Unique identifier of the achievement") @PathVariable Long id) {
        return achievementService.getAchievementById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/user")
    @Operation(summary = "Get user achievements", description = "Retrieves the achievements unlocked by the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User achievements retrieved successfully")
    })
    public List<UserAchievement> getUserAchievements() {
        // TODO: Get from authentication
        Long userId = 1L;
        return achievementService.getUserAchievements(userId);
    }

    // Admin endpoints
    @PostMapping
    @Operation(summary = "Create a new achievement", description = "Creates a new achievement (Admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Achievement created successfully")
    })
    public Achievement createAchievement(@RequestBody Achievement achievement) {
        return achievementService.saveAchievement(achievement);
    }

    @PostMapping("/{id}/unlock")
    @Operation(summary = "Unlock achievement for user", description = "Manually unlocks an achievement for the authenticated user (for testing)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Achievement unlocked successfully"),
        @ApiResponse(responseCode = "400", description = "Achievement already unlocked or not found")
    })
    public ResponseEntity<Map<String, String>> unlockAchievement(@Parameter(description = "ID of the achievement") @PathVariable Long id) {
        // TODO: Get from authentication
        Long userId = 1L;
        boolean success = achievementService.unlockAchievement(userId, id);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Achievement unlocked successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "Achievement already unlocked or not found"));
        }
    }
}