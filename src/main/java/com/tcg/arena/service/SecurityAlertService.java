package com.tcg.arena.service;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserLoginHistory;
import com.tcg.arena.repository.UserEmailPreferencesRepository;
import com.tcg.arena.repository.UserLoginHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);
    
    private final UserLoginHistoryRepository loginHistoryRepository;
    private final UserEmailPreferencesRepository preferencesRepository;
    private final EmailService emailService;

    public SecurityAlertService(UserLoginHistoryRepository loginHistoryRepository,
                                UserEmailPreferencesRepository preferencesRepository,
                                EmailService emailService) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.preferencesRepository = preferencesRepository;
        this.emailService = emailService;
    }

    /**
     * Track user login and send alert if from new device
     */
    @Transactional
    public void trackLoginAndNotify(User user, HttpServletRequest request) {
        String ipAddress = getClientIP(request);
        String userAgent = request.getHeader("User-Agent");
        String deviceFingerprint = generateDeviceFingerprint(ipAddress, userAgent);
        
        // Check if this is a new device
        boolean isNewDevice = !loginHistoryRepository.existsByUserAndDeviceFingerprint(user, deviceFingerprint);
        
        // Create login history record
        UserLoginHistory loginHistory = new UserLoginHistory();
        loginHistory.setUser(user);
        loginHistory.setIpAddress(ipAddress);
        loginHistory.setUserAgent(userAgent);
        loginHistory.setDeviceFingerprint(deviceFingerprint);
        loginHistory.setLocation(getLocationFromIP(ipAddress));
        loginHistory.setNewDevice(isNewDevice);
        loginHistory.setLoginTime(LocalDateTime.now());
        
        loginHistoryRepository.save(loginHistory);
        
        // Send security alert if new device and user has security alerts enabled
        if (isNewDevice && shouldSendSecurityAlert(user)) {
            sendSecurityAlert(user, loginHistory);
        }
    }

    /**
     * Send security alert email
     */
    private void sendSecurityAlert(User user, UserLoginHistory login) {
        try {
            String loginTime = login.getLoginTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            String deviceInfo = extractDeviceInfo(login.getUserAgent());
            String location = login.getLocation() != null ? login.getLocation() : "Sconosciuta";
            
            emailService.sendSecurityAlert(
                user.getEmail(),
                user.getUsername(),
                loginTime,
                deviceInfo,
                location,
                login.getIpAddress()
            );
            
            logger.info("Security alert sent to user: {}", user.getUsername());
        } catch (Exception e) {
            logger.error("Failed to send security alert to user: {}", user.getUsername(), e);
        }
    }

    /**
     * Check if user wants to receive security alerts
     */
    private boolean shouldSendSecurityAlert(User user) {
        return preferencesRepository.findByUser(user)
                .map(prefs -> prefs.getSecurityAlerts())
                .orElse(true); // Default to true
    }

    /**
     * Get client IP address from request
     */
    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    /**
     * Generate device fingerprint from IP and User-Agent
     */
    private String generateDeviceFingerprint(String ipAddress, String userAgent) {
        return String.valueOf((ipAddress + userAgent).hashCode());
    }

    /**
     * Extract device info from User-Agent string
     */
    private String extractDeviceInfo(String userAgent) {
        if (userAgent == null) return "Dispositivo Sconosciuto";
        
        // Simple device detection
        if (userAgent.contains("iPhone")) return "iPhone";
        if (userAgent.contains("iPad")) return "iPad";
        if (userAgent.contains("Android")) return "Android";
        if (userAgent.contains("Windows")) return "Windows PC";
        if (userAgent.contains("Mac")) return "Mac";
        if (userAgent.contains("Linux")) return "Linux";
        
        return "Dispositivo Sconosciuto";
    }

    /**
     * Get location from IP (placeholder - integrate with IP geolocation service)
     */
    private String getLocationFromIP(String ipAddress) {
        // TODO: Integrate with IP geolocation service (e.g., ipapi.co, ip-api.com)
        // For now, return null
        return null;
    }
}
