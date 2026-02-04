package com.tcg.arena.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.ExpansionRepository;
import com.tcg.arena.repository.TCGSetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Client for TCG API - provides real-time pricing data for TCGs
 * Supports: MTG, Pokemon, Yu-Gi-Oh!, Lorcana, One Piece, Digimon
 * 
 * Features:
 * - Full hierarchy management (Expansion → TCGSet → CardTemplate)
 * - Paginated requests with cursor support
 * - Duplicate prevention at all levels
 * - Rate limiting to avoid API throttling
 */
@Service
public class TCGApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TCGApiClient.class);

    // Rate limiting: delay between API calls (ms) -> 5s to avoid 429 rate limit
    private static final long API_DELAY_MS = 5000;
    // Page size for card fetching
    private static final int PAGE_SIZE = 20;
    // Progress logging interval
    private static final int LOG_PROGRESS_EVERY_N_PAGES = 10;

    // ===================== Import Statistics Tracker =====================

    /**
     * Internal class to track import statistics for structured logging
     */
    private static class ImportStats {
        final String tcgName;
        final String gameId;
        final int startOffset;
        final long startTimeMs;

        int pagesProcessed = 0;
        int totalCardsProcessed = 0;
        int newCardsSaved = 0;
        int pricesUpdated = 0;
        int errors = 0;
        int currentOffset = 0;
        int retryCount = 0;
        boolean completed = false;

        ImportStats(String tcgName, String gameId, int startOffset) {
            this.tcgName = tcgName;
            this.gameId = gameId;
            this.startOffset = startOffset;
            this.currentOffset = startOffset;
            this.startTimeMs = System.currentTimeMillis();
        }

        long getElapsedSeconds() {
            return (System.currentTimeMillis() - startTimeMs) / 1000;
        }

        double getCardsPerSecond() {
            long elapsed = getElapsedSeconds();
            return elapsed > 0 ? (double) totalCardsProcessed / elapsed : 0;
        }

        String formatElapsedTime() {
            long seconds = getElapsedSeconds();
            if (seconds < 60) return seconds + "s";
            if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
            return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
        }
    }

    private final WebClient webClient;
    private final WebClient scryfallWebClient;

    // Cache to avoid repeated DB lookups during import
    private final Map<String, Expansion> expansionCache = new ConcurrentHashMap<>();
    private final Map<String, com.tcg.arena.model.TCGSet> tcgSetCache = new ConcurrentHashMap<>();

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private TCGSetRepository tcgSetRepository;

    @Autowired
    private com.tcg.arena.repository.ImportProgressRepository importProgressRepository;

    @Value("${tcg.api.key}")
    private String apiKeyPrimary;

    @Value("${tcg.api.key.secondary:tcg_b23de539c2c8414e854e73c449bf0e84}")
    private String apiKeySecondary;

    @Value("${tcg.api.key.tertiary:tcg_938debac20584990a7354ad04c4f068d}")
    private String apiKeyTertiary;

    // Track which API key is currently active
    private volatile int activeKeyIndex = 0; // 0=primary, 1=secondary, 2=tertiary
    private volatile long lastKeySwitch = 0;
    private static final long KEY_SWITCH_COOLDOWN_MS = 60 * 60 * 1000; // 1 hour cooldown before switching back

    /**
     * Get the current active API key.
     * Automatically switches back to primary key after cooldown period.
     */
    private String getActiveApiKey() {
        // Check if we should switch back to primary key after cooldown
        if (activeKeyIndex > 0 && (System.currentTimeMillis() - lastKeySwitch) > KEY_SWITCH_COOLDOWN_MS) {
            logger.info("[API KEY] Cooldown expired, switching back to PRIMARY key");
            activeKeyIndex = 0;
        }

        String key;
        String keyType;
        switch (activeKeyIndex) {
            case 0:
                key = apiKeyPrimary;
                keyType = "PRIMARY";
                break;
            case 1:
                key = apiKeySecondary;
                keyType = "SECONDARY";
                break;
            case 2:
                key = apiKeyTertiary;
                keyType = "TERTIARY";
                break;
            default:
                key = apiKeyPrimary;
                keyType = "PRIMARY";
                activeKeyIndex = 0;
        }

        return key;
    }

    /**
     * Switch to the next available API key when rate limited.
     * Cycles through: Primary -> Secondary -> Tertiary -> Primary (if all exhausted)
     * Returns true if switch was successful (moved to a different key).
     */
    private synchronized boolean switchToNextApiKey() {
        int previousIndex = activeKeyIndex;
        activeKeyIndex = (activeKeyIndex + 1) % 3; // Cycle through 0, 1, 2

        String newKeyType;
        switch (activeKeyIndex) {
            case 0: newKeyType = "PRIMARY"; break;
            case 1: newKeyType = "SECONDARY"; break;
            case 2: newKeyType = "TERTIARY"; break;
            default: newKeyType = "PRIMARY";
        }

        logger.warn("[API KEY] Rate limit hit! Switching to {} API key (was {})",
                newKeyType, previousIndex == 0 ? "PRIMARY" : previousIndex == 1 ? "SECONDARY" : "TERTIARY");
        lastKeySwitch = System.currentTimeMillis();

        return activeKeyIndex != previousIndex;
    }

    /**
     * Check if error is a rate limit and switch key if needed.
     * Returns true if should retry with new key (i.e., we switched keys).
     * Returns false if we were already on tertiary key (no point retrying).
     */
    private boolean handleRateLimitError(Throwable throwable) {
        if (throwable instanceof RuntimeException) {
            String message = throwable.getMessage();
            if (message != null && message.contains("HTTP 429")) {
                return switchToNextApiKey(); // Only retry if we actually switched
            }
        }
        return false;
    }

    // Mapping from internal TCGType to TCG game IDs (from /games endpoint)
    private static final Map<TCGType, String> TCG_TYPE_TO_GAME_ID = Map.ofEntries(
            Map.entry(TCGType.MAGIC, "magic-the-gathering"),
            Map.entry(TCGType.POKEMON, "pokemon"),
            Map.entry(TCGType.YUGIOH, "yugioh"),
            Map.entry(TCGType.LORCANA, "disney-lorcana"),
            Map.entry(TCGType.ONE_PIECE, "one-piece-card-game"),
            Map.entry(TCGType.DIGIMON, "digimon-card-game"),
            Map.entry(TCGType.RIFTBOUND, "riftbound-league-of-legends-trading-card-game"),
            Map.entry(TCGType.UNION_ARENA, "union-arena"),
            Map.entry(TCGType.DRAGON_BALL_SUPER_FUSION_WORLD, "dragon-ball-super-fusion-world"),
            Map.entry(TCGType.FLESH_AND_BLOOD, "flesh-and-blood-tcg"));

    public TCGApiClient(@Value("${tcg.api.base-url}") String baseUrl,
                        @Value("${scryfall.api.base-url}") String scryfallBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024)) // 50MB buffer
                .build();
        this.scryfallWebClient = WebClient.builder()
                .baseUrl(scryfallBaseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(100 * 1024 * 1024)) // 100MB buffer for bulk data
                .build();
    }

    // ===================== DTO classes for TCG API responses
    // =====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGGame {
        public String id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGSet {
        public String id;
        public String name;
        @JsonProperty("game_id")
        public String gameId;
        public String game;
        public Integer count;
        @JsonProperty("cards_count")
        public Integer cardsCount;
        @JsonProperty("release_date")
        public String releaseDate;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGCard {
        public String id;
        public String name;
        public String game;
        public String set;
        @JsonProperty("set_name")
        public String setName;
        public String number;
        @JsonProperty("tcgplayerId")
        public String tcgplayerId;
        public String rarity;
        public String details;
        @JsonProperty("imageUrl")
        public String imageUrl;
        public List<TCGVariant> variants;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGVariant {
        public String id;
        public String printing;
        public String condition;
        public Double price;
        public Long lastUpdated;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGCardsResponse {
        @JsonProperty("data")
        public List<TCGCard> data;
        public List<TCGCard> cards; // fallback
        @JsonProperty("hasMore")
        public boolean hasMore;
        @JsonProperty("nextCursor")
        public String nextCursor;
        public Integer total;
        // Transient field for internal pagination tracking
        public int currentOffset;

        public List<TCGCard> getCards() {
            return data != null ? data : (cards != null ? cards : Collections.emptyList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TCGSetsResponse {
        @JsonProperty("data")
        public List<TCGSet> data;
        public List<TCGSet> sets; // fallback
        @JsonProperty("hasMore")
        public boolean hasMore;
        @JsonProperty("nextCursor")
        public String nextCursor;

        public List<TCGSet> getSets() {
            return data != null ? data : (sets != null ? sets : Collections.emptyList());
        }
    }

    // ===================== DTO classes for Scryfall API responses =====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallSet {
        public String id; // Scryfall set ID
        public String code; // Set code like "m21"
        public String name;
        public String released_at; // Date
        public int card_count;
        public String set_type;
        public String block;
        public String parent_set_code;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallSetsResponse {
        public List<ScryfallSet> data;
        public boolean has_more;
        public String next_page;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallCard {
        public String id; // Scryfall UUID
        public String name;
        public String set;
        public String set_name;
        public String collector_number;
        public String rarity;
        public String oracle_text;
        public String type_line;
        public String mana_cost;
        public ScryfallImageUris image_uris;
        public List<ScryfallCardFace> card_faces; // For double-faced cards
        public boolean digital; // Skip digital cards
        public boolean oversized; // Skip oversized
        public boolean reserved; // Reserved list
        public boolean reprint;
        public String released_at;
        public ScryfallPrices prices;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallImageUris {
        public String normal;
        public String large;
        public String small;
        public String png;
        public String art_crop;
        public String border_crop;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallCardFace {
        public String type_line;
        public String oracle_text;
        public String mana_cost;
        public ScryfallImageUris image_uris;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallPrices {
        public String usd;
        public String usd_foil;
        public String eur;
        public String eur_foil;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallBulkData {
        public String download_uri;
        public String type;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallBulkDataResponse {
        public List<ScryfallBulkData> data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ScryfallSearchResponse {
        public List<ScryfallCard> data;
        public boolean has_more;
        public String next_page;
        public int total_cards;
        public Object warnings; // Scryfall sometimes includes warnings
    }

    // ===================== API Methods =====================

    /**
     * Get all available games from TCG to discover valid game IDs
     */
    public Mono<List<TCGGame>> getGames() {
        return webClient.get()
                .uri("/games")
                .header("x-api-key", getActiveApiKey())
                .retrieve()
                .bodyToMono(TCGGame[].class)
                .map(games -> List.of(games))
                .doOnSuccess(games -> logger.info("Available TCG games: {}",
                        games.stream().map(g -> g.id + " (" + g.name + ")").toList()))
                .onErrorResume(e -> {
                    logger.error("Error fetching games: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    /**
     * Get sets for a specific game with pagination
     */
    public Flux<TCGSet> getAllSets(String gameId) {
        return getSetsPage(gameId, null)
                .expand(response -> {
                    if (response.hasMore && response.nextCursor != null) {
                        return getSetsPage(gameId, response.nextCursor)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    return Mono.empty();
                })
                .flatMapIterable(response -> response.getSets());
    }

    private Mono<TCGSetsResponse> getSetsPage(String gameId, String cursor) {
        // Use Mono.defer to rebuild the request on each retry (so the API key is re-evaluated)
        return Mono.defer(() -> webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/sets").queryParam("game", gameId);
                    if (cursor != null) {
                        builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .header("x-api-key", getActiveApiKey())
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("[TCG API ERROR] getSetsPage for {}: HTTP {} - Response body: {}",
                                        gameId, response.statusCode().value(), body);
                                // Don't switch key here - let retryWhen handle it
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGSetsResponse.class)
                .timeout(Duration.ofMinutes(10)) // Add timeout to prevent hanging requests
                .doOnSuccess(resp -> logger.info("Fetched sets for {}: {} sets found, hasMore: {}",
                        gameId, resp.getSets().size(), resp.hasMore))
        ).retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofSeconds(5))
                    .maxBackoff(Duration.ofMinutes(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            if (message != null && message.contains("HTTP 429")) {
                                // Only retry if we successfully switched keys
                                // If already on tertiary, don't retry (all keys exhausted)
                                return switchToNextApiKey();
                            }
                            // Always retry 500 errors
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> {
                        String keyType = switch (activeKeyIndex) {
                            case 0 -> "PRIMARY";
                            case 1 -> "SECONDARY";
                            case 2 -> "TERTIARY";
                            default -> "UNKNOWN";
                        };
                        logger.warn("Retrying getSetsPage for {} - attempt {} ({}) [Key: {}]",
                            gameId, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage(),
                            keyType);
                    })
                )
                .onErrorResume(e -> {
                    logger.error("Error fetching sets for {}: {}", gameId, e.getMessage(), e);
                    return Mono.just(new TCGSetsResponse());
                });
    }

    /**
     * Get all cards for a set with offset-based pagination.
     * Continues fetching until zero results are returned.
     */
    public Flux<TCGCard> getAllCardsForSet(String setId) {
        return getCardsPageBySet(setId, 0)
                .expand(response -> {
                    List<TCGCard> cards = response.getCards();
                    // Continue until we get zero results
                    if (!cards.isEmpty()) {
                        int nextOffset = response.currentOffset + PAGE_SIZE;
                        return getCardsPageBySet(setId, nextOffset)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    logger.debug("[API] Set {} pagination complete at offset {}", setId, response.currentOffset);
                    return Mono.empty();
                })
                .flatMapIterable(response -> response.getCards());
    }

    /**
     * Get all cards for a game (not a set) with pagination
     * TCG API requires game parameter, not set
     */
    /**
     * Get all cards for a game (not a set) with pagination
     * TCG API requires game parameter, not set
     * Uses offset-based pagination
     */
    /**
     * Get all card pages for a game starting from a specific offset
     * Returns Flux of TCGCardsResponse to allow processing per page
     */
    public Flux<TCGCardsResponse> getCardPagesForGame(String gameId, int startOffset) {
        logger.debug("[API] Starting pagination for game: {}, offset: {}", gameId, startOffset);
        return getCardsPageByGame(gameId, startOffset)
                .expand(response -> {
                    List<TCGCard> cards = response.getCards();
                    if (response.hasMore && !cards.isEmpty()) {
                        int nextOffset = response.currentOffset + PAGE_SIZE;
                        return getCardsPageByGame(gameId, nextOffset)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    logger.debug("[API] End of pagination reached at offset: {}", response.currentOffset);
                    return Mono.empty();
                });
    }

    /**
     * Legacy method helper (fetches all from 0)
     */
    public Flux<TCGCard> getAllCardsForGame(String gameId) {
        return getCardPagesForGame(gameId, 0)
                .flatMapIterable(TCGCardsResponse::getCards);
    }

    private Mono<TCGCardsResponse> getCardsPageByGame(String gameId, int offset) {
        // Use Mono.defer to rebuild the request on each retry, so the API key is re-evaluated
        return Mono.defer(() -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("game", gameId)
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .build())
                .header("x-api-key", getActiveApiKey())
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("[API] HTTP {} for {} at offset {} - {}",
                                        response.statusCode().value(), gameId, offset, body);
                                // Don't switch key here - let retryWhen handle it
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGCardsResponse.class)
                .map(response -> {
                    response.currentOffset = offset;
                    return response;
                })
                .timeout(Duration.ofMinutes(10))
        ).retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofSeconds(5))
                    .maxBackoff(Duration.ofMinutes(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            if (message != null && message.contains("HTTP 429")) {
                                // Only retry if we successfully switched keys
                                // If already on tertiary, don't retry (all keys exhausted)
                                return switchToNextApiKey();
                            }
                            // Always retry 500 errors
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> {
                        String keyType = switch (activeKeyIndex) {
                            case 0 -> "PRIMARY";
                            case 1 -> "SECONDARY";
                            case 2 -> "TERTIARY";
                            default -> "UNKNOWN";
                        };
                        logger.warn("[API] RETRY {}/5 for {} at offset {} - {} [Key: {}]",
                            retrySignal.totalRetries() + 1, gameId, offset,
                            retrySignal.failure().getMessage().substring(0, Math.min(50, retrySignal.failure().getMessage().length())),
                            keyType);
                    })
                )
                .onErrorResume(e -> {
                    logger.error("[API] FAILED for {} at offset {}: {}", gameId, offset, e.getMessage());
                    TCGCardsResponse errorResponse = new TCGCardsResponse();
                    errorResponse.currentOffset = offset;
                    return Mono.just(errorResponse);
                });
    }

    /**
     * Fetch a page of cards for a set using offset-based pagination
     */
    private Mono<TCGCardsResponse> getCardsPageBySet(String setId, int offset) {
        // Use Mono.defer to rebuild the request on each retry, so the API key is re-evaluated
        return Mono.defer(() -> webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("set", setId)
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .build())
                .header("x-api-key", getActiveApiKey())
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("[TCG API ERROR] getCardsPageBySet for set {}: HTTP {} - Response body: {}",
                                        setId, response.statusCode().value(), body);
                                // Don't switch key here - let retryWhen handle it
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGCardsResponse.class)
                .timeout(Duration.ofMinutes(10))
                .doOnSuccess(resp -> {
                    resp.currentOffset = offset;
                    logger.debug("Fetched cards page for set {}, offset: {}, count: {}, hasMore: {}",
                            setId, offset, resp.getCards().size(), resp.hasMore);
                })
        ).retryWhen(reactor.util.retry.Retry.backoff(5, Duration.ofSeconds(5))
                    .maxBackoff(Duration.ofMinutes(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            if (message != null && message.contains("HTTP 429")) {
                                // Only retry if we successfully switched keys
                                // If already on tertiary, don't retry (all keys exhausted)
                                return switchToNextApiKey();
                            }
                            // Always retry 500 errors
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> {
                        String keyType = switch (activeKeyIndex) {
                            case 0 -> "PRIMARY";
                            case 1 -> "SECONDARY";
                            case 2 -> "TERTIARY";
                            default -> "UNKNOWN";
                        };
                        logger.warn("Retrying getCardsPageBySet for set {} - attempt {} ({}) [Key: {}]",
                            setId, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage(),
                            keyType);
                    })
                )
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for set {}: {}", setId, e.getMessage(), e);
                    TCGCardsResponse errorResponse = new TCGCardsResponse();
                    errorResponse.currentOffset = offset;
                    return Mono.just(errorResponse);
                });
    }

    // ===================== Import Logic =====================

    /**
     * Import cards for NEW sets and EMPTY sets (sets with 0 cards) for a specific TCG type.
     * Instead of offset-based pagination, this method:
     * 1. Fetches all sets from the API
     * 2. Identifies which sets are NEW (not in database) or EMPTY (in database but with 0 cards)
     * 3. Identifies sets with MISSING CARDS (cardCount in DB differs from actual cards)
     * 4. For each new/empty set, fetches all cards using the set parameter
     * 5. For sets with missing cards, only imports the delta (cards not already in DB)
     * 6. If a card fails to save, continues with the next card (no interruption)
     */
    public Mono<Integer> importCardsForTCG(TCGType tcgType) {
        // Special handling for Magic using Scryfall
        if (tcgType == TCGType.MAGIC) {
            return importMagicCards();
        }

        String gameId = TCG_TYPE_TO_GAME_ID.get(tcgType);
        if (gameId == null) {
            logger.warn("[IMPORT] TCG type {} not supported", tcgType);
            return Mono.just(0);
        }

        // Clear caches at start of import
        expansionCache.clear();
        tcgSetCache.clear();

        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ IMPORT START (NEW + EMPTY + DELTA SETS MODE): {}", tcgType.getDisplayName().toUpperCase());
        logger.info("╠══════════════════════════════════════════════════════════════");
        logger.info("║ Game ID: {}", gameId);
        logger.info("╚══════════════════════════════════════════════════════════════");

        // Track statistics
        final ImportStats stats = new ImportStats(tcgType.getDisplayName(), gameId, 0);

        // Load existing setCode from database for this TCG type
        Set<String> existingSetCodes = tcgSetRepository.findAllSetCodesByTcgType(tcgType);
        logger.info("[IMPORT] [{}] Found {} existing sets in database", tcgType, existingSetCodes.size());

        // Load sets with 0 cards (empty sets that need card import)
        List<com.tcg.arena.model.TCGSet> emptySetsInDb = tcgSetRepository.findEmptySetsByTcgType(tcgType);
        logger.info("[IMPORT] [{}] Found {} existing sets with 0 cards", tcgType, emptySetsInDb.size());

        // Load sets with missing cards (cardCount differs from actual cards in DB)
        List<com.tcg.arena.model.TCGSet> setsWithMissingCards = tcgSetRepository.findSetsWithMissingCards(tcgType);
        logger.info("[IMPORT] [{}] Found {} sets with missing cards (need delta import)", tcgType, setsWithMissingCards.size());

        // Step 1: Fetch all sets from API
        return getAllSets(gameId)
                .collectList()
                .flatMap(apiSets -> {
                    logger.info("[IMPORT] [{}] PHASE 1: Fetched {} sets from API", tcgType, apiSets.size());

                    // Identify NEW sets (not in database)
                    List<TCGSet> newSets = apiSets.stream()
                            .filter(set -> !existingSetCodes.contains(set.id))
                            .toList();

                    // Identify empty sets from API (sets in DB with 0 cards)
                    List<TCGSet> emptyApiSets = apiSets.stream()
                            .filter(apiSet -> emptySetsInDb.stream()
                                    .anyMatch(dbSet -> dbSet.getSetCode().equals(apiSet.id)))
                            .toList();

                    // Identify sets with missing cards from API
                    List<TCGSet> deltaApiSets = apiSets.stream()
                            .filter(apiSet -> setsWithMissingCards.stream()
                                    .anyMatch(dbSet -> dbSet.getSetCode().equals(apiSet.id)))
                            .toList();

                    // Combine new sets and empty sets for FULL import
                    List<TCGSet> setsForFullImport = new java.util.ArrayList<>();
                    setsForFullImport.addAll(newSets);
                    setsForFullImport.addAll(emptyApiSets);

                    if (setsForFullImport.isEmpty() && deltaApiSets.isEmpty()) {
                        logger.info("[IMPORT] [{}] No new, empty, or delta sets found. Import complete.", tcgType);
                        return Mono.just(0);
                    }

                    logger.info("[IMPORT] [{}] PHASE 2: Found {} NEW sets, {} EMPTY sets, {} DELTA sets", 
                            tcgType, newSets.size(), emptyApiSets.size(), deltaApiSets.size());
                    
                    for (TCGSet set : newSets) {
                        logger.info("[IMPORT] [{}]   - NEW: {} ({})", tcgType, set.name, set.id);
                    }
                    for (TCGSet set : emptyApiSets) {
                        logger.info("[IMPORT] [{}]   - EMPTY: {} ({})", tcgType, set.name, set.id);
                    }
                    for (TCGSet set : deltaApiSets) {
                        logger.info("[IMPORT] [{}]   - DELTA: {} ({})", tcgType, set.name, set.id);
                    }

                    // Process full import sets, then delta import sets
                    Mono<Integer> fullImportMono = Flux.fromIterable(setsForFullImport)
                            .concatMap(apiSet -> importCardsForSet(apiSet, tcgType, stats))
                            .reduce(0, Integer::sum);

                    Mono<Integer> deltaImportMono = Flux.fromIterable(deltaApiSets)
                            .concatMap(apiSet -> importDeltaCardsForSet(apiSet, tcgType, stats))
                            .reduce(0, Integer::sum);

                    return fullImportMono
                            .flatMap(fullCount -> deltaImportMono.map(deltaCount -> fullCount + deltaCount))
                            .doOnSuccess(total -> {
                                stats.newCardsSaved = total;
                                logImportCompleteNewSets(tcgType, stats, setsForFullImport.size() + deltaApiSets.size());
                            });
                })
                .doOnError(e -> {
                    logger.error("[IMPORT] [{}] FATAL ERROR: {}", tcgType, e.getMessage(), e);
                })
                .doFinally(signal -> {
                    expansionCache.clear();
                    tcgSetCache.clear();
                });
    }

    /**
     * Reload a specific set from JustTCG API (delta import).
     * This is called from the admin dashboard to force reload a set.
     * It does NOT delete any existing cards, only adds missing ones.
     * 
     * @param dbSet The TCGSet from the database to reload
     * @return Map with reload results (newCards, skipped, errors)
     */
    public Map<String, Object> reloadSetFromApi(com.tcg.arena.model.TCGSet dbSet) {
        String setCode = dbSet.getSetCode();
        TCGType tcgType = dbSet.getExpansion() != null ? dbSet.getExpansion().getTcgType() : null;
        
        if (tcgType == null) {
            throw new RuntimeException("Set has no expansion or TCG type defined");
        }
        
        logger.info("[RELOAD] Starting reload for set '{}' (code: {}, tcg: {})", 
                dbSet.getName(), setCode, tcgType.getDisplayName());
        
        // Load existing card keys for this set (name|||setCode|||cardNumber)
        Set<String> existingCardKeys = cardTemplateRepository.findAllCardKeysBySetCode(setCode);
        logger.info("[RELOAD] Set '{}' has {} existing cards in DB", dbSet.getName(), existingCardKeys.size());
        
        final int[] savedCount = {0};
        final int[] skippedCount = {0};
        final int[] errorsCount = {0};
        
        // Fetch all cards for this set and only import missing ones
        getAllCardsForSet(setCode)
                .doOnNext(card -> {
                    if (card == null || card.name == null) {
                        errorsCount[0]++;
                        return;
                    }
                    
                    // Build composite key for this card
                    String cardKey = card.name + "|||" + setCode + "|||" + (card.number != null ? card.number : "");
                    
                    // Skip if card already exists
                    if (existingCardKeys.contains(cardKey)) {
                        skippedCount[0]++;
                        return;
                    }
                    
                    try {
                        if (saveCardIfNotExists(card, dbSet, tcgType)) {
                            savedCount[0]++;
                            logger.debug("[RELOAD] Saved new card: {} ({}) in set {}", 
                                    card.name, card.number, setCode);
                        } else {
                            skippedCount[0]++;
                        }
                    } catch (Exception e) {
                        errorsCount[0]++;
                        logger.warn("[RELOAD] Error saving card '{}' in set '{}': {}", 
                                card.name, setCode, e.getMessage());
                    }
                })
                .blockLast(Duration.ofMinutes(30)); // Block and wait for completion
        
        logger.info("[RELOAD] Set '{}' completed: {} new cards, {} skipped, {} errors",
                dbSet.getName(), savedCount[0], skippedCount[0], errorsCount[0]);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("setId", dbSet.getId());
        result.put("setCode", setCode);
        result.put("setName", dbSet.getName());
        result.put("newCards", savedCount[0]);
        result.put("skipped", skippedCount[0]);
        result.put("errors", errorsCount[0]);
        result.put("totalExisting", existingCardKeys.size());
        result.put("success", true);
        
        return result;
    }

    /**
     * Complete reset of a set from JustTCG API.
     * This will DELETE ALL existing card templates for the set and reload everything from scratch.
     * This is a destructive operation that cannot be undone.
     * 
     * @param dbSet The TCGSet from the database to reset
     * @return Map with reset results (deleted, imported, errors)
     */
    public Map<String, Object> resetSetFromApi(com.tcg.arena.model.TCGSet dbSet) {
        String setCode = dbSet.getSetCode();
        TCGType tcgType = dbSet.getExpansion() != null ? dbSet.getExpansion().getTcgType() : null;
        
        if (tcgType == null) {
            throw new RuntimeException("Set has no expansion or TCG type defined");
        }
        
        logger.info("[RESET] Starting COMPLETE reset for set '{}' (code: {}, tcg: {})", 
                dbSet.getName(), setCode, tcgType.getDisplayName());
        
        // Count existing cards before deletion
        int existingCardsCount = cardTemplateRepository.findAllCardKeysBySetCode(setCode).size();
        logger.info("[RESET] Set '{}' has {} existing cards in DB - deleting them all", 
                dbSet.getName(), existingCardsCount);
        
        // DELETE ALL existing card templates for this set
        int deletedCount = cardTemplateRepository.deleteBySetCode(setCode);
        logger.info("[RESET] Deleted {} card templates for set '{}'", deletedCount, dbSet.getName());
        
        // Now reload all cards from scratch using the existing import logic
        logger.info("[RESET] Starting full import for set '{}' from API", dbSet.getName());
        
        final int[] importedCount = {0};
        final int[] errorsCount = {0};
        
        // Use the existing importCardsForSet method to reload everything
        TCGSet apiSet = new TCGSet();
        apiSet.id = dbSet.getSetCode();
        apiSet.name = dbSet.getName();
        importCardsForSet(apiSet, tcgType, new ImportStats(tcgType.getDisplayName(), TCG_TYPE_TO_GAME_ID.get(tcgType), 0))
                .doOnNext(count -> importedCount[0] += count)
                .doOnError(error -> {
                    errorsCount[0]++;
                    logger.error("[RESET] Error importing cards for set '{}': {}", dbSet.getName(), error.getMessage());
                })
                .block(Duration.ofMinutes(30)); // Block and wait for completion
        
        logger.info("[RESET] Set '{}' reset completed: {} cards deleted, {} cards imported, {} errors",
                dbSet.getName(), deletedCount, importedCount[0], errorsCount[0]);
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("setId", dbSet.getId());
        result.put("setCode", setCode);
        result.put("setName", dbSet.getName());
        result.put("deletedCards", deletedCount);
        result.put("importedCards", importedCount[0]);
        result.put("errors", errorsCount[0]);
        result.put("previousTotal", existingCardsCount);
        result.put("success", true);
        
        return result;
    }

    /**
     * Import ONLY missing cards for a specific set (delta import).
     * Compares cards from API with existing cards in DB and only imports the difference.
     * Uses composite key (name + setCode + cardNumber) to identify missing cards.
     */
    private Mono<Integer> importDeltaCardsForSet(TCGSet apiSet, TCGType tcgType, ImportStats stats) {
        logger.info("[IMPORT] [{}] Starting DELTA import for set: {} ({})", tcgType, apiSet.name, apiSet.id);

        // Get the existing set from database
        com.tcg.arena.model.TCGSet tcgSet;
        try {
            tcgSet = getOrCreateTCGSet(apiSet, tcgType);
        } catch (Exception e) {
            logger.error("[IMPORT] [{}] Failed to get set '{}': {}", tcgType, apiSet.name, e.getMessage());
            return Mono.just(0);
        }

        final com.tcg.arena.model.TCGSet finalTcgSet = tcgSet;
        
        // Load existing card keys for this set (name|||setCode|||cardNumber)
        Set<String> existingCardKeys = cardTemplateRepository.findAllCardKeysBySetCode(apiSet.id);
        logger.info("[IMPORT] [{}] Set '{}' has {} existing cards in DB", tcgType, apiSet.name, existingCardKeys.size());

        final int[] savedInSet = {0};
        final int[] skippedInSet = {0};
        final int[] errorsInSet = {0};

        // Fetch all cards for this set and only import missing ones
        return getAllCardsForSet(apiSet.id)
                .concatMap(card -> {
                    if (card == null || card.name == null) {
                        errorsInSet[0]++;
                        return Mono.just(0);
                    }

                    // Build composite key for this card
                    String cardKey = card.name + "|||" + apiSet.id + "|||" + (card.number != null ? card.number : "");
                    
                    // Skip if card already exists
                    if (existingCardKeys.contains(cardKey)) {
                        skippedInSet[0]++;
                        return Mono.just(0);
                    }

                    try {
                        if (saveCardIfNotExists(card, finalTcgSet, tcgType)) {
                            savedInSet[0]++;
                            return Mono.just(1);
                        } else {
                            // Card already exists (shouldn't happen in delta, but handle it)
                            skippedInSet[0]++;
                            return Mono.just(0);
                        }
                    } catch (Exception e) {
                        errorsInSet[0]++;
                        logger.warn("[IMPORT] [{}] Delta card save error '{}' in set '{}': {}",
                                tcgType, card.name, apiSet.name, e.getMessage());
                        return Mono.just(0);
                    }
                })
                .reduce(0, Integer::sum)
                .doOnSuccess(count -> {
                    stats.totalCardsProcessed += savedInSet[0] + skippedInSet[0] + errorsInSet[0];
                    stats.errors += errorsInSet[0];
                    logger.info("[IMPORT] [{}] DELTA Set '{}' completed: {} new cards, {} skipped (existing), {} errors",
                            tcgType, apiSet.name, savedInSet[0], skippedInSet[0], errorsInSet[0]);
                })
                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
    }

    /**
     * Import all cards for a specific set.
     * Uses the /cards?set={setId} API endpoint with pagination.
     * If a card fails to save, logs the error and continues with the next card.
     */
    private Mono<Integer> importCardsForSet(TCGSet apiSet, TCGType tcgType, ImportStats stats) {
        logger.info("[IMPORT] [{}] Starting import for set: {} ({})", tcgType, apiSet.name, apiSet.id);

        // First, create the set in the database
        com.tcg.arena.model.TCGSet tcgSet;
        try {
            tcgSet = getOrCreateTCGSet(apiSet, tcgType);
        } catch (Exception e) {
            logger.error("[IMPORT] [{}] Failed to create set '{}': {}", tcgType, apiSet.name, e.getMessage());
            return Mono.just(0);
        }

        final com.tcg.arena.model.TCGSet finalTcgSet = tcgSet;
        final int[] savedInSet = {0};
        final int[] errorsInSet = {0};

        // Fetch all cards for this set using pagination
        return getAllCardsForSet(apiSet.id)
                .concatMap(card -> {
                    if (card == null || card.name == null) {
                        errorsInSet[0]++;
                        return Mono.just(0);
                    }

                    try {
                        if (saveCardIfNotExists(card, finalTcgSet, tcgType)) {
                            savedInSet[0]++;
                            return Mono.just(1);
                        } else {
                            // Card already exists (updated prices)
                            stats.pricesUpdated++;
                            return Mono.just(0);
                        }
                    } catch (Exception e) {
                        errorsInSet[0]++;
                        logger.warn("[IMPORT] [{}] Card save error '{}' in set '{}': {}",
                                tcgType, card.name, apiSet.name, e.getMessage());
                        // Continue with next card - do not interrupt
                        return Mono.just(0);
                    }
                })
                .reduce(0, Integer::sum)
                .doOnSuccess(count -> {
                    stats.totalCardsProcessed += savedInSet[0] + errorsInSet[0];
                    stats.errors += errorsInSet[0];
                    logger.info("[IMPORT] [{}] Set '{}' completed: {} new cards, {} errors",
                            tcgType, apiSet.name, savedInSet[0], errorsInSet[0]);
                })
                .delaySubscription(Duration.ofMillis(API_DELAY_MS)); // Rate limiting between sets
    }

    /**
     * Log completion for new sets import mode
     */
    private void logImportCompleteNewSets(TCGType tcgType, ImportStats stats, int setsProcessed) {
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ IMPORT COMPLETED (NEW SETS MODE): {}", tcgType.getDisplayName().toUpperCase());
        logger.info("╠══════════════════════════════════════════════════════════════");
        logger.info("║ Duration      : {}", stats.formatElapsedTime());
        logger.info("║ Sets Processed: {}", setsProcessed);
        logger.info("║ New Cards     : {}", stats.newCardsSaved);
        logger.info("║ Prices Updated: {}", stats.pricesUpdated);
        logger.info("║ Errors        : {}", stats.errors);
        logger.info("║ Avg Speed     : {} cards/sec", String.format("%.1f", stats.getCardsPerSecond()));
        logger.info("╚══════════════════════════════════════════════════════════════");
    }

    // ===================== Logging Helpers =====================

    private void logImportStart(TCGType tcgType, String gameId, int startOffset, ImportProgress progress) {
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ IMPORT START: {}", tcgType.getDisplayName().toUpperCase());
        logger.info("╠══════════════════════════════════════════════════════════════");
        logger.info("║ Game ID     : {}", gameId);
        logger.info("║ Start Offset: {}", startOffset);
        logger.info("║ Status      : {}", progress != null && progress.isComplete() ? "Previously completed" : "In progress");
        logger.info("║ Last Updated: {}", progress != null ? progress.getLastUpdated() : "Never");
        logger.info("╚══════════════════════════════════════════════════════════════");
    }

    private void logProgress(TCGType tcgType, ImportStats stats) {
        logger.info("[IMPORT] [{}] PROGRESS | Pages: {} | Cards: {} | New: {} | Updated: {} | Errors: {} | Time: {} | Speed: {} cards/s",
                tcgType,
                stats.pagesProcessed,
                stats.totalCardsProcessed,
                stats.newCardsSaved,
                stats.pricesUpdated,
                stats.errors,
                stats.formatElapsedTime(),
                String.format("%.1f", stats.getCardsPerSecond()));
    }

    private void logImportComplete(TCGType tcgType, ImportStats stats, boolean fullyCompleted) {
        String status = fullyCompleted ? "COMPLETED" : "PARTIAL";
        logger.info("╔══════════════════════════════════════════════════════════════");
        logger.info("║ IMPORT {}: {}", status, tcgType.getDisplayName().toUpperCase());
        logger.info("╠══════════════════════════════════════════════════════════════");
        logger.info("║ Duration      : {}", stats.formatElapsedTime());
        logger.info("║ Pages         : {}", stats.pagesProcessed);
        logger.info("║ Total Cards   : {}", stats.totalCardsProcessed);
        logger.info("║ New Cards     : {}", stats.newCardsSaved);
        logger.info("║ Prices Updated: {}", stats.pricesUpdated);
        logger.info("║ Errors        : {}", stats.errors);
        logger.info("║ Final Offset  : {}", stats.currentOffset);
        logger.info("║ Avg Speed     : {} cards/sec", String.format("%.1f", stats.getCardsPerSecond()));
        logger.info("╚══════════════════════════════════════════════════════════════");
    }

    /**
     * Synchronize release dates for all existing TCG sets by fetching from TCG
     * API
     * This is a synchronous operation that iterates through all TCG types and their
     * sets
     * 
     * @return Map of TCGType to number of sets updated
     */
    @Transactional
    public Map<String, Integer> syncAllSetReleaseDates() {
        logger.info("Starting release date sync for all sets");
        Map<String, Integer> results = new HashMap<>();

        for (Map.Entry<TCGType, String> entry : TCG_TYPE_TO_GAME_ID.entrySet()) {
            TCGType tcgType = entry.getKey();
            String gameId = entry.getValue();

            try {
                int updated = syncReleaseDatesForGame(tcgType, gameId);
                results.put(tcgType.name(), updated);
                logger.info("Synced {} release dates for {}", updated, tcgType);

                // Small delay between games to avoid rate limiting
                Thread.sleep(1000);
            } catch (Exception e) {
                logger.error("Error syncing release dates for {}: {}", tcgType, e.getMessage());
                results.put(tcgType.name(), -1); // -1 indicates error
            }
        }

        logger.info("Release date sync completed. Results: {}", results);
        return results;
    }

    /**
     * Sync release dates for a specific game/TCG type
     */
    private int syncReleaseDatesForGame(TCGType tcgType, String gameId) {
        logger.info("Fetching sets from TCG for {}", gameId);

        // Fetch all sets from TCG API
        List<TCGSet> tcgSets = getAllSets(gameId)
                .collectList()
                .block(Duration.ofMinutes(5)); // 5 minute timeout

        if (tcgSets == null || tcgSets.isEmpty()) {
            logger.warn("No sets found from TCG for {}", gameId);
            return 0;
        }

        logger.info("Found {} sets from TCG for {}", tcgSets.size(), gameId);

        int updatedCount = 0;
        for (TCGSet set : tcgSets) {
            if (set.releaseDate == null || set.releaseDate.isEmpty()) {
                continue;
            }

            // Find existing set by setCode
            Optional<com.tcg.arena.model.TCGSet> existingOpt = tcgSetRepository.findBySetCode(set.id);
            if (existingOpt.isEmpty()) {
                logger.debug("Set {} not found in DB, skipping", set.id);
                continue;
            }

            com.tcg.arena.model.TCGSet tcgSet = existingOpt.get();
            LocalDateTime newReleaseDate = parseReleaseDate(set.releaseDate);

            // Only update if the date is different (and not the default "now" date)
            // Check if current release date is today or very recent (indicating default
            // value)
            LocalDateTime now = LocalDateTime.now();
            boolean isDefaultDate = tcgSet.getReleaseDate().isAfter(now.minusDays(7));

            if (isDefaultDate || !tcgSet.getReleaseDate().toLocalDate().equals(newReleaseDate.toLocalDate())) {
                logger.debug("Updating release date for {}: {} -> {}",
                        tcgSet.getName(), tcgSet.getReleaseDate(), newReleaseDate);
                tcgSet.setReleaseDate(newReleaseDate);
                tcgSetRepository.save(tcgSet);
                updatedCount++;
            }
        }

        return updatedCount;
    }

    @Transactional
    private ImportProgress getOrCreateImportProgress(TCGType tcgType) {
        ImportProgress progress = importProgressRepository.findByTcgType(tcgType)
                .orElseGet(() -> {
                    logger.info("Creating new import progress for {}", tcgType);
                    ImportProgress p = new ImportProgress(tcgType);
                    return importProgressRepository.saveAndFlush(p);
                });
        logger.info("Import progress loaded/created for {} with ID: {}, offset: {}",
                tcgType, progress.getId(), progress.getLastOffset());
        return progress;
    }

    // Update progress - simplified version that creates or updates by TCGType
    // Uses REQUIRES_NEW to ensure immediate persistence even if parent transaction fails
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void updateProgress(TCGType tcgType, int offset, boolean complete) {
        ImportProgress p = importProgressRepository.findByTcgType(tcgType).orElseGet(() -> {
            logger.debug("[DB] Creating new import progress record for {}", tcgType);
            return new ImportProgress(tcgType);
        });

        int previousOffset = p.getLastOffset();
        p.setLastOffset(offset);
        p.setLastUpdated(LocalDateTime.now());
        p.setComplete(complete);
        importProgressRepository.saveAndFlush(p);

        logger.info("[DB] [{}] Progress saved: offset {} -> {} | complete: {}",
                tcgType, previousOffset, offset, complete);
    }

    /**
     * Get or create TCGSet for a card (simplified method for direct card import)
     */
    @Transactional
    private synchronized com.tcg.arena.model.TCGSet getOrCreateTCGSetForCard(String setId, String setName, TCGType tcgType) {
        // Check cache first
        if (tcgSetCache.containsKey(setId)) {
            return tcgSetCache.get(setId);
        }

        // Check database for existing set by setCode
        Optional<com.tcg.arena.model.TCGSet> existingSet = tcgSetRepository.findBySetCode(setId);
        if (existingSet.isPresent()) {
            com.tcg.arena.model.TCGSet existing = existingSet.get();
            
            // Preserve manually modified release date - don't update from API for this method
            // since we don't have the API data here
            logger.debug("Using existing TCGSet: {} (release date preserved: {})", 
                existing.getName(), existing.getReleaseDate());
            
            tcgSetCache.put(setId, existing);
            return existing;
        }

        // Get or create the parent Expansion
        Expansion expansion = getOrCreateExpansion(setName, tcgType);

        // Create new TCGSet
        com.tcg.arena.model.TCGSet tcgSet = new com.tcg.arena.model.TCGSet();
        tcgSet.setName(setName);
        tcgSet.setSetCode(setId);
        tcgSet.setExpansion(expansion);
        tcgSet.setCardCount(0);
        tcgSet.setReleaseDate(LocalDateTime.now());

        tcgSet = tcgSetRepository.save(tcgSet);
        tcgSetCache.put(setId, tcgSet);

        logger.debug("Created new TCGSet: {} ({})", tcgSet.getName(), tcgSet.getSetCode());
        return tcgSet;
    }

    /**
     * Get or create TCGSet and its parent Expansion
     * Uses caching to avoid repeated DB lookups
     */
    @Transactional
    private synchronized com.tcg.arena.model.TCGSet getOrCreateTCGSet(TCGSet tcgSet, TCGType tcgType) {
        // Check cache first
        String cacheKey = tcgSet.id;
        if (tcgSetCache.containsKey(cacheKey)) {
            return tcgSetCache.get(cacheKey);
        }

        // Check database for existing set by setCode
        Optional<com.tcg.arena.model.TCGSet> existingSet = tcgSetRepository.findBySetCode(tcgSet.id);
        if (existingSet.isPresent()) {
            com.tcg.arena.model.TCGSet existing = existingSet.get();

            // If manually modified, preserve ALL fields - don't update anything from API
            if (existing.getReleaseDateModifiedManually() != null && existing.getReleaseDateModifiedManually()) {
                logger.debug("Set '{}' was manually modified - preserving all fields (date: {})",
                    existing.getName(), existing.getReleaseDate());
                tcgSetCache.put(cacheKey, existing);
                return existing;
            }

            // Update fields from API only if not manually modified
            existing.setName(tcgSet.name);
            existing.setReleaseDate(parseReleaseDate(tcgSet.releaseDate));
            existing.setCardCount(tcgSet.cardsCount != null ? tcgSet.cardsCount : existing.getCardCount());

            // Save the updated set
            com.tcg.arena.model.TCGSet updatedSet = tcgSetRepository.save(existing);
            tcgSetCache.put(cacheKey, updatedSet);
            return updatedSet;
        }

        // Get or create the parent Expansion
        Expansion expansion = getOrCreateExpansion(tcgSet.name, tcgType);

        // Create new TCGSet
        com.tcg.arena.model.TCGSet newTcgSet = new com.tcg.arena.model.TCGSet();
        newTcgSet.setName(tcgSet.name);
        newTcgSet.setSetCode(tcgSet.id);
        newTcgSet.setExpansion(expansion);
        newTcgSet.setCardCount(tcgSet.cardsCount != null ? tcgSet.cardsCount : 0);
        newTcgSet.setReleaseDate(parseReleaseDate(tcgSet.releaseDate));

        newTcgSet = tcgSetRepository.save(newTcgSet);
        tcgSetCache.put(cacheKey, newTcgSet);

        logger.debug("Created new TCGSet: {} ({})", newTcgSet.getName(), newTcgSet.getSetCode());
        return newTcgSet;
    }

    /**
     * Get or create Expansion
     * Synchronized to prevent race conditions and duplicate creations
     * If expansion was manually modified, preserves all fields
     */
    @Transactional
    private synchronized Expansion getOrCreateExpansion(String name, TCGType tcgType) {
        String cacheKey = name + "_" + tcgType.name();

        if (expansionCache.containsKey(cacheKey)) {
            return expansionCache.get(cacheKey);
        }

        // Check database - look for existing expansion by title and tcgType
        Expansion existing = expansionRepository.findByTitle(name);
        if (existing != null && existing.getTcgType() == tcgType) {
            // If manually modified, preserve all fields - don't update anything
            if (existing.getModifiedManually() != null && existing.getModifiedManually()) {
                logger.debug("Expansion '{}' was manually modified - preserving all fields", existing.getTitle());
            }
            expansionCache.put(cacheKey, existing);
            return existing;
        }

        // Create new Expansion
        try {
            Expansion expansion = new Expansion();
            expansion.setTitle(name);
            expansion.setTcgType(tcgType);

            expansion = expansionRepository.save(expansion);
            expansionCache.put(cacheKey, expansion);

            logger.debug("Created new Expansion: {}", name);
            return expansion;
        } catch (Exception e) {
            // Handle duplicate key violation - fetch existing record
            logger.debug("Expansion already exists (constraint violation), fetching: {}", name);
            Expansion existing2 = expansionRepository.findByTitle(name);
            if (existing2 != null && existing2.getTcgType() == tcgType) {
                expansionCache.put(cacheKey, existing2);
                return existing2;
            }
            throw e;
        }
    }

    /**
     * Save card only if it doesn't already exist (by name + setCode + cardNumber)
     * Returns true if card was saved, false if it already exists (prices updated)
     */
    @Transactional
    private boolean saveCardIfNotExists(TCGCard card, com.tcg.arena.model.TCGSet tcgSet, TCGType tcgType) {
        String cardNumber = card.number != null ? card.number : "N/A";

        // Check for existing card by unique composite key (including N/A card numbers)
        List<CardTemplate> existing = cardTemplateRepository.findByNameAndSetCodeAndCardNumberIncludingNA(
                card.name, card.set, cardNumber);

        if (!existing.isEmpty()) {
            // Card exists - update prices only
            CardTemplate existingCard = existing.get(0);
            setPricesFromVariants(existingCard, card.variants);
            existingCard.setLastPriceUpdate(LocalDateTime.now());
            cardTemplateRepository.save(existingCard);
            return false; // Existing card - prices updated
        }

        // Create new CardTemplate
        try {
            CardTemplate template = new CardTemplate();
            template.setName(card.name);
            template.setTcgType(tcgType);
            template.setSetCode(card.set);
            template.setExpansion(tcgSet.getExpansion());
            template.setCardNumber(cardNumber);
            template.setRarity(mapRarity(card.rarity));
            template.setDescription(card.details);
            template.setImageUrl(card.imageUrl);
            template.setTcgplayerId(card.tcgplayerId);
            template.setDateCreated(LocalDateTime.now());
            setPricesFromVariants(template, card.variants);
            template.setLastPriceUpdate(LocalDateTime.now());

            cardTemplateRepository.save(template);
            logger.trace("[CARD] NEW: {} ({}/{})", card.name, card.set, cardNumber);
            return true;
        } catch (Exception e) {
            // Handle duplicate key violation - card was created by concurrent import
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                List<CardTemplate> retryExisting = cardTemplateRepository.findByNameAndSetCodeAndCardNumberIncludingNA(
                        card.name, card.set, cardNumber);
                if (!retryExisting.isEmpty()) {
                    CardTemplate existingCard = retryExisting.get(0);
                    setPricesFromVariants(existingCard, card.variants);
                    existingCard.setLastPriceUpdate(LocalDateTime.now());
                    cardTemplateRepository.save(existingCard);
                    return false;
                }
            }
            logger.warn("[CARD] ERROR saving '{}': {}", card.name, e.getMessage());
            return false;
        }
    }

    /**
     * Extract and set all prices from TCG variants
     */
    private void setPricesFromVariants(CardTemplate template, List<TCGVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        Double lowPrice = null;
        Double highPrice = null;

        for (TCGVariant variant : variants) {
            if (variant.price == null)
                continue;

            String condition = variant.condition != null ? variant.condition.toLowerCase() : "";
            String printing = variant.printing != null ? variant.printing.toLowerCase() : "";
            boolean isFoil = printing.contains("foil") || printing.contains("holo");

            // Track price range
            if (lowPrice == null || variant.price < lowPrice)
                lowPrice = variant.price;
            if (highPrice == null || variant.price > highPrice)
                highPrice = variant.price;

            // Set condition-specific prices
            if (condition.contains("near mint") || condition.equals("nm")) {
                if (isFoil) {
                    template.setPriceFoilNearMint(variant.price);
                } else {
                    template.setPriceNearMint(variant.price);
                    template.setMarketPrice(variant.price); // NM is market price
                }
            } else if (condition.contains("lightly") || condition.equals("lp")) {
                template.setPriceLightlyPlayed(variant.price);
            } else if (condition.contains("moderately") || condition.equals("mp")) {
                template.setPriceModeratelyPlayed(variant.price);
            } else if (condition.contains("heavily") || condition.equals("hp")) {
                template.setPriceHeavilyPlayed(variant.price);
            } else if (condition.contains("damaged") || condition.equals("dmg")) {
                template.setPriceDamaged(variant.price);
            }

            // Set foil price for any foil variant
            if (isFoil && template.getPriceFoil() == null) {
                template.setPriceFoil(variant.price);
            }
        }

        // Set price range
        template.setPriceLow(lowPrice);
        template.setPriceHigh(highPrice);

        // If no NM price found, use first available as market price
        if (template.getMarketPrice() == null && lowPrice != null) {
            template.setMarketPrice(lowPrice);
        }
    }

    /**
     * Update TCGSet card count after import
     */
    private void updateSetCardCount(com.tcg.arena.model.TCGSet tcgSet, int cardCount) {
        if (tcgSet.getCardCount() == null || tcgSet.getCardCount() != cardCount) {
            tcgSet.setCardCount(cardCount);
            tcgSetRepository.save(tcgSet);
        }
    }

    // ===================== Utility Methods =====================

    private Double extractNearMintPrice(List<TCGVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }

        for (TCGVariant variant : variants) {
            if (("Near Mint".equalsIgnoreCase(variant.condition) ||
                    "NM".equalsIgnoreCase(variant.condition)) &&
                    variant.price != null) {
                return variant.price;
            }
        }

        // Fallback: return first available price
        return variants.stream()
                .filter(v -> v.price != null)
                .findFirst()
                .map(v -> v.price)
                .orElse(null);
    }

    private LocalDateTime parseReleaseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // Try ISO format (2024-01-15)
            if (dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
                String[] parts = dateStr.split("-");
                return LocalDateTime.of(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        0, 0);
            }
            // Try other common formats
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            logger.debug("Could not parse date '{}', using current date", dateStr);
            return LocalDateTime.now();
        }
    }

    private Rarity mapRarity(String rarityStr) {
        if (rarityStr == null)
            return Rarity.COMMON;

        String lower = rarityStr.toLowerCase();

        // Order matters - check more specific terms first
        if (lower.contains("mythic"))
            return Rarity.MYTHIC_RARE;
        if (lower.contains("secret"))
            return Rarity.SECRET_RARE;
        if (lower.contains("ultra"))
            return Rarity.ULTRA_RARE;
        if (lower.contains("super"))
            return Rarity.SUPER_RARE;
        if (lower.contains("hyper"))
            return Rarity.HYPER_RARE;
        if (lower.contains("rare"))
            return Rarity.RARE;
        if (lower.contains("uncommon"))
            return Rarity.UNCOMMON;
        if (lower.contains("promo"))
            return Rarity.PROMO;
        if (lower.contains("special"))
            return Rarity.SPECIAL_ART_RARE;
        if (lower.contains("holo"))
            return Rarity.HOLOGRAPHIC;

        return Rarity.COMMON;
    }

    public boolean isTCGSupported(TCGType tcgType) {
        return TCG_TYPE_TO_GAME_ID.containsKey(tcgType);
    }

    public List<TCGType> getSupportedTCGTypes() {
        return new ArrayList<>(TCG_TYPE_TO_GAME_ID.keySet());
    }

    /**
     * Check if there are new data available in the API beyond the current offset
     * This is used to determine if a "completed" import should be resumed
     */
    private boolean checkForNewData(String gameId, int currentOffset) {
        try {
            logger.debug("Checking for new data in {} at offset {}", gameId, currentOffset);
            TCGCardsResponse response = getCardsPageByGame(gameId, currentOffset)
                    .block(); // Method already has timeout handling
            
            if (response == null) {
                logger.warn("Null response when checking for new data");
                return false;
            }
            
            List<TCGCard> cards = response.getCards();
            boolean hasData = cards != null && !cards.isEmpty();
            
            logger.info("New data check for {} at offset {}: {} cards found", 
                    gameId, currentOffset, cards != null ? cards.size() : 0);
            
            return hasData;
        } catch (Exception e) {
            logger.error("Error checking for new data in {} at offset {}: {}", 
                    gameId, currentOffset, e.getMessage());
            // On error, assume no new data to be safe
            return false;
        }
    }

    // ===================== Scryfall-specific methods for Magic =====================

    /**
     * Import Magic cards using Scryfall bulk data
     */
    public Mono<Integer> importMagicCards() {
        return Mono.fromCallable(() -> {
            logger.info("Starting Magic import using Scryfall");

            // Clear caches
            expansionCache.clear();
            tcgSetCache.clear();

            try {
                // Import sets first
                importMagicSets();

                // Import cards from bulk data
                return importMagicCardsFromBulk();

            } catch (Exception e) {
                logger.error("Error during Magic import: {}", e.getMessage(), e);
                return 0;
            }
        });
    }

    /**
     * Import Magic delta using Scryfall search API (only new cards since last import)
     */
    public Mono<Integer> importMagicDelta() {
        return Mono.fromCallable(() -> {
            logger.info("Starting Magic delta import using Scryfall search API");

            try {
                // Clear caches
                expansionCache.clear();
                tcgSetCache.clear();

                // Import sets first (if needed)
                importMagicSets();

                // Get last import date for delta
                LocalDateTime lastImport = getLastMagicImportDate();
                String dateQuery = lastImport.format(DateTimeFormatter.ISO_LOCAL_DATE);

                // Search for new cards since last import
                return searchAndImportNewMagicCards("date:>" + dateQuery);

            } catch (Exception e) {
                logger.error("Error during Magic delta import: {}", e.getMessage(), e);
                return 0;
            }
        });
    }

    /**
     * Import Magic sets from Scryfall
     */
    private void importMagicSets() {
        logger.info("Importing Magic sets from Scryfall");

        try {
            ScryfallSetsResponse response = scryfallWebClient.get()
                    .uri("/sets")
                    .retrieve()
                    .bodyToMono(ScryfallSetsResponse.class)
                    .block(Duration.ofSeconds(30));

            if (response == null || response.data == null) {
                logger.warn("No sets data from Scryfall");
                return;
            }

            int imported = 0;
            for (ScryfallSet set : response.data) {
                try {
                    saveScryfallSet(set, TCGType.MAGIC);
                    imported++;
                } catch (Exception e) {
                    logger.warn("Error saving set {}: {}", set.name, e.getMessage());
                }
            }

            logger.info("Imported {} Magic sets from Scryfall", imported);

        } catch (Exception e) {
            logger.error("Error importing Magic sets: {}", e.getMessage(), e);
        }
    }

    /**
     * Save Scryfall set to database
     */
    private void saveScryfallSet(ScryfallSet scryfallSet, TCGType tcgType) {
        // Check if set already exists
        Optional<com.tcg.arena.model.TCGSet> existing = tcgSetRepository.findBySetCode(scryfallSet.code);
        if (existing.isPresent()) {
            logger.debug("Set {} already exists, skipping", scryfallSet.name);
            return;
        }

        // Get or create expansion
        Expansion expansion = getOrCreateExpansion(scryfallSet.name, tcgType);

        // Create TCGSet
        com.tcg.arena.model.TCGSet tcgSet = new com.tcg.arena.model.TCGSet();
        tcgSet.setName(scryfallSet.name);
        tcgSet.setSetCode(scryfallSet.code);
        tcgSet.setExpansion(expansion);
        tcgSet.setCardCount(scryfallSet.card_count);
        tcgSet.setReleaseDate(parseReleaseDate(scryfallSet.released_at));
        tcgSet.setDescription(scryfallSet.set_type);

        tcgSetRepository.save(tcgSet);
        logger.debug("Saved Scryfall set: {}", scryfallSet.name);
    }

    /**
     * Import Magic cards from Scryfall bulk data
     */
    @Transactional
    private int importMagicCardsFromBulk() {
        logger.info("Importing Magic cards from Scryfall bulk data");

        try {
            // Get bulk data info
            ScryfallBulkDataResponse bulkResponse = scryfallWebClient.get()
                    .uri("/bulk-data")
                    .retrieve()
                    .bodyToMono(ScryfallBulkDataResponse.class)
                    .block(Duration.ofSeconds(30));

            if (bulkResponse == null || bulkResponse.data == null) {
                logger.warn("No bulk data from Scryfall");
                return 0;
            }

            // Find "Default Cards" bulk data
            ScryfallBulkData defaultCards = bulkResponse.data.stream()
                    .filter(b -> "default_cards".equals(b.type))
                    .findFirst()
                    .orElse(null);

            if (defaultCards == null || defaultCards.download_uri == null) {
                logger.warn("Default cards bulk data not found");
                return 0;
            }

            logger.info("Downloading bulk cards from: {}", defaultCards.download_uri);

            // Download and process bulk data
            List<ScryfallCard> cards = scryfallWebClient.get()
                    .uri(defaultCards.download_uri)
                    .retrieve()
                    .bodyToFlux(ScryfallCard.class)
                    .collectList()
                    .block(Duration.ofMinutes(10)); // Allow 10 minutes for download

            if (cards == null) {
                logger.warn("No cards downloaded from bulk data");
                return 0;
            }

            logger.info("Downloaded {} cards from Scryfall bulk data", cards.size());

            // Load existing card keys in memory for fast lookup
            logger.info("Loading existing card keys for duplicate checking...");
            List<Object[]> existingKeys = cardTemplateRepository.findAllCardKeys();
            Set<String> existingCardKeys = new HashSet<>();
            for (Object[] key : existingKeys) {
                String name = (String) key[0];
                String setCode = (String) key[1];
                String cardNumber = (String) key[2];
                String compositeKey = name + "|" + setCode + "|" + cardNumber;
                existingCardKeys.add(compositeKey);
            }
            logger.info("Loaded {} existing card keys", existingCardKeys.size());

            int saved = 0;
            List<CardTemplate> cardsToSave = new ArrayList<>();
            int skipped = 0;
            for (ScryfallCard card : cards) {
                if (card.digital || card.oversized) {
                    continue;
                }
                String cardNumber = card.collector_number != null ? card.collector_number : "N/A";

                // Get TCGSet
                com.tcg.arena.model.TCGSet tcgSet = tcgSetCache.computeIfAbsent(card.set, setCode -> {
                    Optional<com.tcg.arena.model.TCGSet> opt = tcgSetRepository.findBySetCode(setCode);
                    return opt.orElse(null);
                });

                if (tcgSet == null) {
                    logger.warn("TCGSet not found for card {} in set {}", card.name, card.set);
                    continue;
                }

                // Check if card already exists using in-memory lookup
                String compositeKey = card.name + "|" + card.set + "|" + cardNumber;
                if (existingCardKeys.contains(compositeKey)) {
                    skipped++;
                    continue;
                }

                // Create new CardTemplate
                CardTemplate template = new CardTemplate();
                template.setName(card.name);
                template.setTcgType(TCGType.MAGIC);
                template.setSetCode(card.set);
                template.setExpansion(tcgSet.getExpansion());
                template.setCardNumber(cardNumber);
                template.setRarity(mapRarity(card.rarity));
                template.setDescription(card.oracle_text);
                template.setImageUrl(getScryfallImageUrl(card));
                template.setTcgplayerId(card.id); // Save Scryfall ID here
                template.setDateCreated(LocalDateTime.now());

                // Set prices
                setPricesFromScryfall(template, card);

                cardsToSave.add(template);

                if (cardsToSave.size() >= 10000) {
                    try {
                        cardTemplateRepository.saveAll(cardsToSave);
                        saved += cardsToSave.size();
                        logger.info("Saved batch of {} cards, total saved: {}", cardsToSave.size(), saved);
                    } catch (Exception e) {
                        logger.error("Error saving batch of {} cards: {}", cardsToSave.size(), e.getMessage(), e);
                        // Try to save individually to identify problematic cards
                        for (CardTemplate cardTemplate : cardsToSave) {
                            try {
                                cardTemplateRepository.save(cardTemplate);
                                saved++;
                            } catch (Exception ex) {
                                logger.warn("Failed to save card {}: {}", cardTemplate.getName(), ex.getMessage());
                            }
                        }
                    }
                    cardsToSave.clear();
                }
            }
            try {
                cardTemplateRepository.saveAll(cardsToSave);
                saved += cardsToSave.size();
                logger.info("Saved final batch of {} cards, total saved: {}", cardsToSave.size(), saved);
            } catch (Exception e) {
                logger.error("Error saving final batch of {} cards: {}", cardsToSave.size(), e.getMessage(), e);
                // Try to save individually to identify problematic cards
                for (CardTemplate cardTemplate : cardsToSave) {
                    try {
                        cardTemplateRepository.save(cardTemplate);
                        saved++;
                    } catch (Exception ex) {
                        logger.warn("Failed to save card {}: {}", cardTemplate.getName(), ex.getMessage());
                    }
                }
            }

            logger.info("Saved {} new Magic cards from Scryfall ({} skipped as duplicates)", saved, skipped);
            return saved;

        } catch (Exception e) {
            logger.error("Error importing Magic cards from bulk: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Get the last import date for Magic cards
     */
    private LocalDateTime getLastMagicImportDate() {
        // Try to get from ImportProgress, fallback to 30 days ago
        Optional<ImportProgress> progress = importProgressRepository.findByTcgType(TCGType.MAGIC);
        if (progress.isPresent() && progress.get().getLastUpdated() != null) {
            return progress.get().getLastUpdated().toLocalDate().atStartOfDay();
        }
        // Fallback: 30 days ago to be safe
        return LocalDateTime.now().minusDays(30);
    }

    /**
     * Search and import new Magic cards using Scryfall API
     */
    private int searchAndImportNewMagicCards(String query) {
        logger.info("Searching Scryfall for: {}", query);

        try {
            final int[] totalImported = {0};
            int page = 1;
            boolean hasMore = true;

            while (hasMore) {
                final int currentPage = page;
                ScryfallSearchResponse response = scryfallWebClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/cards/search")
                                .queryParam("q", query)
                                .queryParam("page", currentPage)
                                .queryParam("unique", "prints") // Include all printings
                                .build())
                        .retrieve()
                        .bodyToMono(ScryfallSearchResponse.class)
                        .block(Duration.ofSeconds(30));

                if (response == null || response.data == null || response.data.isEmpty()) {
                    break;
                }

                logger.info("Processing page {}: {} cards", page, response.data.size());

                for (ScryfallCard card : response.data) {
                    try {
                        if (saveScryfallCard(card, TCGType.MAGIC)) {
                            totalImported[0]++;
                        }
                    } catch (Exception e) {
                        logger.warn("Error saving card {}: {}", card.name, e.getMessage());
                    }
                }

                hasMore = response.has_more;
                page++;

                // Safety limit to avoid infinite loops
                if (page > 100) {
                    logger.warn("Reached page limit (100), stopping search");
                    break;
                }
            }

            logger.info("Delta import completed: {} new cards imported", totalImported[0]);
            return totalImported[0];

        } catch (Exception e) {
            logger.error("Error searching new Magic cards: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Save Scryfall card to database
     */
    private boolean saveScryfallCard(ScryfallCard card, TCGType tcgType) {
        // Skip digital/oversized cards
        if (card.digital || card.oversized) {
            return false;
        }

        String cardNumber = card.collector_number != null ? card.collector_number : "N/A";

        // Check for existing card
        List<CardTemplate> existing = cardTemplateRepository.findByNameAndSetCodeAndCardNumber(
                card.name, card.set, cardNumber);

        if (!existing.isEmpty()) {
            // Update existing card with Scryfall data
            CardTemplate existingCard = existing.get(0);
            updateCardWithScryfallData(existingCard, card);
            return false; // Not new
        }

        // Get TCGSet
        com.tcg.arena.model.TCGSet tcgSet = tcgSetCache.computeIfAbsent(card.set, setCode -> {
            Optional<com.tcg.arena.model.TCGSet> opt = tcgSetRepository.findBySetCode(setCode);
            return opt.orElse(null);
        });

        if (tcgSet == null) {
            logger.warn("TCGSet not found for card {} in set {}", card.name, card.set);
            return false;
        }

        // Create new CardTemplate
        CardTemplate template = new CardTemplate();
        template.setName(card.name);
        template.setTcgType(tcgType);
        template.setSetCode(card.set);
        template.setExpansion(tcgSet.getExpansion());
        template.setCardNumber(cardNumber);
        template.setRarity(mapRarity(card.rarity));
        template.setDescription(card.oracle_text);
        template.setImageUrl(getScryfallImageUrl(card));
        template.setTcgplayerId(card.id); // Save Scryfall ID here
        template.setDateCreated(LocalDateTime.now());

        // Set prices
        setPricesFromScryfall(template, card);

        cardTemplateRepository.save(template);
        logger.debug("Saved new Scryfall card: {}", card.name);
        return true;
    }

    /**
     * Update existing card with Scryfall data
     */
    private void updateCardWithScryfallData(CardTemplate template, ScryfallCard card) {
        // Update Scryfall ID if not set
        if (template.getTcgplayerId() == null) {
            template.setTcgplayerId(card.id);
        }

        // Update image URL if not set
        if (template.getImageUrl() == null) {
            template.setImageUrl(getScryfallImageUrl(card));
        }

        // Update prices
        setPricesFromScryfall(template, card);

        cardTemplateRepository.save(template);
    }

    /**
     * Get image URL from Scryfall card
     */
    private String getScryfallImageUrl(ScryfallCard card) {
        if (card.image_uris != null && card.image_uris.normal != null) {
            return card.image_uris.normal;
        }
        if (card.card_faces != null && !card.card_faces.isEmpty()) {
            ScryfallCardFace face = card.card_faces.get(0);
            if (face.image_uris != null && face.image_uris.normal != null) {
                return face.image_uris.normal;
            }
        }
        return null;
    }

    /**
     * Set prices from Scryfall data
     */
    private void setPricesFromScryfall(CardTemplate template, ScryfallCard card) {
        if (card.prices != null) {
            try {
                if (card.prices.usd != null) {
                    template.setMarketPrice(Double.parseDouble(card.prices.usd));
                }
                if (card.prices.usd_foil != null) {
                    template.setPriceFoil(Double.parseDouble(card.prices.usd_foil));
                }
            } catch (NumberFormatException e) {
                logger.warn("Invalid price format for card {}: {}", card.name, e.getMessage());
            }
        }
    }
}
