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

/**
 * Service for populating shops from HERE Places API.
 * This is a STABLE alternative to OpenStreetMap with FREE 250k calls/month.
 * 
 * @see https://developer.here.com/documentation/geocoding-search-api/
 */
@Service
public class HerePlacesService {

    private static final Logger logger = LoggerFactory.getLogger(HerePlacesService.class);

    @Autowired
    private ShopRepository shopRepository;

    @Value("${here.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Activate all shops in the database.
     * Useful after bulk population.
     */
    @org.springframework.transaction.annotation.Transactional
    public void activateAllShops() {
        shopRepository.updateAllActive(true);
        logger.info("All shops set to active=true");
    }

    // HERE category IDs for shops we're interested in
    // See:
    // https://developer.here.com/documentation/geocoding-search-api/dev_guide/topics/categories.html
    private static final String[] CATEGORY_IDS = {
            "600-6900-0000", // Bookstore (includes comic shops)
            "600-6100-0000", // Shopping Mall
            "600-6200-0066", // Toy and Game Store
            "600-6800-0000" // Hobby Store
    };

    // Italian cities to search (All 107 province capitals)
    private static final Map<String, double[]> ITALIAN_CITIES = Map.ofEntries(
            // Abruzzo
            Map.entry("L'Aquila", new double[] { 42.3498, 13.3995 }),
            Map.entry("Chieti", new double[] { 42.3510, 14.1675 }),
            Map.entry("Pescara", new double[] { 42.4618, 14.2158 }),
            Map.entry("Teramo", new double[] { 42.6589, 13.7039 }),
            // Basilicata
            Map.entry("Potenza", new double[] { 40.6404, 15.8056 }),
            Map.entry("Matera", new double[] { 40.6664, 16.6043 }),
            // Calabria
            Map.entry("Catanzaro", new double[] { 38.9098, 16.5877 }),
            Map.entry("Cosenza", new double[] { 39.3037, 16.2541 }),
            Map.entry("Crotone", new double[] { 39.0804, 17.1263 }),
            Map.entry("Reggio Calabria", new double[] { 38.1113, 15.6473 }),
            Map.entry("Vibo Valentia", new double[] { 38.6757, 16.0967 }),
            // Campania
            Map.entry("Napoli", new double[] { 40.8518, 14.2681 }),
            Map.entry("Avellino", new double[] { 40.9140, 14.7933 }),
            Map.entry("Benevento", new double[] { 41.1298, 14.7818 }),
            Map.entry("Caserta", new double[] { 41.0743, 14.3312 }),
            Map.entry("Salerno", new double[] { 40.6824, 14.7681 }),
            // Emilia-Romagna
            Map.entry("Bologna", new double[] { 44.4949, 11.3426 }),
            Map.entry("Ferrara", new double[] { 44.8381, 11.6198 }),
            Map.entry("Forlì", new double[] { 44.2227, 12.0407 }),
            Map.entry("Cesena", new double[] { 44.1391, 12.2432 }),
            Map.entry("Modena", new double[] { 44.6471, 10.9252 }),
            Map.entry("Parma", new double[] { 44.8015, 10.3279 }),
            Map.entry("Piacenza", new double[] { 45.0526, 9.6930 }),
            Map.entry("Ravenna", new double[] { 44.4184, 12.2035 }),
            Map.entry("Reggio Emilia", new double[] { 44.6990, 10.6300 }),
            Map.entry("Rimini", new double[] { 44.0678, 12.5695 }),
            // Friuli-Venezia Giulia
            Map.entry("Trieste", new double[] { 45.6495, 13.7768 }),
            Map.entry("Gorizia", new double[] { 45.9402, 13.6202 }),
            Map.entry("Pordenone", new double[] { 45.9626, 12.6563 }),
            Map.entry("Udine", new double[] { 46.0637, 13.2446 }),
            // Lazio
            Map.entry("Roma", new double[] { 41.9028, 12.4964 }),
            Map.entry("Frosinone", new double[] { 41.6397, 13.3411 }),
            Map.entry("Latina", new double[] { 41.4676, 12.9038 }),
            Map.entry("Rieti", new double[] { 42.4042, 12.8624 }),
            Map.entry("Viterbo", new double[] { 42.4174, 12.1047 }),
            // Liguria
            Map.entry("Genova", new double[] { 44.4056, 8.9463 }),
            Map.entry("Imperia", new double[] { 43.8876, 8.0294 }),
            Map.entry("La Spezia", new double[] { 44.1025, 9.8241 }),
            Map.entry("Savona", new double[] { 44.3079, 8.4812 }),
            // Lombardia
            Map.entry("Milano", new double[] { 45.4642, 9.1900 }),
            Map.entry("Bergamo", new double[] { 45.6983, 9.6773 }),
            Map.entry("Brescia", new double[] { 45.5416, 10.2118 }),
            Map.entry("Como", new double[] { 45.8081, 9.0852 }),
            Map.entry("Cremona", new double[] { 45.1332, 10.0213 }),
            Map.entry("Lecco", new double[] { 45.8566, 9.3977 }),
            Map.entry("Lodi", new double[] { 45.3097, 9.5037 }),
            Map.entry("Mantova", new double[] { 45.1564, 10.7914 }),
            Map.entry("Monza", new double[] { 45.5845, 9.2744 }),
            Map.entry("Pavia", new double[] { 45.1847, 9.1582 }),
            Map.entry("Sondrio", new double[] { 46.1711, 9.8715 }),
            Map.entry("Varese", new double[] { 45.8206, 8.8251 }),
            // Marche
            Map.entry("Ancona", new double[] { 43.6158, 13.5189 }),
            Map.entry("Ascoli Piceno", new double[] { 42.8550, 13.5749 }),
            Map.entry("Fermo", new double[] { 43.1605, 13.7183 }),
            Map.entry("Macerata", new double[] { 43.3002, 13.4531 }),
            Map.entry("Pesaro", new double[] { 43.9125, 12.9155 }),
            Map.entry("Urbino", new double[] { 43.7262, 12.6366 }),
            // Molise
            Map.entry("Campobasso", new double[] { 41.5603, 14.6627 }),
            Map.entry("Isernia", new double[] { 41.5880, 14.2259 }),
            // Piemonte
            Map.entry("Torino", new double[] { 45.0703, 7.6869 }),
            Map.entry("Alessandria", new double[] { 44.9133, 8.6148 }),
            Map.entry("Asti", new double[] { 44.8996, 8.2045 }),
            Map.entry("Biella", new double[] { 45.5629, 8.0583 }),
            Map.entry("Cuneo", new double[] { 44.3845, 7.5427 }),
            Map.entry("Novara", new double[] { 45.4469, 8.6212 }),
            Map.entry("Verbania", new double[] { 45.9220, 8.5518 }),
            Map.entry("Vercelli", new double[] { 45.3208, 8.4197 }),
            // Puglia
            Map.entry("Bari", new double[] { 41.1171, 16.8719 }),
            Map.entry("Andria", new double[] { 41.2263, 16.2952 }),
            Map.entry("Barletta", new double[] { 41.3174, 16.2829 }),
            Map.entry("Trani", new double[] { 41.2721, 16.4173 }),
            Map.entry("Brindisi", new double[] { 40.6384, 17.9455 }),
            Map.entry("Foggia", new double[] { 41.4622, 15.5446 }),
            Map.entry("Lecce", new double[] { 40.3515, 18.1750 }),
            Map.entry("Taranto", new double[] { 40.4638, 17.2471 }),
            // Sardegna
            Map.entry("Cagliari", new double[] { 39.2238, 9.1217 }),
            Map.entry("Nuoro", new double[] { 40.3195, 9.3308 }),
            Map.entry("Oristano", new double[] { 39.9056, 8.5910 }),
            Map.entry("Sassari", new double[] { 40.7259, 8.5557 }),
            Map.entry("Sud Sardegna", new double[] { 39.1664, 8.5262 }),
            // Sicilia
            Map.entry("Palermo", new double[] { 38.1157, 13.3615 }),
            Map.entry("Agrigento", new double[] { 37.3111, 13.5765 }),
            Map.entry("Caltanissetta", new double[] { 37.4902, 14.0622 }),
            Map.entry("Catania", new double[] { 37.5079, 15.0830 }),
            Map.entry("Enna", new double[] { 37.5670, 14.2811 }),
            Map.entry("Messina", new double[] { 38.1938, 15.5540 }),
            Map.entry("Ragusa", new double[] { 36.9269, 14.7255 }),
            Map.entry("Siracusa", new double[] { 37.0755, 15.2866 }),
            Map.entry("Trapani", new double[] { 38.0176, 12.5370 }),
            // Toscana
            Map.entry("Firenze", new double[] { 43.7696, 11.2558 }),
            Map.entry("Arezzo", new double[] { 43.4685, 11.8815 }),
            Map.entry("Grosseto", new double[] { 42.7607, 11.1099 }),
            Map.entry("Livorno", new double[] { 43.5485, 10.3106 }),
            Map.entry("Lucca", new double[] { 43.8429, 10.5027 }),
            Map.entry("Massa", new double[] { 44.0205, 10.1197 }),
            Map.entry("Carrara", new double[] { 44.0792, 10.1009 }),
            Map.entry("Pisa", new double[] { 43.7228, 10.4017 }),
            Map.entry("Pistoia", new double[] { 43.9311, 10.9176 }),
            Map.entry("Prato", new double[] { 43.8777, 11.1022 }),
            Map.entry("Siena", new double[] { 43.3188, 11.3308 }),
            // Trentino-Alto Adige
            Map.entry("Trento", new double[] { 46.0748, 11.1217 }),
            Map.entry("Bolzano", new double[] { 46.4993, 11.3566 }),
            // Umbria
            Map.entry("Perugia", new double[] { 43.1107, 12.3908 }),
            Map.entry("Terni", new double[] { 42.5638, 12.6455 }),
            // Valle d'Aosta
            Map.entry("Aosta", new double[] { 45.7373, 7.3204 }),
            // Veneto
            Map.entry("Venezia", new double[] { 45.4408, 12.3155 }),
            Map.entry("Belluno", new double[] { 46.1364, 12.2163 }),
            Map.entry("Padova", new double[] { 45.4064, 11.8768 }),
            Map.entry("Rovigo", new double[] { 45.0705, 11.7906 }),
            Map.entry("Treviso", new double[] { 45.6669, 12.2424 }),
            Map.entry("Verona", new double[] { 45.4384, 10.9916 }),
            Map.entry("Vicenza", new double[] { 45.5455, 11.5354 }));

    // STRONG Keywords: Specific Brands (If found, ACCEPT immediately)
    private static final List<String> STRONG_KEYWORDS = Arrays.asList(
            "pokemon", "pokémon", "magic", "mtg", "yugioh", "yu-gi-oh",
            "one piece", "dragon ball", "lorcana", "digimon", "warcraft",
            "star wars", "flesh and blood", "keyforge", "altered");

    // WEAK Keywords: Generic terms (If found AND Category matches, ACCEPT)
    private static final List<String> WEAK_KEYWORDS = Arrays.asList(
            "trading card", "tcg", "carte", "fumett", "comic", "game",
            "giochi", "gioco", "cards", "collezionismo", "collect",
            "hobby", "ludoteca", "manga", "otaku", "nerd", "geek");

    /**
     * Populate shops database with TCG stores from HERE Places API.
     * Stable API with 250k free calls/month.
     * 
     * @param dryRun if true, only logs what would be inserted without saving
     * @return Summary of the operation
     */
    public Map<String, Object> populateShopsFromHere(boolean dryRun) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("HERE API key not configured. Set here.api.key in application.properties");
        }

