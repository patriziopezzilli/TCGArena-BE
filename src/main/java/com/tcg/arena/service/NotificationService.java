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
        // Create in-app notification
        createNotification(userId, title, message, "push");

        // Send push notification to all user's registered devices
        List<DeviceToken> deviceTokens = getUserDeviceTokens(userId);
        for (DeviceToken deviceToken : deviceTokens) {
            try {
                firebaseMessagingService.sendPushNotification(deviceToken.getToken(), title, message);
            } catch (Exception e) {
                logger.error("Failed to send push notification to device {}: {}", deviceToken.getToken(),
                        e.getMessage());
            }
        }
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
                } catch (Exception e) {
                    logger.error("Failed to send push notification to device {}: {}", deviceToken.getToken(),
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
        String title = "Prenotazione Confermata ✓";
        String message = "La tua prenotazione per " + cardName + " è stata validata da " + shopName;
        sendPushNotification(userId, title, message);
        logger.info("Sent reservation validated notification to user {}", userId);
    }

    /**
     * Notifica quando una prenotazione sta per scadere (30 min prima)
     */
    public void sendReservationExpiringNotification(Long userId, String cardName, String shopName) {
        String title = "Prenotazione in Scadenza ⏰";
        String message = "La tua prenotazione per " + cardName + " presso " + shopName + " scade tra 30 minuti!";
        sendPushNotification(userId, title, message);
        logger.info("Sent reservation expiring notification to user {}", userId);
    }

    // ========== RICHIESTE ==========

    /**
     * Notifica quando un negozio risponde a una richiesta
     */
    public void sendRequestReplyNotification(Long userId, String shopName, String requestTitle) {
        String title = "Nuova Risposta 💬";
        String message = shopName + " ha risposto alla tua richiesta: \"" + requestTitle + "\"";
        sendPushNotification(userId, title, message);
        logger.info("Sent request reply notification to user {}", userId);
    }

    /**
     * Notifica quando lo stato di una richiesta cambia
     */
    public void sendRequestStatusChangedNotification(Long userId, String requestTitle, String newStatus) {
        String title = "Aggiornamento Richiesta";
        String message = "La tua richiesta \"" + requestTitle + "\" è ora: " + newStatus;
        sendPushNotification(userId, title, message);
        logger.info("Sent request status changed notification to user {}", userId);
    }

    // ========== TORNEI ==========

    /**
     * Notifica quando un torneo sta per iniziare (15 min prima)
     */
    public void sendTournamentStartingNotification(Long userId, String tournamentTitle, String shopName) {
        String title = "Il Torneo Sta Per Iniziare! 🎮";
        String message = tournamentTitle + " inizia tra 15 minuti presso " + shopName;
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament starting notification to user {}", userId);
    }

    /**
     * Notifica quando un torneo è iniziato
     */
    public void sendTournamentStartedNotification(Long userId, String tournamentTitle) {
        String title = "Torneo Iniziato! 🎯";
        String message = "Il torneo " + tournamentTitle + " è iniziato. Buona fortuna!";
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament started notification to user {}", userId);
    }

    /**
     * Notifica quando un torneo è concluso con il piazzamento
     */
    public void sendTournamentCompletedNotification(Long userId, String tournamentTitle, int placement,
            int pointsAwarded) {
        String title = "Torneo Concluso 🏆";
        String placementText = getPlacementText(placement);
        String message = "Hai ottenuto il " + placementText + " posto in " + tournamentTitle + "! +" + pointsAwarded
                + " punti";
        sendPushNotification(userId, title, message);
        logger.info("Sent tournament completed notification to user {} with placement {}", userId, placement);
    }

    /**
     * Notifica quando il check-in è disponibile
     */
    public void sendTournamentCheckInNotification(Long userId, String tournamentTitle) {
        String title = "Check-in Disponibile ✅";
        String message = "Il check-in per " + tournamentTitle + " è ora disponibile!";
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
        String title = "Reward Riscattato! 🎁";
        String message = "Hai riscattato " + rewardName + ". Mostra il codice al negozio per ritirarlo.";
        sendPushNotification(userId, title, message);
        logger.info("Sent reward redeemed notification to user {}", userId);
    }

    /**
     * Notifica quando un utente sale di livello
     */
    public void sendLevelUpNotification(Long userId, int newLevel) {
        String title = "Level Up! 🎉";
        String message = "Congratulazioni! Sei salito al livello " + newLevel + "!";
        sendPushNotification(userId, title, message);
        logger.info("Sent level up notification to user {} - new level {}", userId, newLevel);
    }

    // ========== HELPER METHODS ==========

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
}