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

    // Italian regions bounding boxes (South, West, North, East)
    private static final Map<String, double[]> ITALIAN_REGIONS = Map.ofEntries(
            Map.entry("Lombardia", new double[] { 45.0, 8.5, 46.5, 11.5 }),
            Map.entry("Lazio", new double[] { 41.0, 11.5, 42.8, 14.0 }),
            Map.entry("Campania", new double[] { 40.0, 13.5, 41.5, 15.8 }),
            Map.entry("Piemonte", new double[] { 44.0, 6.6, 46.5, 9.0 }),
            Map.entry("Veneto", new double[] { 44.8, 10.6, 47.0, 13.1 }),
            Map.entry("Emilia-Romagna", new double[] { 44.0, 9.2, 45.2, 12.8 }),
            Map.entry("Toscana", new double[] { 42.2, 9.7, 44.5, 12.4 }),
            Map.entry("Sicilia", new double[] { 36.6, 12.4, 38.8, 15.7 }),
            Map.entry("Puglia", new double[] { 39.8, 15.0, 42.0, 18.6 }),
            Map.entry("Liguria", new double[] { 43.8, 7.5, 44.7, 10.1 }),
            Map.entry("Sardegna", new double[] { 38.8, 8.1, 41.3, 9.9 }),
            Map.entry("Calabria", new double[] { 37.9, 15.6, 40.2, 17.2 }),
            Map.entry("Friuli-Venezia Giulia", new double[] { 45.6, 12.3, 46.7, 14.0 }),
            Map.entry("Trentino-Alto Adige", new double[] { 45.7, 10.4, 47.1, 12.5 }),
            Map.entry("Marche", new double[] { 42.7, 12.1, 43.9, 13.9 }),
            Map.entry("Abruzzo", new double[] { 41.7, 13.0, 42.9, 14.8 }),
            Map.entry("Umbria", new double[] { 42.4, 12.0, 43.4, 13.3 }));

    // Alternative Overpass API endpoints for load balancing
    private static final String[] OVERPASS_ENDPOINTS = {
            "https://overpass-api.de/api/interpreter",
            "https://lz4.overpass-api.de/api/interpreter",
            "https://z.overpass-api.de/api/interpreter"
    };

    /**
     * Populate shops database with TCG stores from OpenStreetMap Overpass API.
     * This is 100% FREE with no API limits.
     * Queries by region to avoid timeouts.
     * 
     * @param dryRun if true, only logs what would be inserted without saving
     * @return Summary of the operation
     */
    public Map<String, Object> populateShopsFromOpenStreetMap(boolean dryRun) {
        int totalFound = 0;
        int totalInserted = 0;
        int totalSkipped = 0;
        List<String> errors = new ArrayList<>();
        int regionsProcessed = 0;

        logger.info("Starting OpenStreetMap shop population for ITALY by regions (dryRun: {})", dryRun);

        for (Map.Entry<String, double[]> region : ITALIAN_REGIONS.entrySet()) {
            String regionName = region.getKey();
            double[] bbox = region.getValue();

            logger.info("Processing region: {} ({}/{})", regionName, ++regionsProcessed, ITALIAN_REGIONS.size());

            try {
                String query = buildOverpassQueryForBbox(bbox);
                String response = executeOverpassQueryWithRetry(query);

                if (response == null) {
                    errors.add("Failed to get response for region: " + regionName);
                    continue;
                }

                JsonNode root = objectMapper.readTree(response);
                JsonNode elements = root.get("elements");

                if (elements != null && elements.isArray()) {
                    logger.info("Found {} elements in {}", elements.size(), regionName);

                    for (JsonNode element : elements) {
                        totalFound++;

                        try {
                            Shop shop = parseShopFromOsmElement(element);

                            if (shop == null) {
                                totalSkipped++;
                                continue;
                            }

                            if (shopExists(shop)) {
                                totalSkipped++;
                                continue;
                            }

                            if (!dryRun) {
                                shopRepository.save(shop);
                                logger.info("Inserted: {} - {}", shop.getName(), shop.getAddress());
                            } else {
                                logger.info("Would insert: {} - {}", shop.getName(), shop.getAddress());
                            }
                            totalInserted++;

                        } catch (Exception e) {
                            logger.warn("Error processing element: {}", e.getMessage());
                            totalSkipped++;
                        }
                    }
                }

                // Small delay between regions to be nice to the API
                Thread.sleep(2000);

            } catch (Exception e) {
                String errorMsg = "Error querying region " + regionName + ": " + e.getMessage();
                logger.error(errorMsg);
                errors.add(errorMsg);
            }
        }

        logger.info("Population completed. Found: {}, Inserted: {}, Skipped: {}",
                totalFound, totalInserted, totalSkipped);

        return buildSummary(totalFound, totalInserted, totalSkipped, errors, dryRun);
    }

    /**
     * Build Overpass QL query for a specific bounding box
     */
    private String buildOverpassQueryForBbox(double[] bbox) {
        StringBuilder query = new StringBuilder();
        query.append("[out:json][timeout:120];");
        query.append("(");

        // bbox format: south,west,north,east
        String bboxStr = String.format("%.4f,%.4f,%.4f,%.4f", bbox[0], bbox[1], bbox[2], bbox[3]);

        for (String[] tag : SEARCH_TAGS) {
            String key = tag[0];
            String value = tag[1];
            query.append(String.format("node[\"%s\"=\"%s\"](%s);", key, value, bboxStr));
            query.append(String.format("way[\"%s\"=\"%s\"](%s);", key, value, bboxStr));
        }

        query.append(");");
        query.append("out body center;");

        return query.toString();
    }

    /**
     * Execute Overpass API query with retry on multiple endpoints
     */
    private String executeOverpassQueryWithRetry(String query) {
        for (String endpoint : OVERPASS_ENDPOINTS) {
            try {
                logger.debug("Trying endpoint: {}", endpoint);

                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

                org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
                body.add("data", query);

                org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request = new org.springframework.http.HttpEntity<>(
                        body, headers);

                String response = restTemplate.postForObject(endpoint, request, String.class);

                if (response != null) {
                    return response;
                }

            } catch (Exception e) {
                logger.warn("Endpoint {} failed: {}", endpoint, e.getMessage());
                // Try next endpoint
            }
        }

        logger.error("All Overpass endpoints failed");
        return null;
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