        int totalFound = 0;
        int totalInserted = 0;
        int totalSkipped = 0;
        List<String> errors = new ArrayList<>();
        int citiesProcessed = 0;

        logger.info("Starting HERE Places shop population for ITALY (dryRun: {})", dryRun);

        String[] searchQueries = {
                "fumetteria",
                "negozio carte collezionabili",
                "giochi da tavolo",
                "trading card game",
                "pokemon store",
                "magic the gathering",
                "warhammer"
        };

        for (Map.Entry<String, double[]> city : ITALIAN_CITIES.entrySet()) {
            String cityName = city.getKey();
            double[] coords = city.getValue();

            logger.info("Searching in: {} ({}/{})", cityName, ++citiesProcessed, ITALIAN_CITIES.size());

            for (String query : searchQueries) {
                try {
                    List<Shop> shops = discoverShops(coords[0], coords[1], query);

                    for (Shop shop : shops) {
                        totalFound++;

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
                    }

                    // Rate limiting
                    Thread.sleep(200);

                } catch (Exception e) {
                    String errorMsg = "Error searching " + cityName + " for '" + query + "': " + e.getMessage();
                    logger.error(errorMsg);
                    errors.add(errorMsg);
                }
            }

            // Delay between cities
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        logger.info("Population completed. Found: {}, Inserted: {}, Skipped: {}",
                totalFound, totalInserted, totalSkipped);

        return buildSummary(totalFound, totalInserted, totalSkipped, errors, dryRun);
    }

