package com.tcg.arena.service;

import com.tcg.arena.model.*;
import com.tcg.arena.repository.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for importing data from JustTCG API into the Arena* entity hierarchy.
 * Creates a complete dataset: ArenaGame → ArenaSet → ArenaCard →
 * ArenaCardVariant
 * 
 * This service is separate from TCGApiClient which imports into the legacy
 * Expansion → TCGSet → CardTemplate hierarchy.
 */
@Service
public class ArenaDataImportService {

    private static final Logger logger = LoggerFactory.getLogger(ArenaDataImportService.class);

    private static final long API_DELAY_MS = 3000; // Rate limiting delay
    private static final int PAGE_SIZE = 100;

    private final WebClient webClient;

    @Autowired
    private ArenaGameRepository arenaGameRepository;

    @Autowired
    private ArenaSetRepository arenaSetRepository;

    @Autowired
    private ArenaCardRepository arenaCardRepository;

    @Autowired
    private ArenaCardVariantRepository arenaCardVariantRepository;

    @Autowired
    private ArenaPriceStatisticsRepository arenaPriceStatisticsRepository;

    @Value("${justtcg.api.key}")
    private String apiKey;

    // Cache for current import session
    private final Map<String, ArenaGame> gameCache = new ConcurrentHashMap<>();
    private final Map<String, ArenaSet> setCache = new ConcurrentHashMap<>();

