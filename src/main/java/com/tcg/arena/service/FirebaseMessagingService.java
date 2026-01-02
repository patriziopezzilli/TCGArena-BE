package com.tcg.arena.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;

@Service
public class FirebaseMessagingService {

    /**
     * Exception thrown when a device token is invalid or unregistered
     */
    public static class InvalidTokenException extends Exception {
        private final String token;
        
        public InvalidTokenException(String message, String token) {
            super(message);
            this.token = token;
        }
        
        public String getToken() {
            return token;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(FirebaseMessagingService.class);

    @PostConstruct
    public void initializeFirebase() {
        try {
            // Initialize Firebase with service account key
            // Try multiple locations
            java.io.InputStream serviceAccount = null;
            String loadedFrom = "";

            // 1. Try environment variable path
            String firebaseKeyPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
            if (firebaseKeyPath != null && !firebaseKeyPath.isEmpty()) {
                java.io.File file = new java.io.File(firebaseKeyPath);
                if (file.exists()) {
                    serviceAccount = new FileInputStream(file);
                    loadedFrom = "env path: " + firebaseKeyPath;
                }
            }

            // 2. Try classpath (src/main/resources)
            if (serviceAccount == null) {
                serviceAccount = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
                if (serviceAccount != null) {
                    loadedFrom = "classpath";
                }
            }

            // 3. Try current working directory
            if (serviceAccount == null) {
                java.io.File cwdFile = new java.io.File("firebase-service-account.json");
                if (cwdFile.exists()) {
                    serviceAccount = new FileInputStream(cwdFile);
                    loadedFrom = "cwd: " + cwdFile.getAbsolutePath();
                }
            }

            // 4. Try project root (absolute path for development)
            if (serviceAccount == null) {
                java.io.File devFile = new java.io.File(
                        "/Users/patriziopezzilli/Documents/Sviluppo/TCGArena-BE/firebase-service-account.json");
                if (devFile.exists()) {
                    serviceAccount = new FileInputStream(devFile);
                    loadedFrom = "dev path";
                }
            }

            if (serviceAccount == null) {
                logger.warn("⚠️ Firebase service account not found. Push notifications will be disabled.");
                logger.warn(
                        "   Place firebase-service-account.json in src/main/resources/ or set FIREBASE_SERVICE_ACCOUNT_PATH");
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                logger.info("✅ Firebase initialized successfully from " + loadedFrom);
            }
        } catch (IOException e) {
            logger.error("❌ Failed to initialize Firebase: " + e.getMessage());
        }
    }

    /**
     * Send push notification to a device
     * @param deviceToken FCM device token
     * @param title Notification title
     * @param body Notification body
     * @throws InvalidTokenException if token is invalid or unregistered
     */
    public void sendPushNotification(String deviceToken, String title, String body) throws InvalidTokenException {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                logger.warn("Firebase not initialized, skipping notification");
                return;
            }

            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.debug("✅ Push sent to token ...{}: {}", 
                deviceToken.substring(Math.max(0, deviceToken.length() - 10)), response);
        } catch (com.google.firebase.messaging.FirebaseMessagingException e) {
            String errorCode = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "UNKNOWN";
            String tokenPreview = deviceToken.substring(0, Math.min(20, deviceToken.length())) + "...";
            
            // Check if token is invalid or unregistered
            if (errorCode.contains("UNREGISTERED") || 
                errorCode.contains("INVALID") ||
                e.getMessage().contains("not a valid FCM registration token")) {
                logger.warn("🗑️  Invalid FCM token detected: {} - Error: {}", tokenPreview, errorCode);
                throw new InvalidTokenException("Invalid or unregistered FCM token", deviceToken);
            }
            
            // Authentication errors
            if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
                logger.error("🔐 Firebase authentication failed - Check service account credentials");
            }
            
            logger.error("❌ Failed to send push to {}: {} - {}", tokenPreview, errorCode, e.getMessage());
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending push: {}", e.getMessage());
        }
    }

    public void sendPushNotificationToTopic(String topic, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setTopic(topic)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent message to topic: " + response);
        } catch (Exception e) {
            logger.error("Failed to send push notification to topic: " + e.getMessage());
        }
    }
    
    /**
     * Verify Firebase configuration and credentials
     * @return Map with configuration status
     */
    public java.util.Map<String, Object> verifyConfiguration() {
        java.util.Map<String, Object> status = new java.util.HashMap<>();
        
        try {
            // Check if Firebase is initialized
            boolean isInitialized = !FirebaseApp.getApps().isEmpty();
            status.put("initialized", isInitialized);
            
            if (isInitialized) {
                FirebaseApp app = FirebaseApp.getInstance();
                status.put("projectId", app.getOptions().getProjectId());
                status.put("status", "Firebase initialized successfully");
                
                // Try to get FirebaseMessaging instance to verify permissions
                try {
                    FirebaseMessaging.getInstance();
                    status.put("messagingAvailable", true);
                    status.put("permissions", "Service account appears to have correct permissions");
                } catch (Exception e) {
                    status.put("messagingAvailable", false);
                    status.put("error", "Cannot access Firebase Messaging: " + e.getMessage());
                    status.put("permissions", "⚠️ Service account might lack FCM permissions");
                }
            } else {
                status.put("status", "Firebase NOT initialized");
                status.put("error", "Service account file not found or invalid");
            }
            
        } catch (Exception e) {
            status.put("initialized", false);
            status.put("error", "Error checking Firebase configuration: " + e.getMessage());
        }
        
        return status;
    }
}