    /**
     * Browse for shops near a location using HERE Browse API
     */
    private static final String HERE_DISCOVER_URL = "https://discover.search.hereapi.com/v1/discover";

    /**
     * Search for shops using free-text queries (much better results than category
     * browse)
     */
    private List<Shop> discoverShops(double lat, double lng, String query) {
        List<Shop> shops = new ArrayList<>();

        try {
            // q=query text, at=location
            String url = String.format(
                    "%s?at=%.6f,%.6f&q=%s&in=countryCode:ITA&limit=50&apiKey=%s",
                    HERE_DISCOVER_URL, lat, lng, query, apiKey);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                return shops;
            }

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");

            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    Shop shop = parseShopFromHereItem(item);
                    if (shop != null) {
                        shops.add(shop);
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error discovering HERE API: {}", e.getMessage());
        }

        return shops;
    }

    /**
     * Parse HERE API item into Shop entity
     */
    private Shop parseShopFromHereItem(JsonNode item) {
        try {
            String title = item.has("title") ? item.get("title").asText() : null;
            if (title == null || title.isEmpty()) {
                return null;
            }

            // ULTRA-ROBUST FILTERING LOGIC
            // __________________________________________________________________________

            String lowerTitle = title.toLowerCase();

            // 1. BLACKLIST: Reject immediately if name matches irrelevant terms
            if (isIrrelevantShop(lowerTitle)) {
                return null;
            }

            // 2. STRONG WHITELIST: Accept immediately if name contains specific TCG brands
            // (e.g. "Pokemon Store", "Magic Corner") - regardless of category
            boolean hasStrongKeyword = STRONG_KEYWORDS.stream().anyMatch(lowerTitle::contains);
            if (hasStrongKeyword) {
                // ACCEPT
            } else {
                // 3. INTERSECTION: Category + Weak Keyword
                // If no strong keyword, we REQUIRE both a valid Category AND a "Weak" keyword
                // (e.g. "games", "comics", "cards")

                boolean isTargetCategory = false;
                if (item.has("categories")) {
                    for (JsonNode cat : item.get("categories")) {
                        String catId = cat.has("id") ? cat.get("id").asText() : "";

                        // Check if it matches any of our target categories (Books, Malls, Toys, Hobby)
                        for (String targetId : CATEGORY_IDS) {
                            if (catId.startsWith(targetId.substring(0, 8))) { // Compare only the main category part
                                isTargetCategory = true;
                                break;
                            }
                        }
                        if (isTargetCategory)
                            break; // Found a target category, no need to check other categories for this item
                    }
                }

                if (!isTargetCategory) {
                    return null; // Wrong category
                }

                // If Category matches, we STILL require a Weak Keyword to confirm relevance
                // This filters out "Lego Store" (Toy Category) or "Il Telaio" (Hobby Category)
                boolean hasWeakKeyword = WEAK_KEYWORDS.stream().anyMatch(lowerTitle::contains);

                if (!hasWeakKeyword) {
                    logger.debug("Rejected generic shop '{}' (Category match but no keyword)", title);
                    return null;
                }
            }

            Shop shop = new Shop();
            shop.setName(title);

            // Get coordinates
            JsonNode position = item.get("position");
            if (position != null) {
                shop.setLatitude(position.get("lat").asDouble());
                shop.setLongitude(position.get("lng").asDouble());
            } else {
                return null;
            }

            // Get address
            if (item.has("address")) {
                JsonNode address = item.get("address");
                StringBuilder addr = new StringBuilder();
                if (address.has("street")) {
                    addr.append(address.get("street").asText());
                }
                if (address.has("houseNumber")) {
                    addr.append(" ").append(address.get("houseNumber").asText());
                }
                if (address.has("city")) {
                    if (addr.length() > 0)
                        addr.append(", ");
                    addr.append(address.get("city").asText());
                }
                if (address.has("postalCode")) {
                    if (addr.length() > 0)
                        addr.append(" ");
                    addr.append(address.get("postalCode").asText());
                }
                shop.setAddress(addr.length() > 0 ? addr.toString() : "Italia");
            }

            // Get contact info
            if (item.has("contacts")) {
                JsonNode contacts = item.get("contacts");
                if (contacts.isArray() && contacts.size() > 0) {
                    JsonNode contact = contacts.get(0);
                    if (contact.has("phone")) {
                        JsonNode phones = contact.get("phone");
                        if (phones.isArray() && phones.size() > 0) {
                            shop.setPhoneNumber(phones.get(0).get("value").asText());
                        }
                    }
                    if (contact.has("www")) {
                        JsonNode www = contact.get("www");
                        if (www.isArray() && www.size() > 0) {
                            shop.setWebsiteUrl(www.get(0).get("value").asText());
                        }
                    }
                }
            }

            // Default values - AUTO ACTIVE AS REQUESTED
            shop.setType(ShopType.LOCAL_STORE);
            shop.setIsVerified(false);
            shop.setActive(true); // <--- Auto-activate shops!

            // Detect TCG types from name
            detectTcgTypes(shop, title);

            // Default services
            shop.setServicesList(Arrays.asList(
                    "BUY_CARDS", "SELL_CARDS", "SEALED_PRODUCTS", "ACCESSORIES"));

            return shop;

        } catch (Exception e) {
            logger.warn("Error parsing HERE item: {}", e.getMessage());
            return null;
        }
    }

