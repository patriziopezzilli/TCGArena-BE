package com.tcg.arena.service;

import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopType;
import com.tcg.arena.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

@Service
public class GooglePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(GooglePlacesService.class);

    @Autowired
    private ShopRepository shopRepository;

    @Value("${google.places.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // API Request tracking
    private static class ApiRequestTracker {
        int nearbySearchRequests = 0;
        int placeDetailsRequests = 0;
        int totalRequests = 0;
        
        void incrementNearbySearch() {
            nearbySearchRequests++;
            totalRequests++;
        }
        
        void incrementPlaceDetails() {
            placeDetailsRequests++;
            totalRequests++;
        }
        
        boolean hasReachedLimit(int maxRequests) {
            return totalRequests >= maxRequests;
        }
    }

    // Italian cities with coordinates for radius search - Focus on Italy only to optimize API quota
    private static final Map<String, double[]> ITALIAN_CITIES = Map.ofEntries(
            // Lazio
            Map.entry("Roma, Italia", new double[]{41.9028, 12.4964}),
            Map.entry("Latina, Italia", new double[]{41.4677, 12.9036}),
            Map.entry("Frosinone, Italia", new double[]{41.6395, 13.3509}),
            Map.entry("Viterbo, Italia", new double[]{42.4173, 12.1075}),
            
            // Lombardia
            Map.entry("Milano, Italia", new double[]{45.4642, 9.1900}),
            Map.entry("Bergamo, Italia", new double[]{45.6983, 9.6773}),
            Map.entry("Brescia, Italia", new double[]{45.5416, 10.2118}),
            Map.entry("Monza, Italia", new double[]{45.5845, 9.2744}),
            Map.entry("Como, Italia", new double[]{45.8081, 9.0852}),
            Map.entry("Varese, Italia", new double[]{45.8206, 8.8251}),
            Map.entry("Pavia, Italia", new double[]{45.1847, 9.1582}),
            Map.entry("Cremona, Italia", new double[]{45.1335, 10.0226}),
            Map.entry("Mantova, Italia", new double[]{45.1564, 10.7914}),
            Map.entry("Lecco, Italia", new double[]{45.8559, 9.3988}),
            
            // Campania
            Map.entry("Napoli, Italia", new double[]{40.8518, 14.2681}),
            Map.entry("Salerno, Italia", new double[]{40.6824, 14.7681}),
            Map.entry("Caserta, Italia", new double[]{41.0732, 14.3328}),
            Map.entry("Avellino, Italia", new double[]{40.9142, 14.7906}),
            Map.entry("Benevento, Italia", new double[]{41.1297, 14.7820}),
            
            // Piemonte
            Map.entry("Torino, Italia", new double[]{45.0703, 7.6869}),
            Map.entry("Alessandria, Italia", new double[]{44.9133, 8.6152}),
            Map.entry("Asti, Italia", new double[]{44.9009, 8.2065}),
            Map.entry("Cuneo, Italia", new double[]{44.3841, 7.5426}),
            Map.entry("Novara, Italia", new double[]{45.4469, 8.6218}),
            
            // Toscana
            Map.entry("Firenze, Italia", new double[]{43.7696, 11.2558}),
            Map.entry("Pisa, Italia", new double[]{43.7228, 10.4017}),
            Map.entry("Livorno, Italia", new double[]{43.5485, 10.3106}),
            Map.entry("Prato, Italia", new double[]{43.8777, 11.1022}),
            Map.entry("Lucca, Italia", new double[]{43.8376, 10.4950}),
            Map.entry("Arezzo, Italia", new double[]{43.4632, 11.8796}),
            Map.entry("Siena, Italia", new double[]{43.3188, 11.3308}),
            Map.entry("Pistoia, Italia", new double[]{43.9330, 10.9177}),
            Map.entry("Grosseto, Italia", new double[]{42.7634, 11.1138}),
            
            // Emilia-Romagna
            Map.entry("Bologna, Italia", new double[]{44.4949, 11.3426}),
            Map.entry("Modena, Italia", new double[]{44.6471, 10.9252}),
            Map.entry("Parma, Italia", new double[]{44.8015, 10.3279}),
            Map.entry("Reggio Emilia, Italia", new double[]{44.6989, 10.6297}),
            Map.entry("Ferrara, Italia", new double[]{44.8381, 11.6198}),
            Map.entry("Ravenna, Italia", new double[]{44.4183, 12.2035}),
            Map.entry("Rimini, Italia", new double[]{44.0678, 12.5695}),
            Map.entry("Forlì, Italia", new double[]{44.2226, 12.0408}),
            Map.entry("Cesena, Italia", new double[]{44.1397, 12.2433}),
            Map.entry("Piacenza, Italia", new double[]{45.0526, 9.6929}),
            
            // Veneto
            Map.entry("Venezia, Italia", new double[]{45.4408, 12.3155}),
            Map.entry("Verona, Italia", new double[]{45.4384, 10.9916}),
            Map.entry("Padova, Italia", new double[]{45.4064, 11.8768}),
            Map.entry("Vicenza, Italia", new double[]{45.5455, 11.5354}),
            Map.entry("Treviso, Italia", new double[]{45.6669, 12.2430}),
            Map.entry("Rovigo, Italia", new double[]{45.0703, 11.7898}),
            Map.entry("Belluno, Italia", new double[]{46.1387, 12.2165}),
            
            // Liguria
            Map.entry("Genova, Italia", new double[]{44.4056, 8.9463}),
            Map.entry("La Spezia, Italia", new double[]{44.1027, 9.8244}),
            Map.entry("Savona, Italia", new double[]{44.3080, 8.4813}),
            Map.entry("Imperia, Italia", new double[]{43.8876, 8.0271}),
            
            // Sicilia
            Map.entry("Palermo, Italia", new double[]{38.1157, 13.3615}),
            Map.entry("Catania, Italia", new double[]{37.5079, 15.0830}),
            Map.entry("Messina, Italia", new double[]{38.1938, 15.5540}),
            Map.entry("Siracusa, Italia", new double[]{37.0755, 15.2866}),
            Map.entry("Trapani, Italia", new double[]{38.0176, 12.5365}),
            Map.entry("Agrigento, Italia", new double[]{37.3109, 13.5765}),
            
            // Puglia
            Map.entry("Bari, Italia", new double[]{41.1171, 16.8719}),
            Map.entry("Taranto, Italia", new double[]{40.4762, 17.2403}),
            Map.entry("Foggia, Italia", new double[]{41.4621, 15.5446}),
            Map.entry("Lecce, Italia", new double[]{40.3515, 18.1750}),
            Map.entry("Brindisi, Italia", new double[]{40.6327, 17.9417}),
            
            // Calabria
            Map.entry("Reggio Calabria, Italia", new double[]{38.1113, 15.6473}),
            Map.entry("Catanzaro, Italia", new double[]{38.9098, 16.5877}),
            Map.entry("Cosenza, Italia", new double[]{39.2986, 16.2520}),
            
            // Sardegna
            Map.entry("Cagliari, Italia", new double[]{39.2238, 9.1217}),
            Map.entry("Sassari, Italia", new double[]{40.7259, 8.5594}),
            Map.entry("Olbia, Italia", new double[]{40.9237, 9.5034}),
            
            // Marche
            Map.entry("Ancona, Italia", new double[]{43.6158, 13.5189}),
            Map.entry("Pesaro, Italia", new double[]{43.9103, 12.9133}),
            Map.entry("Macerata, Italia", new double[]{43.2998, 13.4532}),
            Map.entry("Ascoli Piceno, Italia", new double[]{42.8542, 13.5759}),
            
            // Umbria
            Map.entry("Perugia, Italia", new double[]{43.1107, 12.3908}),
            Map.entry("Terni, Italia", new double[]{42.5633, 12.6466}),
            
            // Abruzzo
            Map.entry("L'Aquila, Italia", new double[]{42.3498, 13.3995}),
            Map.entry("Pescara, Italia", new double[]{42.4618, 14.2159}),
            Map.entry("Teramo, Italia", new double[]{42.6589, 13.7039}),
            Map.entry("Chieti, Italia", new double[]{42.3512, 14.1677}),
            
            // Friuli-Venezia Giulia
            Map.entry("Trieste, Italia", new double[]{45.6495, 13.7768}),
            Map.entry("Udine, Italia", new double[]{46.0710, 13.2345}),
            Map.entry("Pordenone, Italia", new double[]{45.9636, 12.6606}),
            
            // Trentino-Alto Adige
            Map.entry("Trento, Italia", new double[]{46.0664, 11.1257}),
            Map.entry("Bolzano, Italia", new double[]{46.4983, 11.3548}),
            
            // Valle d'Aosta
            Map.entry("Aosta, Italia", new double[]{45.7372, 7.3205}),
            
            // Molise
            Map.entry("Campobasso, Italia", new double[]{41.5630, 14.6560}),
            
            // Basilicata
            Map.entry("Potenza, Italia", new double[]{40.6420, 15.7990}),
            Map.entry("Matera, Italia", new double[]{40.6664, 16.6043})
    );

    // Search keywords for TCG shops
    private static final List<String> SEARCH_KEYWORDS = Arrays.asList(
            "pokemon card shop",
            "magic the gathering shop",
            "yugioh card shop",
            "trading card game shop",
            "TCG shop",
            "collectible card game store",
            "fumetteria carte",
            "negozio carte pokemon",
            "negozio giochi da tavolo carte"
    );

    /**
     * Populate shops database with TCG stores from Google Places API
     * @param dryRun if true, only logs what would be inserted without saving
     * @param maxRequests maximum number of API requests to make (default: 950 to stay under 1000)
     * @param skipPlaceDetails if true, skips detailed API calls to save quota
     * @return Summary of the operation
     */
    public Map<String, Object> populateShopsFromGooglePlaces(boolean dryRun, Integer maxRequests, Boolean skipPlaceDetails) {
        // Default values
        int requestLimit = maxRequests != null ? maxRequests : 950; // Safe margin under 1000
        boolean skipDetails = skipPlaceDetails != null ? skipPlaceDetails : false;
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("Google Places API key not configured. Set google.places.api.key in application.properties");
        }

        int totalFound = 0;
        int totalInserted = 0;
        int totalSkipped = 0;
        List<String> errors = new ArrayList<>();
        ApiRequestTracker requestTracker = new ApiRequestTracker();

        logger.info("Starting Google Places shop population for ITALY (dryRun: {}, maxRequests: {}, skipPlaceDetails: {})", 
                dryRun, requestLimit, skipDetails);

        for (Map.Entry<String, double[]> city : ITALIAN_CITIES.entrySet()) {
            // Check if we've reached the request limit
            if (requestTracker.hasReachedLimit(requestLimit)) {
                logger.warn("Reached API request limit of {}. Stopping search.", requestLimit);
                break;
            }
            
            String cityName = city.getKey();
            double[] coords = city.getValue();
            double lat = coords[0];
            double lng = coords[1];

            logger.info("Searching in: {} (API calls: {}/{})", cityName, requestTracker.totalRequests, requestLimit);

            for (String keyword : SEARCH_KEYWORDS) {
                // Check limit before each keyword search
                if (requestTracker.hasReachedLimit(requestLimit)) {
                    break;
                }
                
                try {
                    List<Shop> shops = searchNearbyShops(lat, lng, keyword, 15000, requestTracker, skipDetails);
                    totalFound += shops.size();

                    for (Shop shop : shops) {
                        // Check if shop already exists by name and address similarity
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
                    }

                    // Rate limiting - Google Places has quota limits
                    Thread.sleep(1000); // 1 second between requests
                    
                } catch (Exception e) {
                    String errorMsg = String.format("Error searching %s with keyword '%s': %s", 
                            cityName, keyword, e.getMessage());
                    logger.error(errorMsg, e);
                    errors.add(errorMsg);
                }
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalFound", totalFound);
        summary.put("totalInserted", totalInserted);
        summary.put("totalSkipped", totalSkipped);
        summary.put("errors", errors);
        summary.put("dryRun", dryRun);
        summary.put("apiRequestsUsed", requestTracker.totalRequests);
        summary.put("nearbySearchCalls", requestTracker.nearbySearchRequests);
        summary.put("placeDetailsCalls", requestTracker.placeDetailsRequests);
        summary.put("requestLimit", requestLimit);

        logger.info("Population completed. Found: {}, Inserted: {}, Skipped: {}, API Calls: {}/{}", 
                totalFound, totalInserted, totalSkipped, requestTracker.totalRequests, requestLimit);

        return summary;
    }

    /**
     * Search for shops near a location using Google Places Nearby Search API
     */
    private List<Shop> searchNearbyShops(double lat, double lng, String keyword, int radius, 
                                         ApiRequestTracker tracker, boolean skipDetails) {
        List<Shop> shops = new ArrayList<>();

        try {
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=%f,%f&radius=%d&keyword=%s&key=%s",
                    lat, lng, radius, keyword.replace(" ", "+"), apiKey
            );

            String response = restTemplate.getForObject(url, String.class);
            tracker.incrementNearbySearch(); // Track API call
            
            JsonNode root = objectMapper.readTree(response);

            if (!"OK".equals(root.get("status").asText()) && !"ZERO_RESULTS".equals(root.get("status").asText())) {
                logger.warn("Google Places API returned status: {}", root.get("status").asText());
                return shops;
            }

            JsonNode results = root.get("results");
            if (results != null && results.isArray()) {
                for (JsonNode result : results) {
                    Shop shop = parseShopFromPlaceResult(result, tracker, skipDetails);
                    if (shop != null) {
                        shops.add(shop);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error searching places: {}", e.getMessage(), e);
        }

        return shops;
    }

    /**
     * Parse Google Places result into Shop entity
     */
    private Shop parseShopFromPlaceResult(JsonNode placeNode, ApiRequestTracker tracker, boolean skipDetails) {
        try {
            // Check business status first
            if (placeNode.has("business_status")) {
                String status = placeNode.get("business_status").asText();
                if (!"OPERATIONAL".equals(status)) {
                    logger.debug("Skipping non-operational business: {}", placeNode.get("name").asText());
                    return null; // Skip closed/suspended businesses
                }
            }

            Shop shop = new Shop();

            // Basic info
            shop.setName(placeNode.get("name").asText());
            shop.setAddress(placeNode.has("vicinity") ? placeNode.get("vicinity").asText() : "");

            // Location
            JsonNode location = placeNode.get("geometry").get("location");
            shop.setLatitude(location.get("lat").asDouble());
            shop.setLongitude(location.get("lng").asDouble());

            // Check rating - prefer shops with good ratings
            double minRating = 3.0; // Minimum acceptable rating
            if (placeNode.has("rating")) {
                double rating = placeNode.get("rating").asDouble();
                if (rating < minRating) {
                    logger.debug("Skipping low-rated shop: {} (rating: {})", shop.getName(), rating);
                    return null;
                }
            }

            // Default values
            shop.setType(ShopType.LOCAL_STORE); // Physical store
            shop.setIsVerified(false); // Needs manual verification
            shop.setActive(false); // Inactive until verified by admin

            // Get place details if place_id is available and not skipping
            if (!skipDetails && placeNode.has("place_id")) {
                String placeId = placeNode.get("place_id").asText();
                enrichShopWithPlaceDetails(shop, placeId, tracker);
            } else if (skipDetails) {
                logger.debug("Skipping place details for {} to save API quota", shop.getName());
            }

            // Detect TCG types and services from multiple sources
            detectTcgTypesAndServices(shop, placeNode);

            return shop;

        } catch (Exception e) {
            logger.error("Error parsing place result: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Enrich shop with additional details from Google Places Details API
     */
    private void enrichShopWithPlaceDetails(Shop shop, String placeId, ApiRequestTracker tracker) {
        try {
            tracker.incrementPlaceDetails(); // Track API call
            String url = String.format(
                    "https://maps.googleapis.com/maps/api/place/details/json?place_id=%s&fields=formatted_phone_number,website,opening_hours,formatted_address,editorial_summary,reviews,url&key=%s",
                    placeId, apiKey
            );

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if ("OK".equals(root.get("status").asText())) {
                JsonNode result = root.get("result");

                if (result.has("formatted_phone_number")) {
                    shop.setPhoneNumber(result.get("formatted_phone_number").asText());
                }

                if (result.has("website")) {
                    shop.setWebsiteUrl(result.get("website").asText());
                }

                if (result.has("formatted_address")) {
                    shop.setAddress(result.get("formatted_address").asText());
                }

                // Extract description from editorial summary or reviews
                if (result.has("editorial_summary") && result.get("editorial_summary").has("overview")) {
                    String description = result.get("editorial_summary").get("overview").asText();
                    shop.setDescription(description);
                } else if (result.has("reviews")) {
                    // Use first review text as description if no editorial summary
                    JsonNode reviews = result.get("reviews");
                    if (reviews.isArray() && reviews.size() > 0) {
                        String reviewText = reviews.get(0).get("text").asText();
                        if (reviewText.length() > 500) {
                            reviewText = reviewText.substring(0, 497) + "...";
                        }
                        shop.setDescription(reviewText);
                    }
                }

                // Parse opening hours if available
                if (result.has("opening_hours") && result.get("opening_hours").has("periods")) {
                    try {
                        String openingHoursJson = objectMapper.writeValueAsString(
                            result.get("opening_hours").get("periods")
                        );
                        shop.setOpeningHoursJson(openingHoursJson);
                        logger.debug("Parsed opening hours for {}", shop.getName());
                    } catch (Exception e) {
                        logger.warn("Could not parse opening hours: {}", e.getMessage());
                    }
                }
            }

            // Rate limiting
            Thread.sleep(500);

        } catch (Exception e) {
            logger.warn("Could not enrich shop details for place_id {}: {}", placeId, e.getMessage());
        }
    }

    /**
     * Detect TCG types and services from multiple sources:
     * - Shop name
     * - Google place types
     * - Description/editorial summary (from place details)
     */
    private void detectTcgTypesAndServices(Shop shop, JsonNode placeNode) {
        List<String> tcgTypes = new ArrayList<>();
        List<String> services = new ArrayList<>();
        
        // Combine all text sources for analysis
        StringBuilder allText = new StringBuilder();
        allText.append(shop.getName().toLowerCase()).append(" ");
        
        if (shop.getDescription() != null) {
            allText.append(shop.getDescription().toLowerCase()).append(" ");
        }
        
        // Add Google place types
        if (placeNode.has("types")) {
            JsonNode typesArray = placeNode.get("types");
            for (JsonNode typeNode : typesArray) {
                allText.append(typeNode.asText().toLowerCase()).append(" ");
            }
        }
        
        String combinedText = allText.toString();
        
        // Detect TCG types from combined text
        if (combinedText.contains("pokemon") || combinedText.contains("pokémon")) {
            tcgTypes.add("POKEMON");
        }
        if (combinedText.contains("magic") || combinedText.contains("mtg") || 
            combinedText.contains("the gathering")) {
            tcgTypes.add("MAGIC");
        }
        if (combinedText.contains("yugioh") || combinedText.contains("yu-gi-oh") ||
            combinedText.contains("yu gi oh")) {
            tcgTypes.add("YUGIOH");
        }
        if (combinedText.contains("one piece")) {
            tcgTypes.add("ONE_PIECE");
        }
        if (combinedText.contains("lorcana")) {
            tcgTypes.add("LORCANA");
        }

        // If no specific TCG detected, add all (generic card shop)
        if (tcgTypes.isEmpty()) {
            tcgTypes.addAll(Arrays.asList("POKEMON", "MAGIC", "YUGIOH", "ONE_PIECE", "LORCANA"));
        }

        shop.setTcgTypesList(tcgTypes);

        // Detect services from text and place types
        services.add("BUY_CARDS");
        services.add("SELL_CARDS");
        
        // Detect specific services from keywords
        if (combinedText.contains("trade") || combinedText.contains("scambio")) {
            services.add("TRADE");
        }
        
        if (combinedText.contains("tournament") || combinedText.contains("torneo") ||
            combinedText.contains("competition") || combinedText.contains("event")) {
            services.add("TOURNAMENTS");
            services.add("EVENTS");
        }
        
        if (combinedText.contains("grading") || combinedText.contains("psa") || 
            combinedText.contains("cgc") || combinedText.contains("valutazione")) {
            services.add("CARD_GRADING");
        }
        
        if (combinedText.contains("preorder") || combinedText.contains("pre-order") ||
            combinedText.contains("preordine")) {
            services.add("PREORDERS");
        }
        
        // Always add these for card shops
        services.add("SEALED_PRODUCTS");
        services.add("ACCESSORIES");
        
        // Check if it's likely to have play area
        if (combinedText.contains("play") || combinedText.contains("game") ||
            combinedText.contains("gioco") || combinedText.contains("sala") ||
            placeNode.has("rating") && placeNode.get("rating").asDouble() >= 4.0) {
            services.add("PLAY_AREA");
        }

        shop.setServicesList(services);
    }

    /**
     * Check if shop already exists in database
     */
    private boolean shopExists(Shop shop) {
        // Check by exact name and similar location (within 100 meters)
        List<Shop> existingShops = shopRepository.findAll();
        
        for (Shop existing : existingShops) {
            if (existing.getName().equalsIgnoreCase(shop.getName())) {
                // Check distance
                double distance = calculateDistance(
                        existing.getLatitude(), existing.getLongitude(),
                        shop.getLatitude(), shop.getLongitude()
                );
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
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
