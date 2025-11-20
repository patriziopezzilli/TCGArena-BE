package com.example.tcgbackend.service;

import com.example.tcgbackend.model.*;
import com.example.tcgbackend.repository.CardRepository;
import com.example.tcgbackend.repository.ImportProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TCGApiClient {

    private final WebClient webClient;
    private final WebClient onePieceWebClient;
    private final ObjectMapper objectMapper;
    private final CardRepository cardRepository;
    private final ImportProgressRepository importProgressRepository;

    // Rate limiting tracking
    private int requestsThisMinute = 0;
    private LocalDateTime lastRequestTime = LocalDateTime.now();

    // Scryfall rate limiting (10 requests/second)
    private int scryfallRequestsThisSecond = 0;
    private LocalDateTime lastScryfallRequestTime = LocalDateTime.now();

    @Value("${app.demo-env:false}")
    private boolean demoEnv;

    @Autowired
    public TCGApiClient(CardRepository cardRepository, ImportProgressRepository importProgressRepository) {
        this.cardRepository = cardRepository;
        this.importProgressRepository = importProgressRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.pokemontcg.io")
                .defaultHeader("X-Api-Key", System.getenv("POKEMON_TCG_API_KEY"))
                .build();
        this.onePieceWebClient = WebClient.builder()
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Flux<Card> fetchPokemonCards() {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.POKEMON);

        // Get or create import progress for Pokemon
        ImportProgress progress = getOrCreateProgress(TCGType.POKEMON);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping Pokemon import - recently completed and no need to check for updates yet");
            return Flux.empty();
        }

        // Determine starting page based on progress
        int startPage = progress.getLastProcessedPage() + 1;
        System.out.println("Starting Pokemon import from page " + startPage +
                          " (previously processed: " + progress.getLastProcessedPage() + " pages)");

        // If we need to check for updates (complete but old), start from page 1 to get current total
        if (progress.isComplete() && needsUpdateCheck(progress)) {
            startPage = 1;
            System.out.println("Checking for Pokemon card updates...");
        }

        // Start fetching from the determined page
        return fetchPokemonCardsFromPage(startPage, progress);
    }

    private ImportProgress getOrCreateProgress(TCGType tcgType) {
        Optional<ImportProgress> existingProgress = importProgressRepository.findByTcgType(tcgType);
        if (existingProgress.isPresent()) {
            return existingProgress.get();
        }

        // Create new progress entry
        ImportProgress newProgress = new ImportProgress(tcgType);
        newProgress.setLastProcessedPage(0);
        newProgress.setComplete(false);
        return importProgressRepository.save(newProgress);
    }

    private boolean shouldSkipImport(ImportProgress progress) {
        // In demo environment, never skip imports to allow repeated testing
        if (demoEnv) {
            System.out.println("Demo environment detected - forcing import execution");
            return false;
        }

        // If not complete, we need to continue importing
        if (!progress.isComplete()) {
            return false;
        }

        // If complete but we haven't checked recently, we should check for updates
        if (needsUpdateCheck(progress)) {
            return false;
        }

        // Complete and recently checked - skip
        return true;
    }

    private void resetProgressForDemo(TCGType tcgType) {
        if (!demoEnv) {
            return;
        }

        System.out.println("Demo environment: Resetting progress and clearing existing cards for " + tcgType);

        // Delete all existing cards for this TCG type
        cardRepository.deleteByTcgType(tcgType);

        // Reset import progress
        ImportProgress progress = getOrCreateProgress(tcgType);
        progress.setLastProcessedPage(0);
        progress.setComplete(false);
        progress.setTotalPagesKnown(null);
        progress.setLastCheckDate(null);
        progress.setLastUpdated(LocalDateTime.now());
        importProgressRepository.save(progress);

        System.out.println("Demo environment: Progress reset complete for " + tcgType);
    }

    private boolean needsUpdateCheck(ImportProgress progress) {
        if (progress.getLastCheckDate() == null) {
            return true;
        }

        // Check for updates every 6 hours if complete
        return progress.getLastCheckDate().isBefore(LocalDateTime.now().minusHours(6));
    }

    private Flux<Card> fetchPokemonCardsFromPage(int startPage, ImportProgress progress) {
        return fetchPokemonCardsFromAPI(startPage)
                .flatMapMany(response -> {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response);
                        int currentPage = jsonResponse.path("page").asInt();
                        int pageSize = jsonResponse.path("pageSize").asInt();
                        int totalCount = jsonResponse.path("totalCount").asInt();
                        int totalPages = (int) Math.ceil((double) totalCount / pageSize);

                        // Update progress with known total pages
                        progress.setTotalPagesKnown(totalPages);
                        importProgressRepository.save(progress);

                        System.out.println("Pokemon API: Page " + currentPage + "/" + totalPages +
                                          " (Total cards: " + totalCount + ")");

                        // Safety check: if data array is empty, stop importing
                        JsonNode dataArray = jsonResponse.path("data");
                        if (dataArray.isArray() && dataArray.size() == 0) {
                            System.out.println("Pokemon API: Page " + currentPage + " has empty data array, stopping import");
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            return Flux.empty();
                        }

                        // If this is an update check and we have all cards, mark as checked and stop
                        if (progress.isComplete() && progress.getTotalPagesKnown() != null &&
                            totalPages <= progress.getLastProcessedPage()) {
                            System.out.println("No new Pokemon cards available");
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            return Flux.empty();
                        }

                        // Parse cards from current page
                        Flux<Card> currentPageCards = parsePokemonCards(response);

                        // Update progress for this page
                        progress.setLastProcessedPage(currentPage);
                        importProgressRepository.save(progress);

                        // If this is the last page, mark as complete
                        if (currentPage >= totalPages) {
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            System.out.println("Pokemon import completed! All " + totalCount + " cards imported.");
                            return currentPageCards;
                        }

                        // Continue with next page
                        return currentPageCards.concatWith(
                            fetchPokemonCardsFromPage(currentPage + 1, progress)
                        );

                    } catch (Exception e) {
                        System.err.println("Error parsing Pokemon API response: " + e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private Flux<Card> fetchPokemonCardsFromPage(int startPage, int totalPages) {
        return Flux.range(startPage, totalPages - startPage + 1)
                .flatMap(page -> fetchPokemonCardsFromAPI(page)
                        .delayElement(getRateLimitDelay()) // Smart rate limiting
                        .onErrorResume(e -> {
                            System.err.println("Error fetching page " + page + ": " + e.getMessage());
                            if (isRateLimitError(e)) {
                                System.out.println("Rate limit hit, waiting longer before retry...");
                                return Mono.delay(Duration.ofSeconds(60))
                                        .then(fetchPokemonCardsFromAPI(page));
                            }
                            return Mono.empty();
                        }))
                .flatMap(this::parsePokemonCards);
    }

    private Mono<String> fetchPokemonCardsFromAPI(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/cards")
                        .queryParam("page", page)
                        .queryParam("pageSize", 250)
                        .queryParam("orderBy", "set.releaseDate")
                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    private Duration getRateLimitDelay() {
        LocalDateTime now = LocalDateTime.now();

        // Reset counter every minute
        if (now.minusMinutes(1).isAfter(lastRequestTime)) {
            requestsThisMinute = 0;
            lastRequestTime = now;
        }

        requestsThisMinute++;

        // Pokemon TCG API typically allows 1000 requests per hour (about 16-17 per minute)
        // We'll be conservative and limit to 10 per minute
        if (requestsThisMinute >= 10) {
            long millisToWait = Duration.between(now, lastRequestTime.plusMinutes(1)).toMillis();
            return Duration.ofMillis(Math.max(millisToWait, 6000)); // At least 6 seconds
        }

        // Normal delay between requests
        return Duration.ofMillis(200); // 200ms delay = max 5 requests/second
    }

    private Duration getScryfallRateLimitDelay() {
        LocalDateTime now = LocalDateTime.now();

        // Reset counter every second for Scryfall (10 requests/second limit)
        if (now.minusSeconds(1).isAfter(lastScryfallRequestTime)) {
            scryfallRequestsThisSecond = 0;
            lastScryfallRequestTime = now;
        }

        scryfallRequestsThisSecond++;

        // Scryfall API allows max 10 requests per second
        if (scryfallRequestsThisSecond >= 10) {
            long millisToWait = Duration.between(now, lastScryfallRequestTime.plusSeconds(1)).toMillis();
            return Duration.ofMillis(Math.max(millisToWait, 100)); // At least 100ms
        }

        // Normal delay between requests (100ms = max 10 requests/second)
        return Duration.ofMillis(100);
    }

    private boolean isRateLimitError(Throwable e) {
        if (e instanceof WebClientResponseException) {
            WebClientResponseException we = (WebClientResponseException) e;
            return we.getStatusCode().value() == 429; // Too Many Requests
        }
        return false;
    }

    private Flux<Card> parsePokemonCards(String jsonResponse) {
        List<Card> cards = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data");

            for (JsonNode cardNode : data) {
                Card card = parsePokemonCard(cardNode);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Pokemon cards: " + e.getMessage());
        }

        return Flux.fromIterable(cards);
    }

    private Card parsePokemonCard(JsonNode cardNode) {
        try {
            Card card = new Card();

            // Basic info
            card.setName(cardNode.path("name").asText());
            card.setTcgType(TCGType.POKEMON);

            // Set and card number
            JsonNode setNode = cardNode.path("set");
            if (!setNode.isMissingNode()) {
                card.setSetCode(setNode.path("id").asText());
                card.setCardNumber(cardNode.path("number").asText());

                // Store expansion name for later processing
                card.setDescription(card.getDescription() + " [Expansion: " + setNode.path("name").asText() + "]");
            }

            // Rarity
            String rarityStr = cardNode.path("rarity").asText();
            card.setRarity(mapPokemonRarity(rarityStr));

            // Images
            JsonNode images = cardNode.path("images");
            if (!images.isMissingNode()) {
                card.setImageUrl(images.path("large").asText());
            }

            // Description/flavor text
            card.setDescription(cardNode.path("flavorText").asText());

            // Market price (simplified - would need additional API call)
            card.setMarketPrice(1.0); // Default price

            // Condition
            card.setCondition(CardCondition.NEAR_MINT);

            // Other fields
            card.setManaCost(cardNode.path("convertedEnergyCost").asInt());

            return card;
        } catch (Exception e) {
            System.err.println("Error parsing individual Pokemon card: " + e.getMessage());
            return null;
        }
    }

    private Rarity mapPokemonRarity(String rarityStr) {
        if (rarityStr == null || rarityStr.isEmpty()) {
            return Rarity.COMMON;
        }

        return switch (rarityStr.toLowerCase()) {
            case "common" -> Rarity.COMMON;
            case "uncommon" -> Rarity.UNCOMMON;
            case "rare" -> Rarity.RARE;
            case "rare holo" -> Rarity.RARE;
            case "rare holo ex" -> Rarity.RARE;
            case "rare holo gx" -> Rarity.RARE;
            case "rare holo v" -> Rarity.RARE;
            case "rare holo vmax" -> Rarity.RARE;
            case "rare ultra" -> Rarity.RARE;
            case "rare secret" -> Rarity.RARE;
            case "amazing rare" -> Rarity.RARE;
            case "legendary" -> Rarity.RARE;
            default -> Rarity.COMMON;
        };
    }

    public Flux<Card> fetchMagicCards() {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.MAGIC);

        // Get or create import progress for Magic
        ImportProgress progress = getOrCreateProgress(TCGType.MAGIC);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping Magic import - recently completed and no need to check for updates yet");
            return Flux.empty();
        }

        // Determine starting page based on progress
        int startPage = progress.getLastProcessedPage() + 1;
        System.out.println("Starting Magic import from page " + startPage +
                          " (previously processed: " + progress.getLastProcessedPage() + " pages)");

        // If we need to check for updates (complete but old), start from page 1 to get current total
        if (progress.isComplete() && needsUpdateCheck(progress)) {
            startPage = 1;
            System.out.println("Checking for Magic card updates...");
        }

        // Start fetching from the determined page
        return fetchMagicCardsFromPage(startPage, progress);
    }

    public Flux<Card> fetchOnePieceCards() {
        return fetchOnePieceCardsInternal(Integer.MAX_VALUE); // No limit
    }

    public Flux<Card> fetchOnePieceCardsLimited(int maxPages) {
        return fetchOnePieceCardsInternal(maxPages);
    }

    private Flux<Card> fetchOnePieceCardsInternal(int maxPages) {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.ONE_PIECE);

        // Get or create import progress for One Piece
        ImportProgress progress = getOrCreateProgress(TCGType.ONE_PIECE);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping One Piece import - recently completed and no need to check for updates yet");
            return Flux.empty();
        }

        // Determine starting page based on progress
        int startPage = progress.getLastProcessedPage() + 1;
        System.out.println("Starting One Piece import from page " + startPage +
                          " (previously processed: " + progress.getLastProcessedPage() + " pages, max pages: " + maxPages + ")");

        // If we need to check for updates (complete but old), start from page 1 to get current total
        if (progress.isComplete() && needsUpdateCheck(progress)) {
            startPage = 1;
            System.out.println("Checking for One Piece card updates...");
        }

        // Start fetching from the determined page with page limit
        return fetchOnePieceCardsFromPage(startPage, progress, maxPages);
    }

    private Flux<Card> fetchMagicCardsFromPage(int startPage, ImportProgress progress) {
        return fetchMagicCardsFromAPI(startPage)
                .flatMapMany(response -> {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response);

                        // Scryfall pagination structure
                        boolean hasMore = jsonResponse.path("has_more").asBoolean();
                        int totalCards = jsonResponse.path("total_cards").asInt();
                        String nextPageUrl = hasMore ? jsonResponse.path("next_page").asText() : null;

                        // Calculate current page and total pages
                        int currentPage = startPage;
                        int pageSize = 175; // Scryfall default page size
                        int totalPages = (int) Math.ceil((double) totalCards / pageSize);

                        // Update progress
                        progress.setTotalPagesKnown(totalPages);
                        importProgressRepository.save(progress);

                        System.out.println("Magic API: Page " + currentPage + "/" + totalPages +
                                          " (Total cards: " + totalCards + ", Has more: " + hasMore + ")");

                        // Safety check: if data array is empty, stop importing
                        JsonNode dataArray = jsonResponse.path("data");
                        if (dataArray.isArray() && dataArray.size() == 0) {
                            System.out.println("Magic API: Page " + currentPage + " has empty data array, stopping import");
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            return Flux.empty();
                        }

                        // Parse cards from current page
                        Flux<Card> currentPageCards = parseMagicCards(response);

                        // Update progress for this page
                        progress.setLastProcessedPage(currentPage);
                        importProgressRepository.save(progress);

                        // If no more pages, mark as complete
                        if (!hasMore) {
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            System.out.println("Magic import completed! All " + totalCards + " cards imported.");
                            return currentPageCards;
                        }

                        // Continue with next page
                        return currentPageCards.concatWith(
                            fetchMagicCardsFromPage(currentPage + 1, progress)
                        );

                    } catch (Exception e) {
                        System.err.println("Error parsing Magic API response: " + e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private Flux<Card> fetchOnePieceCardsFromPage(int startPage, ImportProgress progress, int maxPages) {
        return fetchOnePieceCardsFromAPI(startPage)
                .flatMapMany(response -> {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response);

                        // One Piece TCG API response structure
                        int currentPage = jsonResponse.path("page").asInt();
                        int limit = jsonResponse.path("limit").asInt();
                        int totalCards = jsonResponse.path("total").asInt();
                        int totalPages = jsonResponse.path("totalPages").asInt();

                        // Update progress
                        progress.setTotalPagesKnown(totalPages);
                        importProgressRepository.save(progress);

                        System.out.println("One Piece API: Page " + currentPage + "/" + totalPages +
                                          " (Total cards: " + totalCards + ", Limit: " + limit + ")");

                        // Parse cards from current page
                        Flux<Card> currentPageCards = parseOnePieceCards(response);

                        // Update progress for this page
                        progress.setLastProcessedPage(currentPage);
                        importProgressRepository.save(progress);

                        // In demo mode, limit to first few pages to avoid overwhelming the system
                        int maxPagesInDemo = 100; // Only import first 100 pages (10,000 cards) in demo mode
                        boolean isLastPage = currentPage >= totalPages;
                        if (demoEnv && currentPage >= maxPagesInDemo) {
                            isLastPage = true;
                            System.out.println("Demo mode: Limiting import to first " + maxPagesInDemo + " pages (" + (maxPagesInDemo * limit) + " cards max)");
                        }

                        // If this is the last page, mark as complete
                        if (isLastPage) {
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            int importedCards = demoEnv ? Math.min(totalCards, maxPagesInDemo * limit) : totalCards;
                            System.out.println("One Piece import completed! " + importedCards + " cards imported." +
                                             (demoEnv ? " (Limited by demo mode)" : ""));
                            return currentPageCards;
                        }

                        // Check if we've reached the max pages limit
                        if (currentPage >= maxPages) {
                            System.out.println("Reached max pages limit (" + maxPages + "), stopping import");
                            progress.setComplete(true);
                            progress.setLastCheckDate(LocalDateTime.now());
                            importProgressRepository.save(progress);
                            System.out.println("One Piece import completed! Limited to " + (currentPage * limit) + " cards (page limit).");
                            return currentPageCards;
                        }

                        // Continue with next page
                        return currentPageCards.concatWith(
                            fetchOnePieceCardsFromPage(currentPage + 1, progress, maxPages)
                        );

                    } catch (Exception e) {
                        System.err.println("Error parsing One Piece API response: " + e.getMessage());
                        return Flux.empty();
                    }
                });
    }

    private Mono<String> fetchMagicCardsFromAPI(int page) {
        // Scryfall API: Use /cards/search endpoint without query to get all cards
        // Pagination with page parameter, max 175 cards per page
        return webClient.get()
                .uri("https://api.scryfall.com/cards/search?page=" + page)
                .retrieve()
                .bodyToMono(String.class)
                .delayElement(getScryfallRateLimitDelay()); // Scryfall: max 10 requests/second
    }

    private Mono<String> fetchOnePieceCardsFromAPI(int page) {
        // Use the API endpoint directly
        String url = "https://apitcg.com/api/one-piece/cards?page=" + page + "&limit=100";
        System.out.println("Making One Piece API request to: " + url);

        // One Piece TCG API: Use correct endpoint with limit parameter (max 100 per page)
        WebClient.RequestHeadersSpec<?> request = onePieceWebClient.get()
                .uri(url);

        // Add API key header if available
        String apiKey = System.getenv("ONE_PIECE_API_KEY");
        if (apiKey != null && !apiKey.isEmpty()) {
            request = request.header("x-api-key", apiKey);
            System.out.println("Added API key header to One Piece request");
        } else {
            System.out.println("No ONE_PIECE_API_KEY environment variable found");
        }

        // Handle redirects manually but simply - max 1 redirect to prevent loops
        return request.exchangeToMono(response -> {
            if (response.statusCode().is3xxRedirection()) {
                // Get redirect location
                String location = response.headers().header("Location").stream().findFirst().orElse(null);
                if (location != null && !location.equals(url)) {
                    System.out.println("Following redirect to: " + location);
                    // Make second request to redirect location (only once to prevent loops)
                    WebClient.RequestHeadersSpec<?> redirectRequest = onePieceWebClient.get().uri(location);
                    if (apiKey != null && !apiKey.isEmpty()) {
                        redirectRequest = redirectRequest.header("x-api-key", apiKey);
                    }
                    return redirectRequest.retrieve().bodyToMono(String.class);
                } else if (location != null && location.equals(url)) {
                    System.out.println("Redirect location same as original URL, skipping to prevent loop: " + location);
                }
            }
            // For direct responses or when redirect location is same as original, return the body
            return response.bodyToMono(String.class);
        })
        .delayElement(getRateLimitDelay()) // Conservative rate limiting since not documented
        .doOnNext(body -> System.out.println("One Piece API response (first 200 chars): " + body.substring(0, Math.min(200, body.length()))))
        .doOnError(error -> System.out.println("One Piece API request failed: " + error.getMessage()));
    }

    private Flux<Card> parseMagicCards(String jsonResponse) {
        List<Card> cards = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data"); // Scryfall structure might be different

            for (JsonNode cardNode : data) {
                Card card = parseMagicCard(cardNode);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing Magic cards: " + e.getMessage());
        }

        return Flux.fromIterable(cards);
    }

    private Flux<Card> parseOnePieceCards(String jsonResponse) {
        List<Card> cards = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data");

            for (JsonNode cardNode : data) {
                Card card = parseOnePieceCard(cardNode);
                if (card != null) {
                    cards.add(card);
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing One Piece cards: " + e.getMessage());
        }

        return Flux.fromIterable(cards);
    }

    private Card parseMagicCard(JsonNode cardNode) {
        try {
            Card card = new Card();
            card.setTcgType(TCGType.MAGIC);

            // Map Scryfall fields to our Card model
            card.setName(cardNode.path("name").asText());
            card.setSetCode(cardNode.path("set").asText()); // set code

            // Map rarity to our enum
            String rarityStr = cardNode.path("rarity").asText();
            try {
                card.setRarity(Rarity.valueOf(rarityStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                card.setRarity(Rarity.COMMON); // Default fallback
            }

            // Image URLs - use normal size as default
            JsonNode imageUris = cardNode.path("image_uris");
            if (!imageUris.isMissingNode()) {
                card.setImageUrl(imageUris.path("normal").asText());
            }

            // Description - use oracle text
            card.setDescription(cardNode.path("oracle_text").asText());

            // Mana cost - convert CMC to Integer
            double cmc = cardNode.path("cmc").asDouble();
            card.setManaCost((int) Math.round(cmc));

            // Prices - use USD price
            JsonNode prices = cardNode.path("prices");
            if (!prices.isMissingNode()) {
                String usdPrice = prices.path("usd").asText();
                if (!usdPrice.isEmpty() && !"null".equals(usdPrice)) {
                    try {
                        card.setMarketPrice(Double.parseDouble(usdPrice));
                    } catch (NumberFormatException e) {
                        // Ignore invalid price
                    }
                }
            }

            // Set default values for required fields
            card.setCondition(CardCondition.NEAR_MINT);
            card.setDateAdded(LocalDateTime.now());
            card.setOwnerId(1L); // Default owner

            return card;
        } catch (Exception e) {
            System.err.println("Error parsing individual Magic card: " + e.getMessage());
            return null;
        }
    }

    private Card parseOnePieceCard(JsonNode cardNode) {
        try {
            Card card = new Card();
            card.setTcgType(TCGType.ONE_PIECE);

            // Map One Piece TCG fields to our Card model
            String cardName = cardNode.path("name").asText();
            if (cardName == null || cardName.trim().isEmpty()) {
                cardName = "Unknown Card";
            }
            card.setName(cardName);

            // Use code as card number (base code without _p1/_p2 suffix)
            String code = cardNode.path("code").asText();
            String cardNumber;
            if (!code.isEmpty()) {
                cardNumber = code;
            } else {
                // Fallback to id if code is not available
                String id = cardNode.path("id").asText();
                if (!id.isEmpty()) {
                    cardNumber = id;
                } else {
                    cardNumber = "UNKNOWN";
                }
            }
            card.setCardNumber(cardNumber);

            // Map rarity - One Piece uses various codes
            String rarityStr = cardNode.path("rarity").asText();
            try {
                switch (rarityStr.toUpperCase()) {
                    case "L":
                        card.setRarity(Rarity.RARE); // Leader cards are rare
                        break;
                    case "R":
                        card.setRarity(Rarity.RARE);
                        break;
                    case "SR":
                    case "SEC":
                        card.setRarity(Rarity.SECRET);
                        break;
                    case "UR":
                        card.setRarity(Rarity.ULTRA_SECRET);
                        break;
                    case "UC":
                        card.setRarity(Rarity.UNCOMMON);
                        break;
                    case "C":
                        card.setRarity(Rarity.COMMON);
                        break;
                    case "P":
                        card.setRarity(Rarity.RARE); // Promo cards as rare
                        break;
                    default:
                        card.setRarity(Rarity.COMMON); // Default fallback
                        break;
                }
            } catch (Exception e) {
                card.setRarity(Rarity.COMMON); // Default fallback
            }

            // Images - use large as default, small as alternative
            JsonNode images = cardNode.path("images");
            if (!images.isMissingNode()) {
                card.setImageUrl(images.path("large").asText());
                if (card.getImageUrl() == null || card.getImageUrl().isEmpty()) {
                    card.setImageUrl(images.path("small").asText());
                }
            }

            // Description - combine ability and trigger with more details
            StringBuilder description = new StringBuilder();

            // Add type information
            String type = cardNode.path("type").asText();
            if (!type.isEmpty()) {
                description.append("Type: ").append(type).append("\n");
            }

            // Add cost information
            if (cardNode.has("cost") && !cardNode.path("cost").isNull()) {
                description.append("Cost: ").append(cardNode.path("cost").asInt()).append("\n");
            }

            // Add power information
            if (cardNode.has("power") && !cardNode.path("power").isNull()) {
                description.append("Power: ").append(cardNode.path("power").asInt()).append("\n");
            }

            // Add counter information
            String counter = cardNode.path("counter").asText();
            if (!counter.isEmpty() && !counter.equals("-")) {
                description.append("Counter: ").append(counter).append("\n");
            }

            // Add color information
            String color = cardNode.path("color").asText();
            if (!color.isEmpty()) {
                description.append("Color: ").append(color).append("\n");
            }

            // Add family information
            String family = cardNode.path("family").asText();
            if (!family.isEmpty()) {
                description.append("Family: ").append(family).append("\n");
            }

            // Add ability
            String ability = cardNode.path("ability").asText();
            if (!ability.isEmpty() && !ability.equals("-")) {
                description.append("Ability: ").append(ability).append("\n");
            }

            // Add trigger
            String trigger = cardNode.path("trigger").asText();
            if (!trigger.isEmpty()) {
                description.append("Trigger: ").append(trigger).append("\n");
            }

            card.setDescription(description.toString().trim());

            // Cost as mana cost
            if (cardNode.has("cost") && !cardNode.path("cost").isNull()) {
                card.setManaCost(cardNode.path("cost").asInt());
            }

            // Set code from the set information
            JsonNode setNode = cardNode.path("set");
            if (!setNode.isMissingNode()) {
                String setName = setNode.path("name").asText();
                if (!setName.isEmpty()) {
                    card.setSetCode(setName);
                } else {
                    // Fallback: try to extract set code from card code
                    String cardCode = cardNode.path("code").asText();
                    if (!cardCode.isEmpty()) {
                        // Extract set code from card code (e.g., "OP01-001" -> "OP01")
                        String[] parts = cardCode.split("-");
                        if (parts.length > 0) {
                            card.setSetCode(parts[0]);
                        } else {
                            card.setSetCode("UNKNOWN");
                        }
                    } else {
                        card.setSetCode("UNKNOWN");
                    }
                }
            } else {
                // Fallback: try to extract set code from card code
                String cardCode = cardNode.path("code").asText();
                if (!cardCode.isEmpty()) {
                    // Extract set code from card code (e.g., "OP01-001" -> "OP01")
                    String[] parts = cardCode.split("-");
                    if (parts.length > 0) {
                        card.setSetCode(parts[0]);
                    } else {
                        card.setSetCode("UNKNOWN");
                    }
                } else {
                    card.setSetCode("UNKNOWN");
                }
            }

            return card;
        } catch (Exception e) {
            System.err.println("Error parsing One Piece card: " + e.getMessage());
            return null;
        }
    }

    public List<Card> fetchOnePieceCardsSynchronously(int maxPages) {
        List<Card> allCards = new ArrayList<>();
        try {
            for (int page = 1; page <= maxPages; page++) {
                System.out.println("Fetching One Piece page " + page + "/" + maxPages);
                String response = fetchOnePieceCardsFromAPI(page).block();
                if (response != null) {
                    // Check if the response contains data
                    JsonNode jsonResponse = objectMapper.readTree(response);
                    JsonNode dataArray = jsonResponse.path("data");
                    if (dataArray.isArray() && dataArray.size() == 0) {
                        System.out.println("Page " + page + ": data array is empty, stopping import");
                        break; // No more cards to fetch
                    }
                    
                    List<Card> pageCards = parseOnePieceCards(response).collectList().block();
                    if (pageCards != null) {
                        allCards.addAll(pageCards);
                        System.out.println("Page " + page + ": added " + pageCards.size() + " cards (total: " + allCards.size() + ")");
                    }
                }
                // Small delay between pages to be respectful to the API
                Thread.sleep(200);
            }
        } catch (Exception e) {
            System.err.println("Error fetching One Piece cards synchronously: " + e.getMessage());
        }
        return allCards;
    }

    // Legacy method for backward compatibility
    public Double getMarketPrice(String cardName, String setCode) {
        // Stub: return a random price
        return Math.random() * 100 + 1;
    }
}