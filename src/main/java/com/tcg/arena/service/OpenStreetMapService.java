package com.tcg.arena.service;

import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopType;
import com.tcg.arena.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

/**
 * Service for populating shops from OpenStreetMap using Overpass API.
 * This is a FREE alternative to Google Places API with NO limits.
 */
@Service
public class OpenStreetMapService {

    private static final Logger logger = LoggerFactory.getLogger(OpenStreetMapService.class);

    @Autowired
    private ShopRepository shopRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String OVERPASS_API_URL = "https://overpass-api.de/api/interpreter";

    // Search tags for TCG-related shops
    private static final List<String[]> SEARCH_TAGS = Arrays.asList(
            new String[] { "shop", "comic" }, // Fumetterie
            new String[] { "shop", "games" }, // Negozi di giochi
            new String[] { "shop", "toys" }, // Negozi di giocattoli
            new String[] { "shop", "collector" }, // Negozi collezionismo
            new String[] { "shop", "hobby" }, // Hobby shops
            new String[] { "amenity", "game_club" }, // Club di gioco
            new String[] { "leisure", "adult_gaming_centre" } // Gaming centres
    );

    // Keywords to filter TCG-relevant shops
    private static final List<String> TCG_KEYWORDS = Arrays.asList(
            "pokemon", "pokémon", "magic", "mtg", "yugioh", "yu-gi-oh",
            "trading card", "tcg", "carte", "fumett", "comic", "game",
            "one piece", "dragon ball", "lorcana", "digimon");

    /**
     * Populate shops database with TCG stores from OpenStreetMap Overpass API.
     * This is 100% FREE with no API limits.
     * 
     * @param dryRun if true, only logs what would be inserted without saving
     * @return Summary of the operation
     */
    public Map<String, Object> populateShopsFromOpenStreetMap(boolean dryRun) {
        int totalFound = 0;
        int totalInserted = 0;
        int totalSkipped = 0;
        List<String> errors = new ArrayList<>();

        logger.info("Starting OpenStreetMap shop population for ITALY (dryRun: {})", dryRun);

        try {
            // Build Overpass query for Italy
            String query = buildOverpassQuery();
            logger.info("Executing Overpass query...");

            // Execute query
            String response = executeOverpassQuery(query);

            if (response == null) {
                errors.add("Failed to get response from Overpass API");
                return buildSummary(totalFound, totalInserted, totalSkipped, errors, dryRun);
            }

            // Parse response
            JsonNode root = objectMapper.readTree(response);
            JsonNode elements = root.get("elements");

            if (elements != null && elements.isArray()) {
                logger.info("Found {} elements from OpenStreetMap", elements.size());

                for (JsonNode element : elements) {
                    totalFound++;

                    try {
                        Shop shop = parseShopFromOsmElement(element);

                        if (shop == null) {
                            totalSkipped++;
                            continue;
                        }

                        // Check if shop already exists
                        if (shopExists(shop)) {
                            logger.debug("Shop already exists: {}", shop.getName());
                            totalSkipped++;
                            continue;
                        }

                        if (!dryRun) {
                            shopRepository.save(shop);
                            logger.info("Inserted shop: {} - {}", shop.getName(), shop.getAddress());
                        } else {
                            logger.info("Would insert shop: {} - {}", shop.getName(), shop.getAddress());
                        }
                        totalInserted++;

                    } catch (Exception e) {
                        logger.warn("Error processing element: {}", e.getMessage());
                        totalSkipped++;
                    }
                }
            }

        } catch (Exception e) {
            String errorMsg = "Error querying OpenStreetMap: " + e.getMessage();
            logger.error(errorMsg, e);
            errors.add(errorMsg);
        }

        logger.info("Population completed. Found: {}, Inserted: {}, Skipped: {}",
                totalFound, totalInserted, totalSkipped);

        return buildSummary(totalFound, totalInserted, totalSkipped, errors, dryRun);
    }

    /**
     * Build Overpass QL query to find TCG-related shops in Italy
     */
    private String buildOverpassQuery() {
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:300];");
        query.append("area[\"name\"=\"Italia\"][\"admin_level\"=\"2\"]->.italy;");
        query.append("(");

        // Add all search tags for nodes and ways
        for (String[] tag : SEARCH_TAGS) {
            String key = tag[0];
            String value = tag[1];
            query.append(String.format("node[\"%s\"=\"%s\"](area.italy);", key, value));
            query.append(String.format("way[\"%s\"=\"%s\"](area.italy);", key, value));
        }

        query.append(");");
        query.append("out body center;");

