package com.tcg.arena.controller;

import com.tcg.arena.service.GooglePlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/shops")
public class AdminShopPopulationController {

    @Autowired
    private GooglePlacesService googlePlacesService;

    @Value("${app.shop.population.secret.key:}")
    private String secretKey;

    /**
     * Populate shops from Google Places API
     * Public endpoint but requires secret key for security
     * 
     * @param dryRun if true, only simulates the operation without inserting data
     * @param maxRequests maximum API requests (default: 950, to stay under 1000 free tier)
     * @param skipPlaceDetails if true, skips Place Details calls to save quota (faster but less data)
    public ResponseEntity<Map<String, Object>> populateShopsFromGoogle(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) Integer maxRequests,
            @RequestParam(defaultValue = "false") boolean skipPlaceDetails,
            @RequestParam(required = false) String apiKey) {
        
        // Check secret key for security (if configured)
        if (secretKey != null && !secretKey.isEmpty()) {
            if (apiKey == null || !apiKey.equals(secretKey)) {
                return ResponseEntity.status(403).body(
                    Map.of(
                        "error", "Forbidden",
                        "message", "Invalid or missing API key. This endpoint requires authentication."
                    )
                );
            }
        }
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) Integer maxRequests,
            @RequestParam(defaultValue = "false") boolean skipPlaceDetails,
            @RequestParam(required = false) String apiKey) {
        
        try {
            Map<String, Object> result = googlePlacesService.populateShopsFromGooglePlaces(
                dryRun, maxRequests, skipPlaceDetails
            );
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                Map.of(
                    "error", e.getMessage(),
                    "hint", "Configure google.places.api.key in application.properties"
                )configuration
     */
    @GetMapping("/google-places-status")
    public ResponseEntity<Map<String, Object>> checkGooglePlacesStatus() {
        return ResponseEntity.ok(Map.of(
            "message", "Service is available",
            "endpoint", "POST /api/admin/shops/populate-from-google",
            "hint", "Use ?dryRun=true to test without inserting data",
            "authRequired", secretKey != null && !secretKey.isEmpty()
        }
    }

    /**
     * Health check endpoint to verify API key is configured
     */
    @GetMapping("/google-places-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkGooglePlacesStatus() {
        // This will be implemented to check if API key is valid
        return ResponseEntity.ok(Map.of(
            "message", "Use POST /api/admin/shops/populate-from-google?dryRun=true to test"
        ));
    }
}
