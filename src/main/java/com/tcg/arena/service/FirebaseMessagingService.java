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

    public void sendPushNotification(String deviceToken, String title, String body) {
        try {
            Notification notification = Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build();

            Message message = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(notification)
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("Successfully sent message: " + response);
        } catch (Exception e) {
            logger.error("Failed to send push notification: " + e.getMessage());
            // Fallback: could implement APNs or other push service
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
}