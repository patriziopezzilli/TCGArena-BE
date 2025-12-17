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
            // Load from project root or environment variable
            String firebaseKeyPath = System.getenv("FIREBASE_SERVICE_ACCOUNT_PATH");
            if (firebaseKeyPath == null || firebaseKeyPath.isEmpty()) {
                firebaseKeyPath = "firebase-service-account.json";
            }
            FileInputStream serviceAccount = new FileInputStream(firebaseKeyPath);

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException e) {
            logger.error("Failed to initialize Firebase: " + e.getMessage());
            // For development, we'll continue without Firebase initialization
            // In production, this should throw an exception
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