    private boolean isIrrelevantShop(String name) {
        return name.contains("erboristeria") ||
                name.contains("farmacia") ||
                name.contains("abbigliamento") ||
                name.contains("boutique") ||
                name.contains("sartoria") ||
                name.contains("tabacchi") ||
                name.contains("profumeria") ||
                name.contains("ottica") ||
                name.contains("immobiliare") ||
                name.contains("ristorante") ||
                name.contains("pizzeria") ||
                name.contains("bar ") ||
                name.endsWith(" bar") ||
                name.contains("parrucchiere") ||
                name.contains("estetica") ||
                name.contains("supermercato") ||
                name.contains("fashion") ||
                name.contains("moda") ||
                name.contains("donna") ||
                name.contains("uomo") ||
                name.contains("shoes") ||
                name.contains("scarpe") ||
                name.contains("calzature") ||
                name.contains("gioielleria") ||
                name.contains("compro oro") ||
                name.contains("souvenir") ||
                name.contains("kiosk") ||
                name.contains("edicola") || // Often vague, better to exclude unless explicit
                name.contains("zara") ||
                name.contains("h&m") ||
                name.contains("oviesse") ||
                name.contains("ovs") ||
                name.contains("coin") ||
                name.contains("bioprofumeria");
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
        summary.put("source", "HERE Places API");
        summary.put("totalFound", totalFound);
        summary.put("totalInserted", totalInserted);
        summary.put("totalSkipped", totalSkipped);
        summary.put("errors", errors);
        summary.put("dryRun", dryRun);
        summary.put("freeQuota", "250,000 calls/month");
        return summary;
    }
}
