package com.tcg.arena.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for reverse geocoding - converting coordinates to city/country
 * Uses OpenStreetMap Nominatim API (free, no API key required)
 */
@Service
public class GeocodingService {

    private static final String NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/reverse";
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Result class for reverse geocoding
     */
    public static class GeocodingResult {
        private String city;
        private String country;
        private String fullAddress;

        public GeocodingResult(String city, String country, String fullAddress) {
            this.city = city;
            this.country = country;
            this.fullAddress = fullAddress;
        }

        public String getCity() {
            return city;
        }

        public String getCountry() {
            return country;
        }

        public String getFullAddress() {
            return fullAddress;
        }
    }

    /**
     * Reverse geocode coordinates to get city and country
     * 
     * @param latitude  The latitude
     * @param longitude The longitude
     * @return GeocodingResult with city, country and full address
     */
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return new GeocodingResult("", "Italy", "");
        }

        try {
            String url = String.format("%s?lat=%f&lon=%f&format=json&addressdetails=1",
                    NOMINATIM_BASE_URL, latitude, longitude);

            // Nominatim requires a User-Agent header
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TCGArena/1.0 (contact@tcgarena.it)");
            headers.set("Accept-Language", "it");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode address = root.get("address");

                if (address != null) {
                    // Try to get city from different fields (Nominatim uses different fields based
                    // on location)
                    String city = getFirstNonNull(address,
                            "city", "town", "village", "municipality", "county");

                    // Get country
                    String country = address.has("country") ? address.get("country").asText() : "Italy";

                    // Get full display name
                    String fullAddress = root.has("display_name") ? root.get("display_name").asText() : "";

                    System.out.println("📍 Reverse geocoding: " + latitude + ", " + longitude);
                    System.out.println("   City: " + city);
                    System.out.println("   Country: " + country);

                    return new GeocodingResult(city, country, fullAddress);
                }
            }
        } catch (Exception e) {
            System.err.println("⚠️ Geocoding error: " + e.getMessage());
        }

        // Fallback if geocoding fails
        return new GeocodingResult("", "Italy", "");
    }

    /**
     * Helper to get the first non-null value from a JsonNode
     */
    private String getFirstNonNull(JsonNode node, String... fields) {
        for (String field : fields) {
            if (node.has(field) && !node.get(field).isNull()) {
                return node.get(field).asText();
            }
        }
        return "";
    }
}
