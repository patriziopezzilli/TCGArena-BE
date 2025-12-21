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
 * Client for JustTCG API - provides real-time pricing data for TCGs
 * Supports: MTG, Pokemon, Yu-Gi-Oh!, Lorcana, One Piece, Digimon
 * 
 * Features:
 * - Full hierarchy management (Expansion → TCGSet → CardTemplate)
 * - Paginated requests with cursor support
 * - Duplicate prevention at all levels
 * - Rate limiting to avoid API throttling
 */
@Service
public class JustTCGApiClient {

    private static final Logger logger = LoggerFactory.getLogger(JustTCGApiClient.class);

    // Rate limiting: delay between API calls (ms)
    // Rate limiting: delay between API calls (ms) -> increased to 3s to avoid 429
    private static final long API_DELAY_MS = 3000;
    // Max concurrent set imports
    private static final int MAX_CONCURRENCY = 1; // Serial processing to be safe
    // Page size for card fetching
    private static final int PAGE_SIZE = 20;

    private final WebClient webClient;

    // Cache to avoid repeated DB lookups during import
    private final Map<String, Expansion> expansionCache = new ConcurrentHashMap<>();
    private final Map<String, TCGSet> tcgSetCache = new ConcurrentHashMap<>();

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private TCGSetRepository tcgSetRepository;

    @Autowired
    private com.tcg.arena.repository.ImportProgressRepository importProgressRepository;

    @Value("${justtcg.api.key}")
    private String apiKey;

    // Mapping from internal TCGType to JustTCG game IDs (from /games endpoint)
    private static final Map<TCGType, String> TCG_TYPE_TO_GAME_ID = Map.of(
            TCGType.MAGIC, "magic-the-gathering",
            TCGType.POKEMON, "pokemon",
            TCGType.YUGIOH, "yugioh",
            TCGType.LORCANA, "disney-lorcana",
            TCGType.ONE_PIECE, "one-piece-card-game",
            TCGType.DIGIMON, "digimon-card-game",
            TCGType.FLESH_AND_BLOOD, "flesh-and-blood-tcg");

