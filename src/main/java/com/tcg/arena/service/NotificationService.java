package com.tcg.arena.service;

import com.tcg.arena.model.DeviceToken;
import com.tcg.arena.model.Notification;
import com.tcg.arena.repository.DeviceTokenRepository;
import com.tcg.arena.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    @Autowired
    private FirebaseMessagingService firebaseMessagingService;

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
                System.err.println("Failed to send push notification to device " + deviceToken.getToken() + ": " + e.getMessage());
            }
        }
    }
}