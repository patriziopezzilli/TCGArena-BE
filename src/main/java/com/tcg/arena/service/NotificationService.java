package com.tcg.arena.service;

import com.tcg.arena.model.DeviceToken;
import com.tcg.arena.model.Notification;
import com.tcg.arena.model.ShopSubscription;
import com.tcg.arena.repository.DeviceTokenRepository;
import com.tcg.arena.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private FirebaseMessagingService firebaseMessagingService;

    @Autowired
    private ShopSubscriptionService shopSubscriptionService;

    @Autowired
    private com.tcg.arena.repository.UserRepository userRepository; // Need to fetch user locale

    @Autowired
    private org.springframework.context.MessageSource messageSource;

    /**
     * Helper to get localized message
     */
    private String getMessage(String key, Object[] args, Long userId) {
        String localeStr = "it";
        try {
            localeStr = userRepository.findById(userId)
                    .map(com.tcg.arena.model.User::getLocale)
                    .orElse("it");
        } catch (Exception e) {
            logger.warn("Failed to fetch locale for user {}, defaulting to it", userId);
        }

        java.util.Locale locale = java.util.Locale.ITALIAN;
        if (localeStr != null) {
            try {
                locale = new java.util.Locale(localeStr);
            } catch (Exception e) {
                // ignore
            }
        }
        return messageSource.getMessage(key, args, locale);
    }

    public Notification createNotification(Long userId, String title, String message, String type) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setCreatedAt(LocalDateTime.now());
        return notificationRepository.save(notification);
    }

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setIsRead(true);
            notificationRepository.save(notification);
        });
    }

    public void registerDeviceToken(Long userId, String token, String platform) {
        // Remove existing token if any
        deviceTokenRepository.findByToken(token).ifPresent(deviceTokenRepository::delete);

        DeviceToken deviceToken = new DeviceToken();
        deviceToken.setUserId(userId);
        deviceToken.setToken(token);
        deviceToken.setPlatform(platform);
        deviceToken.setRegisteredAt(LocalDateTime.now());
        deviceTokenRepository.save(deviceToken);
    }

    public void unregisterDeviceToken(String token) {
        deviceTokenRepository.findByToken(token).ifPresent(deviceTokenRepository::delete);
    }

    public List<DeviceToken> getUserDeviceTokens(Long userId) {
        return deviceTokenRepository.findByUserId(userId);
    }

    // Send push notification to user using Firebase Cloud Messaging
    public void sendPushNotification(Long userId, String title, String message) {
        sendPushNotification(userId, title, message, null);
    }

    // Send push notification to user using Firebase Cloud Messaging with data
    // payload
    public void sendPushNotification(Long userId, String title, String message, java.util.Map<String, String> data) {
        // Create in-app notification
        createNotification(userId, title, message, "push");

        // Send push notification to all user's registered devices
        List<DeviceToken> deviceTokens = getUserDeviceTokens(userId);
        for (DeviceToken deviceToken : deviceTokens) {
            try {
                firebaseMessagingService.sendPushNotification(deviceToken.getToken(), title, message, data);
            } catch (FirebaseMessagingService.InvalidTokenException e) {
                // Remove invalid token from database
                logger.info("🗑️  Removing invalid device token for user {}", userId);
                deviceTokenRepository.delete(deviceToken);
            } catch (Exception e) {
                logger.error("Failed to send push notification to device {}: {}",
                        deviceToken.getToken().substring(0, Math.min(20, deviceToken.getToken().length())),
                        e.getMessage());
            }
        }
    }

    public void sendChatNotification(Long userId, String title, String message, Long conversationId) {
        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("type", "CHAT");
        data.put("conversationId", String.valueOf(conversationId));

        // Include click_action for Flutter/Android compatibility if needed
        data.put("click_action", "FLUTTER_NOTIFICATION_CLICK");

        sendPushNotification(userId, title, message, data);
    }

    // Send notification to all subscribers of a shop
    public void sendNotificationToShopSubscribers(Long shopId, String title, String message) {
        List<ShopSubscription> subscriptions = shopSubscriptionService.getShopSubscribers(shopId);

        for (ShopSubscription subscription : subscriptions) {
            // Create in-app notification for each subscriber
            createNotification(subscription.getUserId(), title, message, "shop_broadcast");

            // Send push notification to all devices of each subscriber
            List<DeviceToken> deviceTokens = getUserDeviceTokens(subscription.getUserId());
            for (DeviceToken deviceToken : deviceTokens) {
                try {
                    firebaseMessagingService.sendPushNotification(deviceToken.getToken(), title, message);
                } catch (FirebaseMessagingService.InvalidTokenException e) {
                    logger.info("🗑️  Removing invalid device token for user {}", subscription.getUserId());
                    deviceTokenRepository.delete(deviceToken);
                } catch (Exception e) {
                    logger.error("Failed to send push notification to device {}: {}",
                            deviceToken.getToken().substring(0, Math.min(20, deviceToken.getToken().length())),
                            e.getMessage());
                }
            }
        }
    }

    // ========== PRENOTAZIONI ==========

    /**
     * Notifica quando una prenotazione viene validata dal negozio
     */
    public void sendReservationValidatedNotification(Long userId, String cardName, String shopName) {
        String title = getMessage("notification.reservation.validated.title", null, userId);
        String message = getMessage("notification.reservation.validated.message", new Object[] { cardName, shopName },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent reservation validated notification to user {}", userId);
    }

    /**
     * Notifica quando una prenotazione sta per scadere (30 min prima)
     */
    public void sendReservationExpiringNotification(Long userId, String cardName, String shopName) {
        String title = getMessage("notification.reservation.expiring.title", null, userId);
        String message = getMessage("notification.reservation.expiring.message", new Object[] { cardName, shopName },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent reservation expiring notification to user {}", userId);
    }

    // ========== RICHIESTE ==========

    /**
     * Notifica quando un negozio risponde a una richiesta
     */
    public void sendRequestReplyNotification(Long userId, String shopName, String requestTitle) {
        String title = getMessage("notification.request.reply.title", null, userId);
        String message = getMessage("notification.request.reply.message", new Object[] { shopName, requestTitle },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent request reply notification to user {}", userId);
    }

    /**
     * Notifica quando lo stato di una richiesta cambia
     */
    public void sendRequestStatusChangedNotification(Long userId, String requestTitle, String newStatus) {
        String title = getMessage("notification.request.status.title", null, userId);
        String message = getMessage("notification.request.status.message", new Object[] { requestTitle, newStatus },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent request status changed notification to user {}", userId);
    }

    // ========== TORNEI ==========

    /**
     * Notifica quando un torneo sta per iniziare (15 min prima)
     */
    public void sendTournamentStartingNotification(Long userId, String tournamentTitle, String shopName) {
        String title = getMessage("notification.tournament.starting.title", null, userId);
        String message = getMessage("notification.tournament.starting.message",
                new Object[] { tournamentTitle, shopName }, userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament starting notification to user {}", userId);
    }

    /**
     * Notifica quando un torneo è iniziato
     */
    public void sendTournamentStartedNotification(Long userId, String tournamentTitle) {
        String title = getMessage("notification.tournament.started.title", null, userId);
        String message = getMessage("notification.tournament.started.message", new Object[] { tournamentTitle },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament started notification to user {}", userId);
    }

    /**
     * Notifica quando un torneo è concluso con il piazzamento
     */
    public void sendTournamentCompletedNotification(Long userId, String tournamentTitle, int placement,
            int pointsAwarded) {
        String title = getMessage("notification.tournament.completed.title", null, userId);
        String placementText = getPlacementText(placement); // This could also be localized if needed
        String message = getMessage("notification.tournament.completed.message",
                new Object[] { placementText, tournamentTitle, pointsAwarded }, userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament completed notification to user {} with placement {}", userId, placement);
    }

    /**
     * Notifica quando il check-in è disponibile
     */
    public void sendTournamentCheckInNotification(Long userId, String tournamentTitle) {
        String title = getMessage("notification.tournament.checkin.title", null, userId);
        String message = getMessage("notification.tournament.checkin.message", new Object[] { tournamentTitle },
                userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament check-in notification to user {}", userId);
    }

    /**
     * Notifica quando un utente viene rimosso da un torneo
     */
    public void sendTournamentRemovedNotification(Long userId, String tournamentTitle) {
        String title = "Aggiornamento Iscrizione";
        String message = "Sei stato rimosso dal torneo " + tournamentTitle
                + ". Contatta l'organizzatore per informazioni.";
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament removed notification to user {}", userId);
    }

    // ========== EVENTI E NEWS ==========

    /**
     * Notifica ai subscriber quando un negozio crea un nuovo evento
     */
    public void sendNewEventNotification(Long shopId, String shopName, String eventTitle, String eventDate) {
        String title = "Nuovo Evento 📅";
        String message = shopName + " ha pubblicato un nuovo evento: " + eventTitle + " - " + eventDate;
        sendNotificationToShopSubscribers(shopId, title, message);
        logger.info("Sent new event notification to subscribers of shop {}", shopId);
    }

    /**
     * Notifica ai subscriber quando un negozio pubblica una notizia
     */
    public void sendShopNewsNotification(Long shopId, String shopName, String newsTitle) {
        String title = "Novità da " + shopName + " 📢";
        String message = newsTitle;
        sendNotificationToShopSubscribers(shopId, title, message);
        logger.info("Sent shop news notification to subscribers of shop {}", shopId);
    }

    // ========== REWARDS E LIVELLI ==========

    /**
     * Notifica quando un utente riscatta un reward
     */
    public void sendRewardRedeemedNotification(Long userId, String rewardName) {
        String title = getMessage("notification.reward.redeemed.title", null, userId);
        String message = getMessage("notification.reward.redeemed.message", new Object[] { rewardName }, userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent reward redeemed notification to user {}", userId);
    }

    /**
     * Notifica quando il merchant conferma un reward (fulfilled)
     */
    public void sendRewardFulfilledNotification(Long userId, String rewardName, String shopName) {
        String title = getMessage("notification.reward.fulfilled.title", null, userId);
        String message = getMessage("notification.reward.fulfilled.message", new Object[] { shopName, rewardName },
                userId);

        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("type", "reward_fulfilled");

        sendPushNotification(userId, title, message, data);
        logger.info("Sent reward fulfilled notification to user {} for reward '{}'", userId, rewardName);
    }

    /**
     * Notifica quando il merchant annulla un reward
     */
    public void sendRewardCancelledNotification(Long userId, String rewardName, String shopName, int pointsRefunded) {
        String title = getMessage("notification.reward.cancelled.title", null, userId);
        String message = getMessage("notification.reward.cancelled.message",
                new Object[] { rewardName, shopName, pointsRefunded }, userId);

        java.util.Map<String, String> data = new java.util.HashMap<>();
        data.put("type", "reward_cancelled");

        sendPushNotification(userId, title, message, data);
        logger.info("Sent reward cancelled notification to user {} for reward '{}' - {} points refunded", userId,
                rewardName, pointsRefunded);
    }

    /**
     * Notifica quando un utente sale di livello
     */
    public void sendLevelUpNotification(Long userId, int newLevel) {
        String title = getMessage("notification.levelup.title", null, userId);
        String message = getMessage("notification.levelup.message", new Object[] { newLevel }, userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent level up notification to user {} - new level {}", userId, newLevel);
    }

    // ========== HELPER METHODS ==========

    /**
     * Notifica quando qualcuno mette like a un pull
     */
    public void sendPullLikeNotification(Long userId, String likerName, String tcgName) {
        String title = getMessage("notification.like.pull.title", null, userId);
        String message = getMessage("notification.like.pull.message", new Object[] { likerName, tcgName }, userId);
        sendPushNotification(userId, title, message);
        logger.info("Sent pull like notification to user {}", userId);
    }

    private String getPlacementText(int placement) {
        return switch (placement) {
            case 1 -> "1°";
            case 2 -> "2°";
            case 3 -> "3°";
            default -> placement + "°";
        };
    }

    // ========== ADMIN BROADCAST ==========

    /**
     * Send broadcast notification to ALL users with registered device tokens
     * 
     * @param title   Notification title
     * @param message Notification message
     * @return Number of users notified
     */
    public int sendBroadcastNotification(String title, String message) {
        // Get all unique user IDs from device tokens
        List<DeviceToken> allDeviceTokens = deviceTokenRepository.findAll();

        // Count unique users
        java.util.Set<Long> notifiedUsers = new java.util.HashSet<>();
        int successCount = 0;
        int failCount = 0;

        logger.info("📢 Starting broadcast notification to {} device tokens", allDeviceTokens.size());

        for (DeviceToken deviceToken : allDeviceTokens) {
            Long userId = deviceToken.getUserId();

            // Create in-app notification only once per user
            if (!notifiedUsers.contains(userId)) {
                createNotification(userId, title, message, "admin_broadcast");
                notifiedUsers.add(userId);
            }

            // Send push notification to each device
            try {
                firebaseMessagingService.sendPushNotification(deviceToken.getToken(), title, message);
                successCount++;
            } catch (FirebaseMessagingService.InvalidTokenException e) {
                failCount++;
                logger.info("🗑️  Removing invalid device token during broadcast");
                deviceTokenRepository.delete(deviceToken);
            } catch (Exception e) {
                failCount++;
                logger.error("Failed to send broadcast to device {}: {}",
                        deviceToken.getToken().substring(0, Math.min(10, deviceToken.getToken().length())),
                        e.getMessage());
            }
        }

        logger.info("📢 Broadcast complete: {} users notified, {} push sent, {} failed",
                notifiedUsers.size(), successCount, failCount);

        return notifiedUsers.size();
    }

    /**
     * Get count of users with registered device tokens (potential broadcast
     * recipients)
     */
    public long getBroadcastRecipientsCount() {
        return deviceTokenRepository.findAll().stream()
                .map(DeviceToken::getUserId)
                .distinct()
                .count();
    }

    /**
     * Clean up invalid device tokens from database
     * Tests each token with a dummy notification and removes invalid ones
     * 
     * @return Number of tokens removed
     */
    public int cleanInvalidTokens() {
        List<DeviceToken> allTokens = deviceTokenRepository.findAll();
        int removedCount = 0;

        logger.info("🧹 Starting cleanup of {} device tokens...", allTokens.size());

        for (DeviceToken deviceToken : allTokens) {
            try {
                // Try to send a test message (dry run would be ideal but not available in all
                // Firebase versions)
                // We'll catch the error if token is invalid
                firebaseMessagingService.sendPushNotification(
                        deviceToken.getToken(),
                        "Test",
                        "Token validation");
            } catch (FirebaseMessagingService.InvalidTokenException e) {
                logger.info("🗑️  Removing invalid token for user {}: {}",
                        deviceToken.getUserId(),
                        e.getMessage());
                deviceTokenRepository.delete(deviceToken);
                removedCount++;
            } catch (Exception e) {
                logger.warn("⚠️  Error validating token for user {}: {}",
                        deviceToken.getUserId(),
                        e.getMessage());
            }
        }

        logger.info("✅ Cleanup complete: {} invalid tokens removed out of {}",
                removedCount, allTokens.size());

        return removedCount;
    }

    /**
     * Get statistics about device tokens
     */
    public java.util.Map<String, Object> getTokenStatistics() {
        List<DeviceToken> allTokens = deviceTokenRepository.findAll();

        long iosTokens = allTokens.stream()
                .filter(t -> "ios".equalsIgnoreCase(t.getPlatform()))
                .count();

        long androidTokens = allTokens.stream()
                .filter(t -> "android".equalsIgnoreCase(t.getPlatform()))
                .count();

        long uniqueUsers = allTokens.stream()
                .map(DeviceToken::getUserId)
                .distinct()
                .count();

        return java.util.Map.of(
                "totalTokens", allTokens.size(),
                "iosTokens", iosTokens,
                "androidTokens", androidTokens,
                "uniqueUsers", uniqueUsers);
    }
}