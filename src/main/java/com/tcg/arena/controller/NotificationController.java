package com.tcg.arena.controller;

import com.tcg.arena.model.Notification;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.FirebaseMessagingService;
import com.tcg.arena.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "API for managing notifications and device tokens")
public class NotificationController {

        @Autowired
        private NotificationService notificationService;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private FirebaseMessagingService firebaseMessagingService;

        private Long getUserIdFromAuth(Authentication authentication) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));
                return user.getId();
        }

        @GetMapping
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get user notifications", description = "Retrieves all notifications for the authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully")
        })
        public List<Notification> getUserNotifications(Authentication authentication) {
                Long userId = getUserIdFromAuth(authentication);
                return notificationService.getUserNotifications(userId);
        }

        @GetMapping("/unread")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Get unread notifications", description = "Retrieves unread notifications for the authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Unread notifications retrieved successfully")
        })
        public List<Notification> getUnreadNotifications(Authentication authentication) {
                Long userId = getUserIdFromAuth(authentication);
                return notificationService.getUnreadNotifications(userId);
        }

        @PutMapping("/{id}/read")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Mark notification as read", description = "Marks a specific notification as read")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notification marked as read")
        })
        public ResponseEntity<Map<String, String>> markAsRead(
                        @Parameter(description = "ID of the notification") @PathVariable Long id) {
                notificationService.markAsRead(id);
                return ResponseEntity.ok(Map.of("message", "Notifica segnata come letta"));
        }

        @PutMapping("/read-all")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Mark all notifications as read", description = "Marks all unread notifications as read for the authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "All notifications marked as read")
        })
        public ResponseEntity<Map<String, String>> markAllAsRead(Authentication authentication) {
                Long userId = getUserIdFromAuth(authentication);
                notificationService.markAllAsRead(userId);
                return ResponseEntity.ok(Map.of("message", "Tutte le notifiche segnate come lette"));
        }

        @PostMapping("/device-token")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Register device token", description = "Registers a device token for push notifications")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Device token registered successfully")
        })
        public ResponseEntity<Map<String, String>> registerDeviceToken(
                        Authentication authentication,
                        @RequestBody Map<String, String> payload) {
                Long userId = getUserIdFromAuth(authentication);
                String token = payload.get("token");
                String platform = payload.get("platform");
                notificationService.registerDeviceToken(userId, token, platform);
                return ResponseEntity.ok(Map.of("message", "Device token registrato"));
        }

        @DeleteMapping("/device-token")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Unregister device token", description = "Unregisters a device token")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Device token unregistered successfully")
        })
        public ResponseEntity<Map<String, String>> unregisterDeviceToken(@RequestParam String token) {
                notificationService.unregisterDeviceToken(token);
                return ResponseEntity.ok(Map.of("message", "Device token rimosso"));
        }

        @PostMapping("/test-push")
        @PreAuthorize("isAuthenticated()")
        @Operation(summary = "Send test push notification", description = "Sends a test push notification to the authenticated user")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Test notification sent successfully")
        })
        public ResponseEntity<Map<String, String>> sendTestPushNotification(Authentication authentication) {
                Long userId = getUserIdFromAuth(authentication);
                notificationService.sendPushNotification(userId, "Notifica di Test 🔔",
                                "Questa è una notifica di test da TCG Arena! Se la vedi, tutto funziona correttamente.");
                return ResponseEntity.ok(Map.of("message", "Notifica di test inviata"));
        }

        @PostMapping("/shop/{shopId}/broadcast")
        @PreAuthorize("hasRole('MERCHANT')")
        @Operation(summary = "Send notification to shop subscribers", description = "Sends a notification to all subscribers of a shop (merchant only)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Notification sent to subscribers"),
                        @ApiResponse(responseCode = "403", description = "Not authorized to send notifications for this shop")
        })
        public ResponseEntity<Map<String, String>> sendShopNotification(
                        @Parameter(description = "ID of the shop") @PathVariable Long shopId,
                        @RequestBody Map<String, String> payload) {
                String title = payload.get("title");
                String message = payload.get("message");

                // TODO: Verify merchant owns this shop

                notificationService.sendNotificationToShopSubscribers(shopId, title, message);
                return ResponseEntity.ok(Map.of("message", "Notifica inviata ai subscriber"));
        }

        @PostMapping("/admin/clean-invalid-tokens")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Clean invalid device tokens", description = "Removes invalid FCM tokens from database (admin only)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Invalid tokens cleaned successfully")
        })
        public ResponseEntity<Map<String, Object>> cleanInvalidTokens() {
                int removedCount = notificationService.cleanInvalidTokens();
                return ResponseEntity.ok(Map.of(
                                "message", "Token non validi rimossi",
                                "removedTokens", removedCount));
        }

        @GetMapping("/admin/token-statistics")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Get device token statistics", description = "Returns statistics about registered device tokens (admin only)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
        })
        public ResponseEntity<Map<String, Object>> getTokenStatistics() {
                Map<String, Object> stats = notificationService.getTokenStatistics();
                return ResponseEntity.ok(stats);
        }

        @GetMapping("/admin/firebase-status")
        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Check Firebase configuration", description = "Verifies Firebase initialization and permissions (admin only)")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Firebase status retrieved successfully")
        })
        public ResponseEntity<Map<String, Object>> checkFirebaseStatus() {
                Map<String, Object> status = firebaseMessagingService.verifyConfiguration();
                return ResponseEntity.ok(status);
        }
}