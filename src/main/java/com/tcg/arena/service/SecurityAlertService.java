package com.tcg.arena.service;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserLoginHistory;
import com.tcg.arena.repository.UserEmailPreferencesRepository;
import com.tcg.arena.repository.UserLoginHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class SecurityAlertService {

    private static final Logger logger = LoggerFactory.getLogger(SecurityAlertService.class);
    
    private final UserLoginHistoryRepository loginHistoryRepository;
    private final UserEmailPreferencesRepository preferencesRepository;
    private final EmailService emailService;
    private final WebClient webClient;

    public SecurityAlertService(UserLoginHistoryRepository loginHistoryRepository,
                                UserEmailPreferencesRepository preferencesRepository,
                                EmailService emailService) {
        this.loginHistoryRepository = loginHistoryRepository;
        this.preferencesRepository = preferencesRepository;
        this.emailService = emailService;
        this.webClient = WebClient.builder().build();
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
        String ipAddress;
        
        if (xfHeader != null && !xfHeader.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            ipAddress = xfHeader.split(",")[0].trim();
        } else {
            ipAddress = request.getRemoteAddr();
        }
        
        // Handle localhost/private IPs
        if (ipAddress == null || ipAddress.isEmpty() || 
            ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1") ||
            ipAddress.equals("::1") || ipAddress.equals("0.0.0.0")) {
            return "127.0.0.1"; // Default to localhost for development
        }
        
        return ipAddress;
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
     * Get location from IP (integrates with IP geolocation service)
     */
    private String getLocationFromIP(String ipAddress) {
        if (ipAddress == null || ipAddress.equals("0.0.0.0") || ipAddress.equals("127.0.0.1") || ipAddress.equals("localhost")) {
            return "Sconosciuta";
        }
        
        try {
            IpApiResponse response = webClient.get()
                    .uri("http://ip-api.com/json/" + ipAddress)
                    .retrieve()
                    .bodyToMono(IpApiResponse.class)
                    .block();
            
            if (response != null && "success".equals(response.status)) {
                StringBuilder location = new StringBuilder();
                if (response.city != null && !response.city.isEmpty()) {
                    location.append(response.city);
                }
                if (response.regionName != null && !response.regionName.isEmpty()) {
                    if (location.length() > 0) location.append(", ");
                    location.append(response.regionName);
                }
                if (response.country != null && !response.country.isEmpty()) {
                    if (location.length() > 0) location.append(", ");
                    location.append(response.country);
                }
                return location.length() > 0 ? location.toString() : "Sconosciuta";
            }
        } catch (Exception e) {
            logger.warn("Failed to get location for IP {}: {}", ipAddress, e.getMessage());
        }
        
        return "Sconosciuta";
    }

    /**
     * DTO for IP-API.com response
     */
    private static class IpApiResponse {
        public String status;
        public String country;
        public String countryCode;
        public String region;
        public String regionName;
        public String city;
        public String zip;
        public double lat;
        public double lon;
        public String timezone;
        public String isp;
        public String org;
        public String as;
        public String query;
    }
}
