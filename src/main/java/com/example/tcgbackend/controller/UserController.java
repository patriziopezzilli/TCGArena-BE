package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.User;
import com.example.tcgbackend.service.UserService;
import com.example.tcgbackend.service.UserStatsService;
import com.example.tcgbackend.model.UserStats;
import com.example.tcgbackend.repository.UserRepository;
import com.example.tcgbackend.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "API for managing users in the TCG Arena system")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserStatsService userStatsService;

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieves a list of all registered users")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users")
    })
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User found and returned"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> getUserById(@Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
        return userService.getUserById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    @Operation(summary = "Search users", description = "Searches for users based on a query string")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results")
    })
    public List<User> searchUsers(@Parameter(description = "Search query string") @RequestParam String query) {
        // Implement search logic, for now return all
        return userService.getAllUsers();
    }

    @GetMapping("/leaderboard")
    @Operation(summary = "Get user leaderboard", description = "Retrieves the leaderboard of users based on their points")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved leaderboard")
    })
    public List<User> getLeaderboard() {
        // Return users ordered by points descending
        return userRepository.findAllByOrderByPointsDesc();
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Creates a new user account in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid user data provided")
    })
    public User createUser(@Parameter(description = "User object to be created") @RequestBody User user) {
        return userService.saveUser(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing user", description = "Updates the details of an existing user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> updateUser(@Parameter(description = "Unique identifier of the user to update") @PathVariable Long id, @Parameter(description = "Updated user object") @RequestBody User user) {
        return userService.updateUser(id, user)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user", description = "Deletes a user account from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User deleted successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Void> deleteUser(@Parameter(description = "Unique identifier of the user to delete") @PathVariable Long id) {
        if (userService.deleteUser(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/profile-image")
    @Operation(summary = "Update user profile image", description = "Updates the profile image URL for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile image updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> updateUserProfileImage(@Parameter(description = "Unique identifier of the user") @PathVariable Long id, @Parameter(description = "Profile image URL") @RequestBody String profileImageUrl) {
        return userService.getUserById(id).map(user -> {
            user.setProfileImageUrl(profileImageUrl);
            User updatedUser = userRepository.save(user);
            
            // Log profile image update activity
            userActivityService.logActivity(id,
                com.example.tcgbackend.model.ActivityType.USER_PROFILE_UPDATED,
                "Updated profile image");
            
            return ResponseEntity.ok(updatedUser);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/device-token")
    @Operation(summary = "Update user device token", description = "Updates the device token for push notifications for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Device token updated successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<User> updateUserDeviceToken(@Parameter(description = "Unique identifier of the user") @PathVariable Long id, @Parameter(description = "Device token for push notifications") @RequestBody String deviceToken) {
        return userService.getUserById(id).map(user -> {
            user.setDeviceToken(deviceToken);
            User updatedUser = userRepository.save(user);

            return ResponseEntity.ok(updatedUser);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/stats")
    @Operation(summary = "Get user statistics", description = "Retrieves detailed statistics for a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User statistics retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserStats> getUserStats(@Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
        return userService.getUserById(id).map(user -> {
            UserStats stats = userStatsService.getOrCreateUserStats(user);
            return ResponseEntity.ok(stats);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/leaderboard/stats")
    @Operation(summary = "Get user leaderboard stats", description = "Retrieves the leaderboard of users with detailed statistics")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Leaderboard stats retrieved successfully")
    })
    public List<UserStats> getLeaderboardStats(@Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int limit) {
        return userStatsService.getLeaderboard(limit);
    }

    @GetMapping("/leaderboard/active")
    @Operation(summary = "Get active players leaderboard", description = "Retrieves the leaderboard of active players")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active players leaderboard retrieved successfully")
    })
    public List<User> getActivePlayersLeaderboard() {
        // For now, return same as general leaderboard
        // TODO: Implement logic for active players (recent tournament participants)
        return userRepository.findAllByOrderByPointsDesc();
    }
}