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

    // Rate limiting: delay between API calls (ms)
    // Rate limiting: delay between API calls (ms) -> increased to 3s to avoid 429
    private static final long API_DELAY_MS = 3000;
    // Page size for card fetching
    private static final int PAGE_SIZE = 20;

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
    private String apiKey;

    // Mapping from internal TCGType to TCG game IDs (from /games endpoint)
    private static final Map<TCGType, String> TCG_TYPE_TO_GAME_ID = Map.ofEntries(
            Map.entry(TCGType.MAGIC, "magic-the-gathering"),
            Map.entry(TCGType.POKEMON, "pokemon"),
            Map.entry(TCGType.YUGIOH, "yugioh"),
            Map.entry(TCGType.LORCANA, "disney-lorcana"),
            Map.entry(TCGType.ONE_PIECE, "one-piece-card-game"),
            Map.entry(TCGType.DIGIMON, "digimon-card-game"),
            Map.entry(TCGType.RIFTBOUND, "riftbound-league-of-legends-trading-card-game"));

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
                .header("x-api-key", apiKey)
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
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/sets").queryParam("game", gameId);
                    if (cursor != null) {
                        builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .header("x-api-key", apiKey)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("[TCG API ERROR] getSetsPage for {}: HTTP {} - Response body: {}",
                                        gameId, response.statusCode().value(), body);
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGSetsResponse.class)
                .doOnSuccess(resp -> logger.info("Fetched sets for {}: {} sets found, hasMore: {}",
                        gameId, resp.getSets().size(), resp.hasMore))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying getSetsPage for {} - attempt {}", 
                            gameId, retrySignal.totalRetries() + 1))
                )
                .onErrorResume(e -> {
                    logger.error("Error fetching sets for {}: {}", gameId, e.getMessage(), e);
                    return Mono.just(new TCGSetsResponse());
                });
    }

    /**
     * Get all cards for a set with pagination
     */
    public Flux<TCGCard> getAllCardsForSet(String setId) {
        return getCardsPage(setId, null)
                .expand(response -> {
                    if (response.hasMore && response.nextCursor != null) {
                        return getCardsPage(setId, response.nextCursor)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
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
        logger.info("[FLUX] getCardPagesForGame called for game: {}, startOffset: {}", gameId, startOffset);
        return getCardsPageByGame(gameId, startOffset)
                .doOnNext(resp -> logger.debug("[FLUX] First page received, cards: {}", resp.getCards().size()))
                .expand(response -> {
                    List<TCGCard> cards = response.getCards();
                    logger.debug("[FLUX-EXPAND] Checking if we need next page. Current cards: {}, offset: {}",
                            cards.size(), response.currentOffset);
                    if (!cards.isEmpty()) {
                        int nextOffset = response.currentOffset + PAGE_SIZE;
                        logger.debug("[FLUX-EXPAND] Fetching next page at offset: {}", nextOffset);
                        return getCardsPageByGame(gameId, nextOffset)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    logger.info("[FLUX-EXPAND] No more cards, ending stream");
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
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("game", gameId)
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .build())
                .header("x-api-key", apiKey)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error(
                                        "[TCG API ERROR] getCardsPageByGame for {} (offset {}): HTTP {} - Response body: {}",
                                        gameId, offset, response.statusCode().value(), body);
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGCardsResponse.class)
                .map(response -> {
                    response.currentOffset = offset;
                    return response;
                })
                .doOnSuccess(resp -> {
                    if (resp != null && resp.getCards() != null) {
                        logger.info("Fetched cards page for game {}: {} cards (offset: {})",
                                gameId, resp.getCards().size(), offset);
                    }
                })
                // Add retry mechanism for 500 errors with exponential backoff
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying request for game {} (offset {}) - attempt {}", 
                            gameId, offset, retrySignal.totalRetries() + 1))
                )
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for game {} (offset {}): {}", gameId, offset, e.getMessage(), e);
                    // Return empty response but preserve the CURRENT offset (not 0)
                    // This allows the main flow to detect error and stop without saving wrong
                    // offset
                    TCGCardsResponse errorResponse = new TCGCardsResponse();
                    errorResponse.currentOffset = offset; // Preserve offset where error occurred
                    return Mono.just(errorResponse);
                });
    }

    private Mono<TCGCardsResponse> getCardsPage(String setId, String cursor) {
        return webClient.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder
                            .path("/cards")
                            .queryParam("set", setId)
                            .queryParam("limit", PAGE_SIZE);
                    if (cursor != null) {
                        builder.queryParam("cursor", cursor);
                    }
                    return builder.build();
                })
                .header("x-api-key", apiKey)
                .retrieve()
                .onStatus(status -> status.isError(), response -> {
                    return response.bodyToMono(String.class)
                            .flatMap(body -> {
                                logger.error("[TCG API ERROR] getCardsPage for set {}: HTTP {} - Response body: {}",
                                        setId, response.statusCode().value(), body);
                                return Mono.error(new RuntimeException(
                                        "TCG API error: HTTP " + response.statusCode().value() + " - " + body));
                            });
                })
                .bodyToMono(TCGCardsResponse.class)
                .doOnSuccess(resp -> logger.debug("Fetched cards page for set {}, count: {}, hasMore: {}",
                        setId, resp.getCards().size(), resp.hasMore))
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                    .filter(throwable -> {
                        if (throwable instanceof RuntimeException) {
                            String message = throwable.getMessage();
                            return message != null && message.contains("HTTP 500");
                        }
                        return false;
                    })
                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> retrySignal.failure())
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying getCardsPage for set {} - attempt {}", 
                            setId, retrySignal.totalRetries() + 1))
                )
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for set {}: {}", setId, e.getMessage(), e);
                    return Mono.just(new TCGCardsResponse());
                });
    }

    // ===================== Import Logic =====================

    /**
     * Import all cards for a specific TCG type
     * Hybrid approach: fetch sets first for full metadata, then fetch all cards
     */
    /**
     * Import all cards for a specific TCG type
     * Hybrid approach: fetch sets first for full metadata, then fetch all cards
     * page by page
     * Resumable: Stores progress in DB
     */
    public Mono<Integer> importCardsForTCG(TCGType tcgType) {
        // Special handling for Magic using Scryfall
        if (tcgType == TCGType.MAGIC) {
            return importMagicCards();
        }

        String gameId = TCG_TYPE_TO_GAME_ID.get(tcgType);
        if (gameId == null) {
            logger.warn("TCG type {} not supported by TCG", tcgType);
            return Mono.just(0);
        }

        // Clear caches at start of import
        expansionCache.clear();
        tcgSetCache.clear();

        logger.info("Starting TCG import for {} (game: {})", tcgType.getDisplayName(), gameId);

        // Get current progress
        ImportProgress progress = getOrCreateImportProgress(tcgType);

        int startOffset = (progress != null && progress.getLastOffset() != 0) ? progress.getLastOffset() : 0;
        logger.info("Resuming import from offset: {}", startOffset);

        // Check if import is already complete - but verify if there are actually more data
        if (progress != null && progress.isComplete()) {
            logger.info("Import for {} was marked as completed. Checking if there are new data available...", tcgType);
            
            // Test if there are more cards available by making a quick API call
            boolean hasMoreData = checkForNewData(gameId, startOffset);
            if (!hasMoreData) {
                logger.info("No new data available for {}. Import remains completed.", tcgType);
                return Mono.just(0);
            } else {
                logger.info("New data found! Resetting completion status and resuming import from offset {}", startOffset);
                // Reset completion status since there are new data
                progress.setComplete(false);
                importProgressRepository.saveAndFlush(progress);
            }
        }

        // Track progress in memory during import
        final int[] lastSuccessfulOffset = { startOffset };
        final boolean[] importCompleted = { false };
        final int[] pagesProcessed = { 0 }; // Track number of pages processed
        final int SAVE_PROGRESS_EVERY_N_PAGES = 5; // Save progress every 5 pages

        // Step 1: Fetch all sets first to get full metadata
        return getAllSets(gameId)
                .collectList()
                .flatMap(sets -> {
                    logger.info("Fetched {} sets for {}", sets.size(), gameId);

                    // Create all sets in DB first with proper metadata
                    Map<String, com.tcg.arena.model.TCGSet> setMap = new HashMap<>();
                    for (TCGSet set : sets) {
                        try {
                            com.tcg.arena.model.TCGSet tcgSet = getOrCreateTCGSet(set, tcgType);
                            setMap.put(set.id, tcgSet);
                        } catch (Exception e) {
                            logger.warn("Error creating set {}: {}", set.name, e.getMessage());
                        }
                    }
                    logger.info("Created/loaded {} sets in DB", setMap.size());

                    // Step 2: Fetch cards page by page starting from offset
                    logger.info("Starting card fetch for {} from offset {}", gameId, startOffset);
                    return getCardPagesForGame(gameId, startOffset)
                            .concatMap(response -> { // concatMap ensures sequential processing
                                List<TCGCard> cards = response.getCards();

                                if (cards.isEmpty()) {
                                    // Empty response - end of data
                                    logger.info("No more cards. Import completed successfully at offset: {}",
                                            lastSuccessfulOffset[0]);
                                    importCompleted[0] = true;
                                    return Mono.empty(); // Stop the stream
                                }

                                // Process cards in this page
                                int savedInPage = 0;
                                for (TCGCard card : cards) {
                                    if (card == null || card.name == null) {
                                        logger.warn("Skipping null or invalid card at offset {}",
                                                response.currentOffset);
                                        continue;
                                    }

                                    try {
                                        String setId = card.set != null ? card.set : "unknown";
                                        com.tcg.arena.model.TCGSet tcgSet = setMap.get(setId);
                                        if (tcgSet == null) {
                                            String setName = card.setName != null ? card.setName : setId;
                                            tcgSet = getOrCreateTCGSetForCard(setId, setName, tcgType);
                                            setMap.put(setId, tcgSet);
                                        }

                                        if (saveCardIfNotExists(card, tcgSet, tcgType)) {
                                            savedInPage++;
                                        }
                                    } catch (Exception e) {
                                        logger.warn("Error saving card '{}': {}", card.name, e.getMessage());
                                    }
                                }

                                // Update last successful offset in memory
                                lastSuccessfulOffset[0] = response.currentOffset;
                                pagesProcessed[0]++;
                                
                                // Save progress to DB every N pages for real-time visibility
                                if (pagesProcessed[0] % SAVE_PROGRESS_EVERY_N_PAGES == 0) {
                                    try {
                                        logger.info("Saving progress checkpoint at offset: {} (page {})", 
                                            response.currentOffset, pagesProcessed[0]);
                                        updateProgress(tcgType, lastSuccessfulOffset[0], false);
                                    } catch (Exception e) {
                                        logger.warn("Error saving progress checkpoint: {}", e.getMessage());
                                        // Don't fail the import, just log the warning
                                    }
                                }
                                
                                logger.debug("Processed page at offset: {} ({} cards saved)", response.currentOffset,
                                        savedInPage);

                                return Mono.just(savedInPage);
                            })
                            .onErrorResume(e -> {
                                logger.error("Error during import for {}: {}", gameId, e.getMessage(), e);
                                logger.info("Import stopped at offset: {}", lastSuccessfulOffset[0]);
                                return Mono.empty(); // Stop processing
                            })
                            .reduce(0, Integer::sum)
                            .doOnSuccess(total -> {
                                logger.info("Import complete for {}: {} new cards imported", gameId, total);
                                // Update progress in DB only at the end
                                try {
                                    updateProgress(tcgType, lastSuccessfulOffset[0], importCompleted[0]);
                                } catch (Exception e) {
                                    logger.error("Error updating progress after successful import: {}", e.getMessage(),
                                            e);
                                }
                            });
                })
                .doOnError(e -> {
                    // Save progress on error
                    logger.error("Import failed, saving progress at offset: {}", lastSuccessfulOffset[0], e);
                    try {
                        updateProgress(tcgType, lastSuccessfulOffset[0], false);
                    } catch (Exception ex) {
                        logger.error("Error saving progress after failed import: {}", ex.getMessage(), ex);
                    }
                })
                .doFinally(signal -> {
                    logger.info("Import finalized with signal: {} for {}", signal, tcgType);
                    expansionCache.clear();
                    tcgSetCache.clear();
                });
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
            logger.info("Creating new import progress for {}", tcgType);
            ImportProgress newProgress = new ImportProgress(tcgType);
            return newProgress;
        });

        logger.info("📊 Updating progress for {}: offset {} -> {}, complete={}, progressId={}",
                tcgType, p.getLastOffset(), offset, complete, p.getId());
        p.setLastOffset(offset);
        p.setLastUpdated(LocalDateTime.now());
        p.setComplete(complete);
        ImportProgress saved = importProgressRepository.saveAndFlush(p);
        logger.info("✅ Progress persisted for {} (ID: {}). Offset: {}, complete: {}, lastUpdated: {}",
                tcgType, saved.getId(), saved.getLastOffset(), saved.isComplete(), saved.getLastUpdated());
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
            tcgSetCache.put(setId, existingSet.get());
            return existingSet.get();
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
            tcgSetCache.put(cacheKey, existingSet.get());
            return existingSet.get();
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
     * Returns true if card was saved, false if it already exists
     * Handles duplicate key violations gracefully
     */
    @Transactional
    private boolean saveCardIfNotExists(TCGCard card, com.tcg.arena.model.TCGSet tcgSet, TCGType tcgType) {
        String cardNumber = card.number != null ? card.number : "N/A";

        // Check for existing card by unique composite key
        List<CardTemplate> existing = cardTemplateRepository.findByNameAndSetCodeAndCardNumber(
                card.name, card.set, cardNumber);

        if (!existing.isEmpty()) {
            // Card exists - update prices only
            CardTemplate existingCard = existing.get(0);
            setPricesFromVariants(existingCard, card.variants);
            existingCard.setLastPriceUpdate(LocalDateTime.now());
            cardTemplateRepository.save(existingCard);
            logger.debug("Updated prices for existing card: {}", card.name);
            return false; // Not a new card
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

            // Set all prices from variants
            setPricesFromVariants(template, card.variants);
            template.setLastPriceUpdate(LocalDateTime.now());

            cardTemplateRepository.save(template);
            return true;
        } catch (Exception e) {
            // Handle duplicate key violation - card was created by concurrent import
            if (e.getMessage() != null && e.getMessage().contains("constraint")) {
                logger.debug("Card already exists (constraint violation), updating prices: {}", card.name);
                List<CardTemplate> retryExisting = cardTemplateRepository.findByNameAndSetCodeAndCardNumber(
                        card.name, card.set, cardNumber);
                if (!retryExisting.isEmpty()) {
                    CardTemplate existingCard = retryExisting.get(0);
                    setPricesFromVariants(existingCard, card.variants);
                    existingCard.setLastPriceUpdate(LocalDateTime.now());
                    cardTemplateRepository.save(existingCard);
                    return false;
                }
            }
            logger.error("Error saving card {}: {}", card.name, e.getMessage());
            throw e;
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
                    .block(Duration.ofSeconds(30)); // Timeout for this check
            
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

            int saved = 0;
            List<CardTemplate> cardsToSave = new ArrayList<>();
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
                        logger.info("Saved batch of 10000 cards, total saved: {}", saved);
                    } catch (Exception e) {
                        logger.warn("Error saving batch: {}", e.getMessage());
                    }
                    cardsToSave.clear();
                }
            }
            try {
                cardTemplateRepository.saveAll(cardsToSave);
                saved += cardsToSave.size();
            } catch (Exception e) {
                logger.warn("Error saving final batch: {}", e.getMessage());
            }

            logger.info("Saved {} new Magic cards from Scryfall", saved);
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