    public ArenaDataImportService(@Value("${justtcg.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(50 * 1024 * 1024))
                .build();
    }

    // ==================== Public Import Methods ====================

    /**
     * Import all games from JustTCG API.
     * 
     * @return Number of games imported
     */
    @Transactional
    public Mono<Integer> importGames() {
        logger.info("Starting Arena games import from JustTCG...");

        return fetchGames()
                .collectList()
                .map(games -> {
                    int count = 0;
                    for (TCGApiClient.TCGGame game : games) {
                        ArenaGame arenaGame = arenaGameRepository.findById(game.id)
                                .orElseGet(() -> new ArenaGame(game.id, game.name));
                        arenaGame.setName(game.name);
                        arenaGame.setLastSync(LocalDateTime.now());
                        arenaGameRepository.save(arenaGame);
                        gameCache.put(game.id, arenaGame);
                        count++;
                    }
                    logger.info("Imported {} games", count);
                    return count;
                });
    }

    /**
     * Import all sets for a specific game.
     * 
     * @param gameId JustTCG game ID (e.g., "pokemon", "magic-the-gathering")
     * @return Number of sets imported
     */
    @Transactional
    public Mono<Integer> importSetsForGame(String gameId) {
        logger.info("Starting Arena sets import for game: {}", gameId);

        return getOrCreateGame(gameId)
                .flatMap(game -> fetchSets(gameId)
                        .collectList()
                        .map(sets -> {
                            int count = 0;
                            for (TCGApiClient.TCGSet set : sets) {
                                ArenaSet arenaSet = arenaSetRepository.findById(set.id)
                                        .orElseGet(() -> new ArenaSet(set.id, set.name));
                                arenaSet.setName(set.name);
                                arenaSet.setGame(game);
                                arenaSet.setCardsCount(set.cardsCount != null ? set.cardsCount : set.count);
                                arenaSet.setLastSync(LocalDateTime.now());
                                arenaSetRepository.save(arenaSet);
                                setCache.put(set.id, arenaSet);
                                count++;
                            }
                            logger.info("Imported {} sets for game {}", count, gameId);
                            return count;
                        }));
    }

    /**
     * Full import: game → sets → cards → variants for a specific game.
     * 
     * @param gameId JustTCG game ID
     * @return Total number of cards imported
     */
    public Mono<Integer> fullImportForGame(String gameId) {
        logger.info("Starting FULL Arena import for game: {}", gameId);

        // Clear caches
        gameCache.clear();
        setCache.clear();

        return getOrCreateGame(gameId)
                .flatMap(game ->
                // Step 1: Import all sets
                fetchSets(gameId)
                        .collectList()
                        .flatMap(sets -> {
                            logger.info("Fetched {} sets for {}", sets.size(), gameId);

                            // Save all sets first
                            for (TCGApiClient.TCGSet set : sets) {
                                ArenaSet arenaSet = arenaSetRepository.findById(set.id)
                                        .orElseGet(() -> new ArenaSet(set.id, set.name));
                                arenaSet.setName(set.name);
                                arenaSet.setGame(game);
                                arenaSet.setCardsCount(set.cardsCount != null ? set.cardsCount : set.count);
                                arenaSet.setLastSync(LocalDateTime.now());
                                arenaSetRepository.save(arenaSet);
                                setCache.put(set.id, arenaSet);
                            }

                            // Step 2: Fetch all cards page by page
                            return fetchCardPages(gameId, 0)
                                    .concatMap(response -> {
                                        List<TCGApiClient.TCGCard> cards = response.getCards();
                                        if (cards.isEmpty()) {
                                            return Mono.empty();
                                        }

                                        int savedCount = 0;
                                        for (TCGApiClient.TCGCard card : cards) {
                                            if (card == null || card.name == null)
                                                continue;

                                            try {
                                                saveCard(card, game, gameId);
                                                savedCount++;
                                            } catch (Exception e) {
                                                logger.warn("Error saving card {}: {}", card.name, e.getMessage());
                                            }
                                        }

                                        logger.debug("Processed page at offset {}: {} cards",
                                                response.currentOffset, savedCount);
                                        return Mono.just(savedCount);
                                    })
                                    .reduce(0, Integer::sum);
                        }))
                .doOnSuccess(total -> {
                    logger.info("Full import complete for {}: {} cards imported", gameId, total);
                    gameCache.clear();
                    setCache.clear();
                })
                .doOnError(e -> {
                    logger.error("Error during full import for {}: {}", gameId, e.getMessage());
                    gameCache.clear();
                    setCache.clear();
                });
    }

    // ==================== Card Saving Logic ====================

    @Transactional
    protected void saveCard(TCGApiClient.TCGCard card, ArenaGame game, String gameId) {
        // Get or create set
        ArenaSet set = null;
        if (card.set != null) {
            set = setCache.get(card.set);
            if (set == null) {
                set = arenaSetRepository.findById(card.set).orElseGet(() -> {
                    ArenaSet newSet = new ArenaSet(card.set, card.setName != null ? card.setName : card.set);
                    newSet.setGame(game);
                    newSet.setLastSync(LocalDateTime.now());
                    return arenaSetRepository.save(newSet);
                });
                setCache.put(card.set, set);
            }
        }

        // Get or create card
        ArenaCard arenaCard = arenaCardRepository.findById(card.id).orElseGet(() -> {
            ArenaCard newCard = new ArenaCard(card.id, card.name);
            return newCard;
        });

        // Update card fields
        arenaCard.setName(card.name);
        arenaCard.setGame(game);
        arenaCard.setSet(set);
        arenaCard.setSetName(card.setName);
        arenaCard.setNumber(card.number);
        arenaCard.setTcgplayerId(card.tcgplayerId);
        arenaCard.setRarity(card.rarity);
        arenaCard.setDetails(card.details);
        arenaCard.setImageUrl(card.imageUrl);
        arenaCard.setLastSync(LocalDateTime.now());

        arenaCard = arenaCardRepository.save(arenaCard);

        // Save variants
        if (card.variants != null && !card.variants.isEmpty()) {
            saveVariants(arenaCard, card.variants);
        }
    }

    @Transactional
    protected void saveVariants(ArenaCard arenaCard, List<TCGApiClient.TCGVariant> variants) {
        for (TCGApiClient.TCGVariant variant : variants) {
            if (variant.id == null)
                continue;

            ArenaCardVariant arenaVariant = arenaCardVariantRepository.findById(variant.id)
                    .orElseGet(() -> {
                        ArenaCardVariant newVariant = new ArenaCardVariant();
                        newVariant.setId(variant.id);
                        newVariant.setCard(arenaCard);
                        return newVariant;
                    });

            // Parse condition
            ArenaCardCondition condition = ArenaCardCondition.fromString(variant.condition);
            if (condition == null) {
                condition = ArenaCardCondition.NEAR_MINT; // Default
            }
            arenaVariant.setCondition(condition);

            // Parse printing
            ArenaPrinting printing = ArenaPrinting.fromString(variant.printing);
            arenaVariant.setPrinting(printing);

            // Set price
            arenaVariant.setPrice(variant.price);
            arenaVariant.setLastUpdatedEpoch(variant.lastUpdated);

            arenaCardVariantRepository.save(arenaVariant);
        }
    }

    // ==================== API Fetching Methods ====================

    private Flux<TCGApiClient.TCGGame> fetchGames() {
        return webClient.get()
                .uri("/games")
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToFlux(TCGApiClient.TCGGame.class)
                .doOnNext(game -> logger.debug("Fetched game: {}", game.id))
                .onErrorResume(e -> {
                    logger.error("Error fetching games: {}", e.getMessage());
                    return Flux.empty();
                });
    }

    private Flux<TCGApiClient.TCGSet> fetchSets(String gameId) {
        return fetchSetsPage(gameId, null)
                .expand(response -> {
                    if (response.hasMore && response.nextCursor != null) {
                        return fetchSetsPage(gameId, response.nextCursor)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    return Mono.empty();
                })
                .flatMapIterable(TCGApiClient.TCGSetsResponse::getSets);
    }

    private Mono<TCGApiClient.TCGSetsResponse> fetchSetsPage(String gameId, String cursor) {
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
                .bodyToMono(TCGApiClient.TCGSetsResponse.class)
                .doOnSuccess(resp -> logger.debug("Fetched {} sets for {}", resp.getSets().size(), gameId))
                .onErrorResume(e -> {
                    logger.error("Error fetching sets for {}: {}", gameId, e.getMessage());
                    return Mono.just(new TCGApiClient.TCGSetsResponse());
                });
    }

    private Flux<TCGApiClient.TCGCardsResponse> fetchCardPages(String gameId, int startOffset) {
        return fetchCardsPage(gameId, startOffset)
                .expand(response -> {
                    List<TCGApiClient.TCGCard> cards = response.getCards();
                    if (!cards.isEmpty()) {
                        int nextOffset = response.currentOffset + PAGE_SIZE;
                        return fetchCardsPage(gameId, nextOffset)
                                .delaySubscription(Duration.ofMillis(API_DELAY_MS));
                    }
                    return Mono.empty();
                });
    }

    private Mono<TCGApiClient.TCGCardsResponse> fetchCardsPage(String gameId, int offset) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("game", gameId)
                        .queryParam("limit", PAGE_SIZE)
                        .queryParam("offset", offset)
                        .build())
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToMono(TCGApiClient.TCGCardsResponse.class)
                .map(response -> {
                    response.currentOffset = offset;
                    return response;
                })
                .doOnSuccess(resp -> {
                    if (resp != null && resp.getCards() != null) {
                        logger.info("Fetched cards page for {}: {} cards (offset: {})",
                                gameId, resp.getCards().size(), offset);
                    }
                })
                .onErrorResume(e -> {
                    logger.error("Error fetching cards for {} (offset {}): {}", gameId, offset, e.getMessage());
                    TCGApiClient.TCGCardsResponse errorResponse = new TCGApiClient.TCGCardsResponse();
                    errorResponse.currentOffset = offset;
                    return Mono.just(errorResponse);
                });
    }

