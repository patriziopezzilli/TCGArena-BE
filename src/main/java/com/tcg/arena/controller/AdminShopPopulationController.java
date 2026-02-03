package com.tcg.arena.controller;

import com.tcg.arena.service.GooglePlacesService;
import com.tcg.arena.service.HerePlacesService;
import com.tcg.arena.service.OpenStreetMapService;
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

    @Autowired
    private OpenStreetMapService openStreetMapService;

    @Autowired
    private HerePlacesService herePlacesService;

    @Value("${app.shop.population.secret.key:}")
    private String secretKey;

    /**
     * Populate shops from Google Places API
     * Public endpoint but requires secret key for security
     * 
     * @param dryRun           if true, only simulates the operation without
     *                         inserting data
     * @param maxRequests      maximum API requests (default: 950, to stay under
     *                         1000 free tier)
     * @param skipPlaceDetails if true, skips Place Details calls to save quota
     *                         (faster but less data)
     * @param apiKey           secret key for authentication (configured in
     *                         application.properties)
     * @return Summary of the operation
     */
    @PostMapping("/populate-from-google")
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
                                "message", "Invalid or missing API key. This endpoint requires authentication."));
            }
        }

        try {
            Map<String, Object> result = googlePlacesService.populateShopsFromGooglePlaces(
                    dryRun, maxRequests, skipPlaceDetails);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", e.getMessage(),
                            "hint", "Configure google.places.api.key in application.properties"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Failed to populate shops: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint to verify API configuration
     */
    @GetMapping("/google-places-status")
    public ResponseEntity<Map<String, Object>> checkGooglePlacesStatus() {
        return ResponseEntity.ok(Map.of(
                "message", "Service is available",
                "endpoint", "POST /api/admin/shops/populate-from-google",
                "hint", "Use ?dryRun=true to test without inserting data",
                "authRequired", secretKey != null && !secretKey.isEmpty()));
    }

    /**
     * Populate shops from OpenStreetMap Overpass API (FREE - NO LIMITS)
     * This is a free alternative to Google Places API.
     * 
     * @param dryRun if true, only simulates the operation without inserting data
     * @param apiKey secret key for authentication
     * @return Summary of the operation
     */
    @PostMapping("/populate-from-osm")
    public ResponseEntity<Map<String, Object>> populateShopsFromOpenStreetMap(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) String apiKey) {

        // Check secret key for security (if configured)
        if (secretKey != null && !secretKey.isEmpty()) {
            if (apiKey == null || !apiKey.equals(secretKey)) {
                return ResponseEntity.status(403).body(
                        Map.of(
                                "error", "Forbidden",
                                "message", "Invalid or missing API key. This endpoint requires authentication."));
            }
        }

        try {
            Map<String, Object> result = openStreetMapService.populateShopsFromOpenStreetMap(dryRun);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Failed to populate shops from OpenStreetMap: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for OpenStreetMap service
     */
    @GetMapping("/osm-status")
    public ResponseEntity<Map<String, Object>> checkOsmStatus() {
        return ResponseEntity.ok(Map.of(
                "message", "OpenStreetMap service is available",
                "endpoint", "POST /api/admin/shops/populate-from-osm",
                "hint", "Use ?dryRun=true to test without inserting data",
                "cost", "FREE - No API limits!",
                "authRequired", secretKey != null && !secretKey.isEmpty()));
    }

    /**
     * Populate shops from HERE Places API (STABLE - 250k FREE/MONTH)
     * Covers major worldwide cities across all continents
     * 
     * @param dryRun if true, only simulates the operation without inserting data
     * @param apiKey secret key for authentication
     * @return Summary of the operation
     */
    @PostMapping("/populate-from-here")
    public ResponseEntity<Map<String, Object>> populateShopsFromHere(
            @RequestParam(defaultValue = "true") boolean dryRun,
            @RequestParam(required = false) String apiKey) {

        // Check secret key for security (if configured)
        if (secretKey != null && !secretKey.isEmpty()) {
            if (apiKey == null || !apiKey.equals(secretKey)) {
                return ResponseEntity.status(403).body(
                        Map.of(
                                "error", "Forbidden",
                                "message", "Invalid or missing API key. This endpoint requires authentication."));
            }
        }

        try {
            Map<String, Object> result = herePlacesService.populateShopsFromHere(dryRun);
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(
                    Map.of(
                            "error", e.getMessage(),
                            "hint", "Configure here.api.key in application.properties"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of(
                            "error", "Failed to populate shops from HERE Places: " + e.getMessage()));
        }
    }

    /**
     * Health check endpoint for HERE Places service
     */
    @GetMapping("/here-status")
    public ResponseEntity<Map<String, Object>> checkHereStatus() {
        return ResponseEntity.ok(Map.of(
                "message", "HERE Places service is available",
                "endpoint", "POST /api/admin/shops/populate-from-here",
                "hint", "Use ?dryRun=true to test without inserting data",
                "cost", "FREE 250k calls/month",
                "authRequired", secretKey != null && !secretKey.isEmpty()));
    }

    /**
     * Activate ALL shops in the database (admin utility)
     */
    @PostMapping("/activate-all")
    public ResponseEntity<Map<String, Object>> activateAllShops(
            @RequestParam(required = false) String apiKey) {

        // Check secret key
        if (secretKey != null && !secretKey.isEmpty()) {
            if (apiKey == null || !apiKey.equals(secretKey)) {
                return ResponseEntity.status(403).body(
                        Map.of("error", "Forbidden", "message", "Invalid API key"));
            }
        }

        try {
            herePlacesService.activateAllShops();
            return ResponseEntity.ok(Map.of("message", "All shops have been set to active=true"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Failed to activate shops: " + e.getMessage()));
        }
    }
}