    public JustTCGApiClient(@Value("${justtcg.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024)) // 50MB buffer
                .build();
    }

    // ===================== DTO classes for JustTCG API responses
    // =====================

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGGame {
        public String id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGSet {
        public String id;
        public String name;
        @JsonProperty("game_id")
        public String gameId;
        public String game;
        public Integer count;
        @JsonProperty("cards_count")
        public Integer cardsCount;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGCard {
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
        public List<JustTCGVariant> variants;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGVariant {
        public String id;
        public String printing;
        public String condition;
        public Double price;
        public Long lastUpdated;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGCardsResponse {
        @JsonProperty("data")
        public List<JustTCGCard> data;
        public List<JustTCGCard> cards; // fallback
        @JsonProperty("hasMore")
        public boolean hasMore;
        @JsonProperty("nextCursor")
        public String nextCursor;
        public Integer total;
        // Transient field for internal pagination tracking
        public int currentOffset;

        public List<JustTCGCard> getCards() {
            return data != null ? data : (cards != null ? cards : Collections.emptyList());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JustTCGSetsResponse {
        @JsonProperty("data")
        public List<JustTCGSet> data;
        public List<JustTCGSet> sets; // fallback
        @JsonProperty("hasMore")
        public boolean hasMore;
        @JsonProperty("nextCursor")
        public String nextCursor;

        public List<JustTCGSet> getSets() {
            return data != null ? data : (sets != null ? sets : Collections.emptyList());
        }
    }

    // ===================== API Methods =====================

    /**
     * Get all available games from JustTCG to discover valid game IDs
     */
    public Mono<List<JustTCGGame>> getGames() {
        return webClient.get()
                .uri("/games")
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(JustTCGGame[].class)
                .map(games -> List.of(games))
                .doOnSuccess(games -> logger.info("Available JustTCG games: {}",
                        games.stream().map(g -> g.id + " (" + g.name + ")").toList()))
                .onErrorResume(e -> {
                    logger.error("Error fetching games: {}", e.getMessage());
                    return Mono.just(Collections.emptyList());
                });
    }

    /**
     * Get sets for a specific game with pagination
     */
    public Flux<JustTCGSet> getAllSets(String gameId) {
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

    private Mono<JustTCGSetsResponse> getSetsPage(String gameId, String cursor) {
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
                .bodyToMono(JustTCGSetsResponse.class)
                .doOnSuccess(resp -> logger.info("Fetched sets for {}: {} sets found, hasMore: {}",
                        gameId, resp.getSets().size(), resp.hasMore))
                .onErrorResume(e -> {
                    logger.error("Error fetching sets for {}: {}", gameId, e.getMessage());
                    return Mono.just(new JustTCGSetsResponse());
                });
    }

    /**
     * Get all cards for a set with pagination
     */
    public Flux<JustTCGCard> getAllCardsForSet(String setId) {
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
     * JustTCG API requires game parameter, not set
     */
    /**
     * Get all cards for a game (not a set) with pagination
     * JustTCG API requires game parameter, not set
     * Uses offset-based pagination
     */
    /**
     * Get all card pages for a game starting from a specific offset
     * Returns Flux of JustTCGCardsResponse to allow processing per page
     */
    public Flux<JustTCGCardsResponse> getCardPagesForGame(String gameId, int startOffset) {
        return getCardsPageByGame(gameId, startOffset)
                .expand(response -> {
                    List<JustTCGCard> cards = response.getCards();
                    if (!cards.isEmpty()) {
                        int nextOffset = response.currentOffset + PAGE_SIZE;
                        return getCardsPageByGame(gameId, nextOffset)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    return Mono.empty();
                });
    }

    /**
     * Legacy method helper (fetches all from 0)
     */
    public Flux<JustTCGCard> getAllCardsForGame(String gameId) {
        return getCardPagesForGame(gameId, 0)
                .flatMapIterable(JustTCGCardsResponse::getCards);
    }

    private Mono<JustTCGCardsResponse> getCardsPageByGame(String gameId, int offset) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("game", gameId)
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .build())
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(JustTCGCardsResponse.class)
                .map(response -> {
                    response.currentOffset = offset;
                    return response;
                })
                .doOnSuccess(resp -> logger.info("Fetched cards page for game {}: {} cards (offset: {})",
                        gameId, resp.getCards().size(), offset))
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for game {} (offset {}): {}", gameId, offset, e.getMessage());
                    // Return empty response but preserve the CURRENT offset (not 0)
                    // This allows the main flow to detect error and stop without saving wrong
                    // offset
                    JustTCGCardsResponse errorResponse = new JustTCGCardsResponse();
                    errorResponse.currentOffset = offset; // Preserve offset where error occurred
                    return Mono.just(errorResponse);
                });
    }

    private Mono<JustTCGCardsResponse> getCardsPage(String setId, String cursor) {
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
                .bodyToMono(JustTCGCardsResponse.class)
                .doOnSuccess(resp -> logger.debug("Fetched cards page for set {}, count: {}, hasMore: {}",
                        setId, resp.getCards().size(), resp.hasMore))
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for set {}: {}", setId, e.getMessage());
                    return Mono.just(new JustTCGCardsResponse());
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
        String gameId = TCG_TYPE_TO_GAME_ID.get(tcgType);
        if (gameId == null) {
            logger.warn("TCG type {} not supported by JustTCG", tcgType);
            return Mono.just(0);
        }

        // Clear caches at start of import
        expansionCache.clear();
        tcgSetCache.clear();

        logger.info("Starting JustTCG import for {} (game: {})", tcgType.getDisplayName(), gameId);

        // Get current progress
        ImportProgress progress = getOrCreateImportProgress(tcgType);
        int startOffset = progress.getLastOffset();
        logger.info("Resuming import from offset: {}", startOffset);

        // Step 1: Fetch all sets first to get full metadata (only if starting fresh or
        // needed)
        // For simplicity, we always refresh sets metadata as it's quick
        return getAllSets(gameId)
                .collectList()
                .flatMap(sets -> {
                    logger.info("Fetched {} sets for {}", sets.size(), gameId);

                    // Create all sets in DB first with proper metadata
                    Map<String, TCGSet> setMap = new HashMap<>();
                    for (JustTCGSet justSet : sets) {
                        try {
                            TCGSet tcgSet = getOrCreateTCGSet(justSet, tcgType);
                            setMap.put(justSet.id, tcgSet);
                        } catch (Exception e) {
                            logger.warn("Error creating set {}: {}", justSet.name, e.getMessage());
                        }
                    }
                    logger.info("Created/loaded {} sets in DB", setMap.size());

                    // Step 2: Fetch cards page by page starting from offset
                    // Track last successful offset
                    final int[] lastSuccessfulOffset = { startOffset > 0 ? startOffset - PAGE_SIZE : 0 };

                    return getCardPagesForGame(gameId, startOffset)
                            .concatMap(response -> { // concatMap ensures sequential processing
                                List<JustTCGCard> cards = response.getCards();

                                if (cards.isEmpty()) {
                                    // Empty response could mean:
                                    // 1. End of data (normal completion)
                                    // 2. Error occurred (check if offset matches last successful)

                                    if (response.currentOffset == 0 && lastSuccessfulOffset[0] > 0) {
                                        // Error case: offset was reset to 0, use last successful offset
                                        logger.warn(
                                                "Import stopped due to error at offset {}. Last successful offset: {}",
                                                response.currentOffset, lastSuccessfulOffset[0]);
                                        updateProgress(tcgType, lastSuccessfulOffset[0], false);
                                    } else {
                                        // Normal completion: no more cards
                                        logger.info("Import completed successfully. Total offset reached: {}",
                                                lastSuccessfulOffset[0]);
                                        updateProgress(tcgType, lastSuccessfulOffset[0], true);
                                    }
                                    return Mono.empty(); // Stop the stream
                                }

                                // Process cards in this page
                                int savedInPage = 0;
                                for (JustTCGCard card : cards) {
                                    try {
                                        String setId = card.set != null ? card.set : "unknown";
                                        TCGSet tcgSet = setMap.get(setId);
                                        if (tcgSet == null) {
                                            String setName = card.setName != null ? card.setName : setId;
                                            tcgSet = getOrCreateTCGSetForCard(setId, setName, tcgType);
                                            setMap.put(setId, tcgSet);
                                        }

                                        if (saveCardIfNotExists(card, tcgSet, tcgType)) {
                                            savedInPage++;
                                        }
                                    } catch (Exception e) {
                                        logger.warn("Error saving card {}: {}", card.name, e.getMessage());
                                    }
                                }

                                // Update last successful offset to CURRENT page offset
                                lastSuccessfulOffset[0] = response.currentOffset;

                                // Save progress after successful page processing
                                // Save CURRENT offset (where we successfully processed), not next
                                updateProgress(tcgType, response.currentOffset, false);
                                logger.debug("Progress saved at offset: {} ({} cards saved)", response.currentOffset,
                                        savedInPage);

                                return Mono.just(savedInPage);
                            })
                            .onErrorResume(e -> {
                                logger.error("Error during import for {}: {}", gameId, e.getMessage(), e);
                                // Progress was already saved for successful pages
                                logger.info("Import stopped at offset: {}", lastSuccessfulOffset[0]);
                                return Mono.empty(); // Stop processing
                            })
                            .reduce(0, Integer::sum)
                            .doOnSuccess(total -> logger.info("Import complete for {}: {} new cards imported", gameId,
                                    total));
                })
                .doFinally(signal -> {
                    expansionCache.clear();
                    tcgSetCache.clear();
                });
    }

    private ImportProgress getOrCreateImportProgress(TCGType tcgType) {
        return importProgressRepository.findByTcgType(tcgType)
                .orElseGet(() -> {
                    ImportProgress p = new ImportProgress(tcgType);
                    return importProgressRepository.save(p);
                });
    }

    // Removed @Transactional as it is ignored on private methods called internally
    private void updateProgress(TCGType tcgType, int offset, boolean complete) {
        importProgressRepository.findByTcgType(tcgType).ifPresentOrElse(p -> {
            logger.info("Updating progress for {}: offset {} -> {}, complete={}", tcgType, p.getLastOffset(), offset,
                    complete);
            p.setLastOffset(offset);
            p.setLastUpdated(LocalDateTime.now()); // Explicitly update timestamp
            if (complete) {
                p.setComplete(true);
            } else {
                p.setComplete(false);
            }
            importProgressRepository.saveAndFlush(p);
            logger.info("Progress saved for {} (ID: {}). New offset: {}", tcgType, p.getId(), p.getLastOffset());
        }, () -> {
            logger.error("Could not find import progress for {}", tcgType);
        });
    }

    /**
     * Get or create TCGSet for a card (simplified method for direct card import)
     */
    @Transactional
    private synchronized TCGSet getOrCreateTCGSetForCard(String setId, String setName, TCGType tcgType) {
        // Check cache first
        if (tcgSetCache.containsKey(setId)) {
            return tcgSetCache.get(setId);
        }

        // Check database for existing set by setCode
        Optional<TCGSet> existingSet = tcgSetRepository.findBySetCode(setId);
        if (existingSet.isPresent()) {
            tcgSetCache.put(setId, existingSet.get());
            return existingSet.get();
        }

        // Get or create the parent Expansion
        Expansion expansion = getOrCreateExpansion(setName, tcgType);

        // Create new TCGSet
        TCGSet tcgSet = new TCGSet();
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
    private synchronized TCGSet getOrCreateTCGSet(JustTCGSet justTCGSet, TCGType tcgType) {
        // Check cache first
        String cacheKey = justTCGSet.id;
        if (tcgSetCache.containsKey(cacheKey)) {
            return tcgSetCache.get(cacheKey);
        }

        // Check database for existing set by setCode
        Optional<TCGSet> existingSet = tcgSetRepository.findBySetCode(justTCGSet.id);
        if (existingSet.isPresent()) {
            tcgSetCache.put(cacheKey, existingSet.get());
            return existingSet.get();
        }

        // Get or create the parent Expansion
        Expansion expansion = getOrCreateExpansion(justTCGSet.name, tcgType);

        // Create new TCGSet
        TCGSet tcgSet = new TCGSet();
        tcgSet.setName(justTCGSet.name);
        tcgSet.setSetCode(justTCGSet.id);
        tcgSet.setExpansion(expansion);
        tcgSet.setCardCount(justTCGSet.cardsCount != null ? justTCGSet.cardsCount : 0);
        tcgSet.setReleaseDate(LocalDateTime.now()); // JustTCG doesn't provide release date

        tcgSet = tcgSetRepository.save(tcgSet);
        tcgSetCache.put(cacheKey, tcgSet);

        logger.debug("Created new TCGSet: {} ({})", tcgSet.getName(), tcgSet.getSetCode());
        return tcgSet;
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
    private boolean saveCardIfNotExists(JustTCGCard card, TCGSet tcgSet, TCGType tcgType) {
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
     * Extract and set all prices from JustTCG variants
     */
    private void setPricesFromVariants(CardTemplate template, List<JustTCGVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return;
        }

        Double lowPrice = null;
        Double highPrice = null;

        for (JustTCGVariant variant : variants) {
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
    private void updateSetCardCount(TCGSet tcgSet, int cardCount) {
        if (tcgSet.getCardCount() == null || tcgSet.getCardCount() != cardCount) {
            tcgSet.setCardCount(cardCount);
            tcgSetRepository.save(tcgSet);
        }
    }

    // ===================== Utility Methods =====================

    private Double extractNearMintPrice(List<JustTCGVariant> variants) {
        if (variants == null || variants.isEmpty()) {
            return null;
        }

        for (JustTCGVariant variant : variants) {
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
}