        return query.toString();
    }

    /**
     * Execute Overpass API query using POST (recommended method)
     */
    private String executeOverpassQuery(String query) {
        try {
            logger.debug("Executing Overpass query: {}", query);

            // Use POST with form data - this is the recommended approach for Overpass API
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

            org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("data", query);

            org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(
                    body, headers);

            return restTemplate.postForObject(OVERPASS_API_URL, request, String.class);

        } catch (Exception e) {
            logger.error("Error executing Overpass query: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse OpenStreetMap element into Shop entity
     */
    private Shop parseShopFromOsmElement(JsonNode element) {
        try {
            JsonNode tags = element.get("tags");
            if (tags == null) {
                return null;
            }

            // Get name - required
            String name = tags.has("name") ? tags.get("name").asText() : null;
            if (name == null || name.isEmpty()) {
                return null;
            }

            // Check if it's likely a TCG shop by name
            String lowerName = name.toLowerCase();
            boolean seemsTcgRelated = TCG_KEYWORDS.stream()
                    .anyMatch(keyword -> lowerName.contains(keyword));

            // Also check if it's a comic/game shop (likely to have TCG)
            String shopType = tags.has("shop") ? tags.get("shop").asText() : "";
            boolean isGameShop = "comic".equals(shopType) || "games".equals(shopType) ||
                    "toys".equals(shopType) || "hobby".equals(shopType) ||
                    "collector".equals(shopType);

            if (!seemsTcgRelated && !isGameShop) {
                return null;
            }

            Shop shop = new Shop();
            shop.setName(name);

            // Get coordinates
            double lat, lon;
            if (element.has("lat") && element.has("lon")) {
                // Node
                lat = element.get("lat").asDouble();
                lon = element.get("lon").asDouble();
            } else if (element.has("center")) {
                // Way with center
                lat = element.get("center").get("lat").asDouble();
                lon = element.get("center").get("lon").asDouble();
            } else {
                return null;
            }

            shop.setLatitude(lat);
            shop.setLongitude(lon);

            // Build address from OSM tags
            StringBuilder address = new StringBuilder();
            if (tags.has("addr:street")) {
                address.append(tags.get("addr:street").asText());
                if (tags.has("addr:housenumber")) {
                    address.append(" ").append(tags.get("addr:housenumber").asText());
                }
            }
            if (tags.has("addr:city")) {
                if (address.length() > 0)
                    address.append(", ");
                address.append(tags.get("addr:city").asText());
            }
            if (tags.has("addr:postcode")) {
                if (address.length() > 0)
                    address.append(" ");
                address.append(tags.get("addr:postcode").asText());
            }
            if (address.length() == 0) {
                address.append("Italia"); // Fallback
            }
            shop.setAddress(address.toString());

            // Optional fields
            if (tags.has("phone")) {
                shop.setPhoneNumber(tags.get("phone").asText());
            }
            if (tags.has("website")) {
                shop.setWebsiteUrl(tags.get("website").asText());
            }
            if (tags.has("description")) {
                shop.setDescription(tags.get("description").asText());
            }

            // Default values
            shop.setType(ShopType.LOCAL_STORE);
            shop.setIsVerified(false);
            shop.setActive(false);

            // Detect TCG types from name
            detectTcgTypes(shop, name);

            // Default services
            shop.setServicesList(Arrays.asList(
                    "BUY_CARDS", "SELL_CARDS", "SEALED_PRODUCTS", "ACCESSORIES"));

            return shop;

        } catch (Exception e) {
            logger.warn("Error parsing OSM element: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Detect TCG types from shop name
     */
    private void detectTcgTypes(Shop shop, String name) {
        List<String> tcgTypes = new ArrayList<>();
        String lowerName = name.toLowerCase();

        if (lowerName.contains("pokemon") || lowerName.contains("pokémon")) {
            tcgTypes.add("POKEMON");
        }
        if (lowerName.contains("magic") || lowerName.contains("mtg")) {
            tcgTypes.add("MAGIC");
        }
        if (lowerName.contains("yugioh") || lowerName.contains("yu-gi-oh")) {
            tcgTypes.add("YUGIOH");
        }
        if (lowerName.contains("one piece")) {
            tcgTypes.add("ONE_PIECE");
        }
        if (lowerName.contains("dragon ball")) {
            tcgTypes.add("DRAGON_BALL");
        }
        if (lowerName.contains("lorcana")) {
            tcgTypes.add("LORCANA");
        }
        if (lowerName.contains("digimon")) {
            tcgTypes.add("DIGIMON");
        }

        // If no specific TCG detected, assume all (generic card shop)
        if (tcgTypes.isEmpty()) {
            tcgTypes.addAll(Arrays.asList("POKEMON", "MAGIC", "YUGIOH", "ONE_PIECE", "DRAGON_BALL", "LORCANA"));
        }

        shop.setTcgTypesList(tcgTypes);
    }

    /**
     * Check if shop already exists in database
     */
    private boolean shopExists(Shop shop) {
        List<Shop> existingShops = shopRepository.findAll();

        for (Shop existing : existingShops) {
            if (existing.getName().equalsIgnoreCase(shop.getName())) {
                double distance = calculateDistance(
                        existing.getLatitude(), existing.getLongitude(),
                        shop.getLatitude(), shop.getLongitude());
                if (distance < 0.1) { // Less than 100 meters
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculate distance between two coordinates in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Build summary response
     */
    private Map<String, Object> buildSummary(int totalFound, int totalInserted,
            int totalSkipped, List<String> errors, boolean dryRun) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("source", "OpenStreetMap");
        summary.put("totalFound", totalFound);
        summary.put("totalInserted", totalInserted);
        summary.put("totalSkipped", totalSkipped);
        summary.put("errors", errors);
        summary.put("dryRun", dryRun);
        summary.put("cost", "FREE");
        return summary;
    }
}
