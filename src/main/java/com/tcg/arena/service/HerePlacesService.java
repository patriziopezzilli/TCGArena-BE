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

    // ISO-2 to ISO-3 country code mapping for HERE API (Worldwide)
    private static final Map<String, String> ISO2_TO_ISO3 = Map.ofEntries(
            // Europe
            Map.entry("FR", "FRA"), // France
            Map.entry("DE", "DEU"), // Germany
            Map.entry("ES", "ESP"), // Spain
            Map.entry("GB", "GBR"), // United Kingdom
            Map.entry("NL", "NLD"), // Netherlands
            Map.entry("BE", "BEL"), // Belgium
            Map.entry("AT", "AUT"), // Austria
            Map.entry("CH", "CHE"), // Switzerland
            Map.entry("PT", "PRT"), // Portugal
            Map.entry("SE", "SWE"), // Sweden
            Map.entry("NO", "NOR"), // Norway
            Map.entry("DK", "DNK"), // Denmark
            Map.entry("FI", "FIN"), // Finland
            Map.entry("PL", "POL"), // Poland
            Map.entry("CZ", "CZE"), // Czech Republic
            // North America
            Map.entry("US", "USA"), // United States
            Map.entry("CA", "CAN"), // Canada
            Map.entry("MX", "MEX"), // Mexico
            // South America
            Map.entry("BR", "BRA"), // Brazil
            Map.entry("AR", "ARG"), // Argentina
            Map.entry("CO", "COL"), // Colombia
            Map.entry("PE", "PER"), // Peru
            Map.entry("CL", "CHL"), // Chile
            Map.entry("VE", "VEN"), // Venezuela
            // Asia
            Map.entry("JP", "JPN"), // Japan
            Map.entry("KR", "KOR"), // South Korea
            Map.entry("CN", "CHN"), // China
            Map.entry("IN", "IND"), // India
            Map.entry("TH", "THA"), // Thailand
            Map.entry("SG", "SGP"), // Singapore
            Map.entry("MY", "MYS"), // Malaysia
            Map.entry("PH", "PHL"), // Philippines
            Map.entry("ID", "IDN"), // Indonesia
            Map.entry("TW", "TWN"), // Taiwan
            // Oceania
            Map.entry("AU", "AUS"), // Australia
            Map.entry("NZ", "NZL"), // New Zealand
            // Middle East
            Map.entry("AE", "ARE"), // United Arab Emirates
            Map.entry("SA", "SAU"), // Saudi Arabia
            Map.entry("IL", "ISR"), // Israel
            Map.entry("TR", "TUR"), // Turkey
            // Africa
            Map.entry("ZA", "ZAF"), // South Africa
            Map.entry("EG", "EGY"), // Egypt
            Map.entry("MA", "MAR"), // Morocco
            Map.entry("NG", "NGA"), // Nigeria
            Map.entry("KE", "KEN"), // Kenya
            Map.entry("GH", "GHA")  // Ghana
    );

    // Worldwide countries and their major cities (excluding Europe)
    private static final Map<String, Map<String, double[]>> WORLDWIDE_COUNTRIES = Map.ofEntries(
            // North America
            Map.entry("US", Map.ofEntries( // United States
                    Map.entry("New York", new double[] { 40.7128, -74.0060 }),
                    Map.entry("Los Angeles", new double[] { 34.0522, -118.2437 }),
                    Map.entry("Chicago", new double[] { 41.8781, -87.6298 }),
                    Map.entry("Houston", new double[] { 29.7604, -95.3698 }),
                    Map.entry("Phoenix", new double[] { 33.4484, -112.0740 }),
                    Map.entry("Philadelphia", new double[] { 39.9526, -75.1652 }),
                    Map.entry("San Antonio", new double[] { 29.4241, -98.4936 }),
                    Map.entry("San Diego", new double[] { 32.7157, -117.1611 }),
                    Map.entry("Dallas", new double[] { 32.7767, -96.7970 }),
                    Map.entry("San Jose", new double[] { 37.3382, -121.8863 })
            )),
            Map.entry("CA", Map.ofEntries( // Canada
                    Map.entry("Toronto", new double[] { 43.6532, -79.3832 }),
                    Map.entry("Montreal", new double[] { 45.5017, -73.5673 }),
                    Map.entry("Vancouver", new double[] { 49.2827, -123.1207 }),
                    Map.entry("Calgary", new double[] { 51.0447, -114.0719 }),
                    Map.entry("Edmonton", new double[] { 53.5444, -113.4909 }),
                    Map.entry("Ottawa", new double[] { 45.4215, -75.6972 }),
                    Map.entry("Winnipeg", new double[] { 49.8951, -97.1384 }),
                    Map.entry("Quebec City", new double[] { 46.8139, -71.2080 }),
                    Map.entry("Hamilton", new double[] { 43.2557, -79.8711 }),
                    Map.entry("Kitchener", new double[] { 43.4516, -80.4925 })
            )),
            Map.entry("MX", Map.ofEntries( // Mexico
                    Map.entry("Mexico City", new double[] { 19.4326, -99.1332 }),
                    Map.entry("Guadalajara", new double[] { 20.6597, -103.3496 }),
                    Map.entry("Monterrey", new double[] { 25.6866, -100.3161 }),
                    Map.entry("Puebla", new double[] { 19.0414, -98.2063 }),
                    Map.entry("Tijuana", new double[] { 32.5149, -117.0382 }),
                    Map.entry("León", new double[] { 21.1236, -101.6809 }),
                    Map.entry("Juárez", new double[] { 31.6904, -106.4245 }),
                    Map.entry("Torreón", new double[] { 25.5428, -103.4068 }),
                    Map.entry("Querétaro", new double[] { 20.5888, -100.3899 }),
                    Map.entry("Mérida", new double[] { 20.9674, -89.5926 })
            )),
            // South America
            Map.entry("BR", Map.ofEntries( // Brazil
                    Map.entry("São Paulo", new double[] { -23.5505, -46.6333 }),
                    Map.entry("Rio de Janeiro", new double[] { -22.9068, -43.1729 }),
                    Map.entry("Brasília", new double[] { -15.7942, -47.8822 }),
                    Map.entry("Salvador", new double[] { -12.9714, -38.5014 }),
                    Map.entry("Fortaleza", new double[] { -3.7319, -38.5267 }),
                    Map.entry("Belo Horizonte", new double[] { -19.9191, -43.9386 }),
                    Map.entry("Manaus", new double[] { -3.1190, -60.0217 }),
                    Map.entry("Curitiba", new double[] { -25.4284, -49.2733 }),
                    Map.entry("Recife", new double[] { -8.0476, -34.8770 }),
                    Map.entry("Porto Alegre", new double[] { -30.0346, -51.2177 })
            )),
            Map.entry("AR", Map.ofEntries( // Argentina
                    Map.entry("Buenos Aires", new double[] { -34.6118, -58.3966 }),
                    Map.entry("Córdoba", new double[] { -31.4201, -64.1888 }),
                    Map.entry("Rosario", new double[] { -32.9468, -60.6393 }),
                    Map.entry("Mendoza", new double[] { -32.8895, -68.8458 }),
                    Map.entry("San Miguel de Tucumán", new double[] { -26.8241, -65.2226 }),
                    Map.entry("La Plata", new double[] { -34.9215, -57.9545 }),
                    Map.entry("Mar del Plata", new double[] { -38.0055, -57.5426 }),
                    Map.entry("Salta", new double[] { -24.7859, -65.4117 }),
                    Map.entry("Santa Fe", new double[] { -31.6107, -60.6973 }),
                    Map.entry("San Juan", new double[] { -31.5375, -68.5364 })
            )),
            // Asia
            Map.entry("JP", Map.ofEntries( // Japan
                    Map.entry("Tokyo", new double[] { 35.6762, 139.6503 }),
                    Map.entry("Yokohama", new double[] { 35.4437, 139.6380 }),
                    Map.entry("Osaka", new double[] { 34.6937, 135.5023 }),
                    Map.entry("Nagoya", new double[] { 35.1815, 136.9066 }),
                    Map.entry("Sapporo", new double[] { 43.0618, 141.3545 }),
                    Map.entry("Fukuoka", new double[] { 33.5904, 130.4017 }),
                    Map.entry("Kobe", new double[] { 34.6901, 135.1955 }),
                    Map.entry("Kawasaki", new double[] { 35.5308, 139.7030 }),
                    Map.entry("Kyoto", new double[] { 35.0116, 135.7681 }),
                    Map.entry("Saitama", new double[] { 35.8617, 139.6453 })
            )),
            Map.entry("KR", Map.ofEntries( // South Korea
                    Map.entry("Seoul", new double[] { 37.5665, 126.9780 }),
                    Map.entry("Busan", new double[] { 35.1796, 129.0756 }),
                    Map.entry("Incheon", new double[] { 37.4563, 126.7052 }),
                    Map.entry("Daegu", new double[] { 35.8714, 128.6014 }),
                    Map.entry("Daejeon", new double[] { 36.3504, 127.3845 }),
                    Map.entry("Gwangju", new double[] { 35.1595, 126.8526 }),
                    Map.entry("Suwon", new double[] { 37.2636, 127.0286 }),
                    Map.entry("Goyang", new double[] { 37.6584, 126.8320 }),
                    Map.entry("Seongnam", new double[] { 37.4449, 127.1389 }),
                    Map.entry("Ulsan", new double[] { 35.5384, 129.3114 })
            )),
            Map.entry("CN", Map.ofEntries( // China
                    Map.entry("Shanghai", new double[] { 31.2304, 121.4737 }),
                    Map.entry("Beijing", new double[] { 39.9042, 116.4074 }),
                    Map.entry("Shenzhen", new double[] { 22.3193, 114.1694 }),
                    Map.entry("Guangzhou", new double[] { 23.1291, 113.2644 }),
                    Map.entry("Dongguan", new double[] { 23.0207, 113.7518 }),
                    Map.entry("Tianjin", new double[] { 39.3434, 117.3616 }),
                    Map.entry("Wuhan", new double[] { 30.5928, 114.3055 }),
                    Map.entry("Chengdu", new double[] { 30.5728, 104.0668 }),
                    Map.entry("Nanjing", new double[] { 32.0603, 118.7969 }),
                    Map.entry("Xi'an", new double[] { 34.3416, 108.9398 })
            )),
            Map.entry("IN", Map.ofEntries( // India
                    Map.entry("Mumbai", new double[] { 19.0760, 72.8777 }),
                    Map.entry("Delhi", new double[] { 28.7041, 77.1025 }),
                    Map.entry("Bangalore", new double[] { 12.9716, 77.5946 }),
                    Map.entry("Hyderabad", new double[] { 17.3850, 78.4867 }),
                    Map.entry("Ahmedabad", new double[] { 23.0225, 72.5714 }),
                    Map.entry("Chennai", new double[] { 13.0827, 80.2707 }),
                    Map.entry("Kolkata", new double[] { 22.5726, 88.3639 }),
                    Map.entry("Surat", new double[] { 21.1702, 72.8311 }),
                    Map.entry("Pune", new double[] { 18.5204, 73.8567 }),
                    Map.entry("Jaipur", new double[] { 26.9124, 75.7873 })
            )),
            // Oceania
            Map.entry("AU", Map.ofEntries( // Australia
                    Map.entry("Sydney", new double[] { -33.8688, 151.2093 }),
                    Map.entry("Melbourne", new double[] { -37.8136, 144.9631 }),
                    Map.entry("Brisbane", new double[] { -27.4698, 153.0251 }),
                    Map.entry("Perth", new double[] { -31.9505, 115.8605 }),
                    Map.entry("Adelaide", new double[] { -34.9285, 138.6007 }),
                    Map.entry("Gold Coast", new double[] { -28.0167, 153.4000 }),
                    Map.entry("Canberra", new double[] { -35.2809, 149.1300 }),
                    Map.entry("Newcastle", new double[] { -32.9283, 151.7817 }),
                    Map.entry("Wollongong", new double[] { -34.4278, 150.8931 }),
                    Map.entry("Logan City", new double[] { -27.6392, 153.1094 })
            )),
            // Middle East
            Map.entry("AE", Map.ofEntries( // United Arab Emirates
                    Map.entry("Dubai", new double[] { 25.2048, 55.2708 }),
                    Map.entry("Abu Dhabi", new double[] { 24.4539, 54.3773 }),
                    Map.entry("Sharjah", new double[] { 25.3463, 55.4209 }),
                    Map.entry("Al Ain", new double[] { 24.1302, 55.8023 }),
                    Map.entry("Ajman", new double[] { 25.4052, 55.5136 }),
                    Map.entry("Ras Al Khaimah", new double[] { 25.6741, 55.9804 }),
                    Map.entry("Fujairah", new double[] { 25.1288, 56.3265 }),
                    Map.entry("Umm Al Quwain", new double[] { 25.5647, 55.5552 }),
                    Map.entry("Khor Fakkan", new double[] { 25.3392, 56.3018 }),
                    Map.entry("Dibba Al-Fujairah", new double[] { 25.5858, 56.2479 })
            )),
            Map.entry("TR", Map.ofEntries( // Turkey
                    Map.entry("Istanbul", new double[] { 41.0082, 28.9784 }),
                    Map.entry("Ankara", new double[] { 39.9334, 32.8597 }),
                    Map.entry("İzmir", new double[] { 38.4237, 27.1428 }),
                    Map.entry("Bursa", new double[] { 40.1885, 29.0610 }),
                    Map.entry("Antalya", new double[] { 36.8969, 30.7133 }),
                    Map.entry("Konya", new double[] { 37.8714, 32.4846 }),
                    Map.entry("Adana", new double[] { 36.9862, 35.3250 }),
                    Map.entry("Gaziantep", new double[] { 37.0662, 37.3833 }),
                    Map.entry("Kocaeli", new double[] { 40.8533, 29.8815 }),
                    Map.entry("Mercin", new double[] { 36.7953, 34.6179 })
            )),
            // Africa
            Map.entry("EG", Map.ofEntries( // Egypt
                    Map.entry("Cairo", new double[] { 30.0444, 31.2357 }),
                    Map.entry("Alexandria", new double[] { 31.2001, 29.9187 }),
                    Map.entry("Giza", new double[] { 30.0131, 31.2089 }),
                    Map.entry("Shubra El-Kheima", new double[] { 30.1286, 31.2422 }),
                    Map.entry("Port Said", new double[] { 31.2565, 32.2841 }),
                    Map.entry("Suez", new double[] { 29.9737, 32.5263 }),
                    Map.entry("Luxor", new double[] { 25.6872, 32.6396 }),
                    Map.entry("Mansoura", new double[] { 31.0364, 31.3807 }),
                    Map.entry("Tanta", new double[] { 30.7885, 31.0019 }),
                    Map.entry("Asyut", new double[] { 27.1801, 31.1837 })
            )),
            // Asia Pacific
            Map.entry("SG", Map.ofEntries( // Singapore
                    Map.entry("Singapore", new double[] { 1.3521, 103.8198 })
            )),
            Map.entry("HK", Map.ofEntries( // Hong Kong
                    Map.entry("Hong Kong", new double[] { 22.3193, 114.1694 })
            ))
    );

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
        int countriesProcessed = 0;

        logger.info("Starting HERE Places shop population for WORLDWIDE major cities (dryRun: {})", dryRun);

        String[] searchQueries = {
                // English
                "comic book store", "trading card game", "pokemon store", "magic the gathering",
                "board game store", "hobby shop", "game store",
                // French
                "magasin de bandes dessinées", "magasin de jeux", "magasin de hobby",
                "magasin de cartes à collectionner", "pokemon boutique",
                // German
                "comicshop", "spielwarengeschäft", "hobbygeschäft", "kartenspiel",
                "pokemon laden", "magic the gathering laden",
                // Spanish
                "tienda de cómics", "tienda de juegos", "tienda de hobby",
                "juegos de mesa", "pokemon tienda",
                // Dutch
                "stripwinkel", "spelletjeswinkel", "hobbywinkel", "pokemon winkel",
                // Swedish
                "serietidningsaffär", "spelaffär", "hobbyaffär", "pokemon butik",
                // Norwegian
                "tegneseriebutikk", "spillbutikk", "hobbybutikk", "pokemon butikk",
                // Danish
                "tegneseriebutik", "spilbutik", "hobbybutik", "pokemon butik",
                // Finnish
                "sarjakuvakauppa", "pelikauppa", "harrastekauppa", "pokemon kauppa",
                // Polish
                "sklep z komiksami", "sklep z grami", "sklep hobbystyczny", "pokemon sklep",
                // Czech
                "obchod s komiksy", "hračka", "hobby obchod", "pokemon obchod"
        };

        for (Map.Entry<String, Map<String, double[]>> countryEntry : WORLDWIDE_COUNTRIES.entrySet()) {
            String countryCode = countryEntry.getKey();
            Map<String, double[]> cities = countryEntry.getValue();

            logger.info("Searching in country: {} ({}/{})", countryCode, ++countriesProcessed, WORLDWIDE_COUNTRIES.size());

            for (Map.Entry<String, double[]> cityEntry : cities.entrySet()) {
                String cityName = cityEntry.getKey();
                double[] coords = cityEntry.getValue();

                logger.info("Searching in: {} ({})", cityName, countryCode);

                for (String query : searchQueries) {
                    try {
                        List<Shop> shops = discoverShops(coords[0], coords[1], query, countryCode);

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
                        String errorMsg = "Error searching " + cityName + " (" + countryCode + ") for '" + query + "': " + e.getMessage();
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

            // Delay between countries
            try {
                Thread.sleep(1000);
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
    private List<Shop> discoverShops(double lat, double lng, String query, String countryCodeIso2) {
        List<Shop> shops = new ArrayList<>();

        try {
            // Convert ISO-2 to ISO-3 for HERE API
            String countryCodeIso3 = ISO2_TO_ISO3.getOrDefault(countryCodeIso2, countryCodeIso2);

            // q=query text, at=location
            // Use Locale.US to ensure decimal point instead of comma
            String url = String.format(Locale.US,
                    "%s?at=%.6f,%.6f&q=%s&in=countryCode:%s&limit=50&apiKey=%s",
                    HERE_DISCOVER_URL, lat, lng, query, countryCodeIso3, apiKey);

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
        if (lowerName.contains("lorcana")) {
            tcgTypes.add("LORCANA");
        }

        // If no specific TCG detected, assume all (generic card shop)
        if (tcgTypes.isEmpty()) {
            tcgTypes.addAll(Arrays.asList("POKEMON", "MAGIC", "YUGIOH", "ONE_PIECE", "LORCANA"));
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
