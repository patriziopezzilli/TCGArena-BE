package com.tcg.arena.controller;

import com.tcg.arena.dto.UpdateProfileRequest;
import com.tcg.arena.dto.UserWithStatsDTO;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserCard;
import com.tcg.arena.service.UserService;
import com.tcg.arena.service.UserStatsService;
import com.tcg.arena.service.UserCardService;
import com.tcg.arena.model.UserStats;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.UserActivityService;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.security.JwtUserDetailsService;
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

        @Autowired
        private UserCardService userCardService;

        @Autowired
        private JwtTokenUtil jwtTokenUtil;

        @Autowired
        private JwtUserDetailsService userDetailsService;

        @GetMapping
        @Operation(summary = "Get all users with stats", description = "Retrieves a list of all registered users with their statistics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of users with stats")
        })
        public List<UserWithStatsDTO> getAllUsers() {
                return userService.getAllUsersWithStats();
        }

        @GetMapping("/me")
        @Operation(summary = "Get current user", description = "Retrieves the currently authenticated user's profile")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved current user"),
                        @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<?> getCurrentUser(
                        org.springframework.security.core.Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        return ResponseEntity.status(401).body("Not authenticated");
                }

                String username = authentication.getName();
                return userService.getUserByUsername(username)
                                .map(ResponseEntity::ok)
                                .orElse(ResponseEntity.status(404).build());
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get user by ID", description = "Retrieves a specific user by their unique ID with stats")
        public ResponseEntity<UserWithStatsDTO> getUserById(@PathVariable Long id) {
                return userService.getUserById(id).map(user -> {
                        UserStats stats = userStatsService.getOrCreateUserStats(user);
                        return ResponseEntity.ok(UserWithStatsDTO.fromUserAndStats(user, stats));
                }).orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/search")
        @Operation(summary = "Search users", description = "Searches for users based on a query string")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results")
        })
        public List<UserWithStatsDTO> searchUsers(
                        @Parameter(description = "Search query string") @RequestParam String query) {
                // Implement search logic, for now return all with stats
                return userService.getAllUsersWithStats();
        }

        @GetMapping("/check-username")
        @Operation(summary = "Check username availability", description = "Checks if a username is already taken")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Returns true if available, false if taken")
        })
        public ResponseEntity<java.util.Map<String, Boolean>> checkUsernameAvailability(
                        @Parameter(description = "Username to check") @RequestParam String username) {
                boolean available = userService.isUsernameAvailable(username);
                return ResponseEntity.ok(java.util.Map.of("available", available));
        }

        @GetMapping("/leaderboard")
        @Operation(summary = "Get user leaderboard with stats", description = "Retrieves the leaderboard of users with their full statistics")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved leaderboard with stats")
        })
        public List<UserWithStatsDTO> getLeaderboard() {
                return userService.getLeaderboardWithStats();
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
        public ResponseEntity<User> updateUser(
                        @Parameter(description = "Unique identifier of the user to update") @PathVariable Long id,
                        @Parameter(description = "Updated user object") @RequestBody User user) {
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
        public ResponseEntity<Void> deleteUser(
                        @Parameter(description = "Unique identifier of the user to delete") @PathVariable Long id) {
                if (userService.deleteUser(id)) {
                        return ResponseEntity.noContent().build();
                }
                return ResponseEntity.notFound().build();
        }

        @PatchMapping("/{id}/profile")
        @Operation(summary = "Update user profile partially", description = "Updates only specific profile fields (displayName, bio, favoriteGame)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<?> updateUserProfile(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Profile update request") @RequestBody UpdateProfileRequest request) {
                return userService.getUserById(id).map(user -> {
                        // Display Name Update
                        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
                                user.setDisplayName(request.getDisplayName());
                        }

                        // Username Update (Limited to 2 changes)
                        if (request.getUsername() != null && !request.getUsername().isBlank()
                                        && !request.getUsername().equals(user.getUsername())) {

                                // Check if user has already changed username 2 times
                                if (user.getUsernameChangeCount() >= 2) {
                                        return ResponseEntity.badRequest()
                                                        .body("Hai già esaurito i 2 cambi username consentiti.");
                                }

                                // Check if new username is available
                                if (!userService.isUsernameAvailable(request.getUsername())) {
                                        return ResponseEntity.badRequest().body("Username già in uso.");
                                }

                                user.setUsername(request.getUsername());
                                user.setUsernameChangeCount(user.getUsernameChangeCount() + 1);
                        }

                        if (request.getFavoriteGame() != null && !request.getFavoriteGame().isBlank()) {
                                try {
                                        user.setFavoriteGame(com.tcg.arena.model.TCGType
                                                        .valueOf(request.getFavoriteGame().toUpperCase()));
                                } catch (IllegalArgumentException e) {
                                        // Invalid TCG type, ignore
                                }
                        }
                        // Note: bio field is not currently in User model, skipped

                        // Locale Update
                        if (request.getLocale() != null && !request.getLocale().isBlank()) {
                                try {
                                        String loc = request.getLocale().toLowerCase();
                                        if (loc.length() >= 2) {
                                                loc = loc.substring(0, 2);
                                                if (loc.equals("en") || loc.equals("it")) {
                                                        user.setLocale(loc);
                                                }
                                        }
                                } catch (Exception e) {
                                        // Ignore invalid locale
                                }
                        }

                        User updatedUser = userRepository.save(user);

                        // Log profile update activity
                        userActivityService.logActivity(id,
                                        com.tcg.arena.model.ActivityType.USER_PROFILE_UPDATED,
                                        "Aggiornato profilo");

                        // If username was updated, generate new tokens
                        if (request.getUsername() != null && !request.getUsername().isBlank()) {
                                final org.springframework.security.core.userdetails.UserDetails userDetails = userDetailsService
                                                .loadUserByUsername(updatedUser.getUsername());
                                final String token = jwtTokenUtil.generateToken(userDetails);
                                final String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails);

                                java.util.Map<String, Object> response = new java.util.HashMap<>();
                                response.put("token", token);
                                response.put("refreshToken", refreshToken);
                                response.put("user", updatedUser);
                                return ResponseEntity.ok(response);
                        }

                        return ResponseEntity.ok((Object) updatedUser);
                }).orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/profile-image")
        @Operation(summary = "Update user profile image", description = "Updates the profile image URL for a specific user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Profile image updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<User> updateUserProfileImage(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Profile image URL") @RequestBody String profileImageUrl) {
                return userService.getUserById(id).map(user -> {
                        user.setProfileImageUrl(profileImageUrl);
                        User updatedUser = userRepository.save(user);

                        // Log profile image update activity
                        userActivityService.logActivity(id,
                                        com.tcg.arena.model.ActivityType.USER_PROFILE_UPDATED,
                                        "Aggiornata immagine profilo");

                        return ResponseEntity.ok(updatedUser);
                }).orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/device-token")
        @Operation(summary = "Update user device token", description = "Updates the device token for push notifications for a specific user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Device token updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<User> updateUserDeviceToken(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Device token for push notifications") @RequestBody String deviceToken) {
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
        public ResponseEntity<UserStats> getUserStats(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
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
        public List<UserStats> getLeaderboardStats(
                        @Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int limit) {
                return userStatsService.getLeaderboard(limit);
        }

        @GetMapping("/leaderboard/active")
        @Operation(summary = "Get active players leaderboard", description = "Retrieves the leaderboard of active players")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Active players leaderboard retrieved successfully")
        })
        public List<UserStats> getActivePlayersLeaderboard(
                        @Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int limit) {
                return userStatsService.getActivePlayersLeaderboard(limit);
        }

        @GetMapping("/leaderboard/collection")
        @Operation(summary = "Get collection leaderboard", description = "Retrieves the leaderboard of users by total cards collected")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Collection leaderboard retrieved successfully")
        })
        public List<UserStats> getCollectionLeaderboard(
                        @Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int limit) {
                return userStatsService.getTopCollectors(limit);
        }

        @GetMapping("/leaderboard/tournaments")
        @Operation(summary = "Get tournament leaderboard", description = "Retrieves the leaderboard of users by tournament performance")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Tournament leaderboard retrieved successfully")
        })
        public List<UserStats> getTournamentLeaderboard(
                        @Parameter(description = "Maximum number of results to return") @RequestParam(defaultValue = "50") int limit) {
                return userStatsService.getTopTournamentPlayers(limit);
        }

        @GetMapping("/{id}/favorite-tcgs")
        @Operation(summary = "Get user's favorite TCG types", description = "Retrieves the list of TCG types the user has marked as favorites")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Favorite TCGs retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<List<com.tcg.arena.model.TCGType>> getFavoriteTCGs(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
                return userService.getUserById(id)
                                .map(user -> ResponseEntity.ok(user.getFavoriteTCGTypes()))
                                .orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/favorite-tcgs")
        @Operation(summary = "Update user's favorite TCG types", description = "Updates the list of TCG types the user has marked as favorites")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Favorite TCGs updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<List<com.tcg.arena.model.TCGType>> updateFavoriteTCGs(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "List of TCG types to set as favorites") @RequestBody List<com.tcg.arena.model.TCGType> favoriteTCGs) {
                System.out.println("📝 Received favorite TCGs update for user " + id + ": " + favoriteTCGs);
                return userService.getUserById(id).map(user -> {
                        System.out.println("📝 Before update: " + user.getFavoriteTCGTypesString());
                        user.setFavoriteTCGTypes(favoriteTCGs);
                        System.out.println("📝 After setFavoriteTCGTypes: " + user.getFavoriteTCGTypesString());
                        userRepository.save(user);
                        System.out.println("📝 Returning: " + user.getFavoriteTCGTypes());
                        return ResponseEntity.ok(user.getFavoriteTCGTypes());
                }).orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/privacy")
        @Operation(summary = "Update user privacy setting", description = "Updates whether the user profile is hidden from Discover section")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Privacy setting updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<java.util.Map<String, Boolean>> updatePrivacy(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Privacy setting") @RequestBody java.util.Map<String, Boolean> request) {
                return userService.getUserById(id).map(user -> {
                        Boolean isPrivate = request.getOrDefault("isPrivate", false);
                        user.setIsPrivate(isPrivate);
                        userRepository.save(user);
                        return ResponseEntity.ok(java.util.Map.of("isPrivate", user.getIsPrivate()));
                }).orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/{id}/cards")
        @Operation(summary = "Get user's inventory cards", description = "Retrieves all inventory cards for a specific user (public endpoint for community profiles)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's inventory cards"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<List<UserCard>> getUserCards(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
                return userService.getUserById(id).map(user -> {
                        List<UserCard> userCards = userCardService.getUserCardsByUserId(id);
                        return ResponseEntity.ok(userCards);
                }).orElse(ResponseEntity.notFound().build());
        }

        @PutMapping("/{id}/location")
        @Operation(summary = "Update user location", description = "Updates the GPS location and city/country for a specific user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Location updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<User> updateLocation(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Location update request") @RequestBody com.tcg.arena.dto.LocationUpdateRequest request) {
                return userService.getUserById(id).map(user -> {
                        com.tcg.arena.model.UserLocation location = new com.tcg.arena.model.UserLocation();
                        location.setLatitude(request.getLatitude());
                        location.setLongitude(request.getLongitude());
                        location.setCity(request.getCity());
                        location.setCountry(request.getCountry());
                        user.setLocation(location);

                        User updatedUser = userRepository.save(user); // Use repository directly or service if available
                        // Ideally strictly use service, but for simple property update repo is fine or
                        // add method to service
                        // Using userRepository here for consistency with other methods in this
                        // controller
                        // (Note: updateUserProfile uses userRepository.save)

                        return ResponseEntity.ok(updatedUser);
                }).orElse(ResponseEntity.notFound().build());
        }

        @GetMapping("/{id}/notification-preferences")
        @Operation(summary = "Get user notification preferences", description = "Retrieves user's current notification preferences")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notification preferences retrieved successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<java.util.Map<String, Object>> getNotificationPreferences(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id) {
                return userService.getUserById(id).map(user -> {
                        java.util.Map<String, Object> preferences = new java.util.HashMap<>();
                        preferences.put("emailNotificationsEnabled", user.getEmailNotificationsEnabled());
                        return ResponseEntity.ok(preferences);
                }).orElse(ResponseEntity.notFound().build());
        }

        @PatchMapping("/{id}/notification-preferences")
        @Operation(summary = "Update user notification preferences", description = "Updates user's email notification preferences")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notification preferences updated successfully"),
                        @ApiResponse(responseCode = "404", description = "User not found")
        })
        public ResponseEntity<User> updateNotificationPreferences(
                        @Parameter(description = "Unique identifier of the user") @PathVariable Long id,
                        @Parameter(description = "Email notifications enabled flag") @RequestParam boolean emailNotificationsEnabled) {
                return userService.getUserById(id).map(user -> {
                        user.setEmailNotificationsEnabled(emailNotificationsEnabled);

                        User updatedUser = userRepository.save(user);

                        // Log preference update activity
                        userActivityService.logActivity(id,
                                        com.tcg.arena.model.ActivityType.USER_PREFERENCES_UPDATED,
                                        "Aggiornate preferenze notifiche email: "
                                                        + (emailNotificationsEnabled ? "abilitate" : "disabilitate"));

                        return ResponseEntity.ok(updatedUser);
                }).orElse(ResponseEntity.notFound().build());
        }

        @PostMapping("/favorite-tcgs")
        @Operation(summary = "Update favorite TCGs", description = "Updates user's favorite TCG types for event notifications")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Favorite TCGs updated successfully"),
                        @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<?> updateFavoriteTCGs(
                        @RequestBody java.util.Map<String, Object> requestBody,
                        org.springframework.security.core.Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        return ResponseEntity.status(401).body("Not authenticated");
                }

                String username = authentication.getName();
                return userService.getUserByUsername(username).map(user -> {
                        @SuppressWarnings("unchecked")
                        java.util.List<String> favoriteTCGs = (java.util.List<String>) requestBody.get("favoriteTCGs");

                        if (favoriteTCGs == null || favoriteTCGs.isEmpty()) {
                                user.setFavoriteTCGTypesString("");
                        } else {
                                // Convert list to comma-separated string
                                String tcgString = String.join(",", favoriteTCGs);
                                user.setFavoriteTCGTypesString(tcgString);
                        }

                        User updatedUser = userRepository.save(user);

                        // Log activity
                        userActivityService.logActivity(user.getId(),
                                        com.tcg.arena.model.ActivityType.USER_PREFERENCES_UPDATED,
                                        "Aggiornati TCG favoriti per notifiche: " + String.join(", ", favoriteTCGs));

                        java.util.Map<String, Object> response = new java.util.HashMap<>();
                        response.put("message", "Favorite TCGs updated successfully");
                        response.put("favoriteTCGs", favoriteTCGs);

                        return ResponseEntity.ok(response);
                }).orElseGet(() -> {
                        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                        errorResponse.put("error", "User not found");
                        return ResponseEntity.status(404).body(errorResponse);
                });
        }

        @GetMapping("/favorite-tcgs")
        @Operation(summary = "Get favorite TCGs", description = "Retrieves user's favorite TCG types")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Favorite TCGs retrieved successfully"),
                        @ApiResponse(responseCode = "401", description = "Not authenticated")
        })
        public ResponseEntity<?> getFavoriteTCGs(
                        org.springframework.security.core.Authentication authentication) {
                if (authentication == null || !authentication.isAuthenticated()) {
                        return ResponseEntity.status(401).body("Not authenticated");
                }

                String username = authentication.getName();
                return userService.getUserByUsername(username).map(user -> {
                        String tcgString = user.getFavoriteTCGTypesString();
                        java.util.List<String> favoriteTCGs = new java.util.ArrayList<>();

                        if (tcgString != null && !tcgString.isEmpty()) {
                                favoriteTCGs = java.util.Arrays.asList(tcgString.split(","));
                        }

                        java.util.Map<String, Object> response = new java.util.HashMap<>();
                        response.put("favoriteTCGs", favoriteTCGs);

                        return ResponseEntity.ok(response);
                }).orElseGet(() -> {
                        java.util.Map<String, Object> errorResponse = new java.util.HashMap<>();
                        errorResponse.put("error", "User not found");
                        return ResponseEntity.status(404).body(errorResponse);
                });
        }
}