    // ==================== Helper Methods ====================

    private Mono<ArenaGame> getOrCreateGame(String gameId) {
        if (gameCache.containsKey(gameId)) {
            return Mono.just(gameCache.get(gameId));
        }

        Optional<ArenaGame> existing = arenaGameRepository.findById(gameId);
        if (existing.isPresent()) {
            gameCache.put(gameId, existing.get());
            return Mono.just(existing.get());
        }

        // Fetch game name from API
        return webClient.get()
                .uri("/games")
                .header("x-api-key", apiKey)
                .retrieve()
                .bodyToFlux(TCGApiClient.TCGGame.class)
                .filter(g -> g.id.equals(gameId))
                .next()
                .map(g -> {
                    ArenaGame game = new ArenaGame(g.id, g.name);
                    game.setLastSync(LocalDateTime.now());
                    game = arenaGameRepository.save(game);
                    gameCache.put(gameId, game);
                    return game;
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Game not found in API, create with ID as name
                    ArenaGame game = new ArenaGame(gameId, gameId);
                    game.setLastSync(LocalDateTime.now());
                    ArenaGame saved = arenaGameRepository.save(game);
                    gameCache.put(gameId, saved);
                    return Mono.just(saved);
                }));
    }

    /**
     * Get list of supported game IDs from JustTCG.
     */
    public List<String> getSupportedGameIds() {
        return List.of(
                "pokemon",
                "magic-the-gathering",
                "yugioh",
                "disney-lorcana",
                "one-piece-card-game",
                "digimon-card-game",
                "union-arena");
    }
}
