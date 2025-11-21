package com.example.tcgbackend.service;

import com.example.tcgbackend.model.*;
import com.example.tcgbackend.repository.CardRepository;
import com.example.tcgbackend.repository.CardTemplateRepository;
import com.example.tcgbackend.repository.ImportProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.core.scheduler.Schedulers;
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
    private final WebClient scryfallWebClient;
    private final ObjectMapper objectMapper;
    private final CardRepository cardRepository;
    private final CardTemplateRepository cardTemplateRepository;
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
    public TCGApiClient(CardRepository cardRepository, CardTemplateRepository cardTemplateRepository, ImportProgressRepository importProgressRepository) {
        this.cardRepository = cardRepository;
        this.cardTemplateRepository = cardTemplateRepository;
        this.importProgressRepository = importProgressRepository;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.pokemontcg.io")
                .defaultHeader("X-Api-Key", System.getenv("POKEMON_TCG_API_KEY"))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024)) // Increase buffer limit to 2MB for large Pokemon API responses
                .build();
        this.onePieceWebClient = WebClient.builder()
                .build();
        this.scryfallWebClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // Increase buffer limit to 1MB for large Scryfall responses
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Mono<Void> fetchPokemonCards() {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.POKEMON);

        // Get or create import progress for Pokemon
        ImportProgress progress = getOrCreateProgress(TCGType.POKEMON);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping Pokemon import - recently completed and no need to check for updates yet");
            return Mono.empty();
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
        final ImportProgress importProgress = progress;
        final int startPageFinal = startPage;
        return Mono.fromRunnable(() -> {
            try {
                fetchPokemonCardsFromPageSync(startPageFinal, importProgress);
            } catch (Exception e) {
                System.err.println("Error during Pokemon import: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void fetchOnePieceCardsFromPageSync(int startPage, ImportProgress progress) {
        int currentPage = startPage;
        boolean continueImport = true;

        while (continueImport) {
            try {
                // Fetch current page
                String response = fetchOnePieceCardsFromAPI(currentPage).block();
                if (response == null) {
                    System.err.println("Failed to fetch One Piece cards for page " + currentPage);
                    break;
                }

                JsonNode jsonResponse = objectMapper.readTree(response);
                int limit = jsonResponse.path("limit").asInt();
                int totalCards = jsonResponse.path("total").asInt();
                int totalPages = jsonResponse.path("totalPages").asInt();

                // Update progress with known total pages
                progress.setTotalPagesKnown(totalPages);
                importProgressRepository.save(progress);

                System.out.println("One Piece API: Page " + currentPage + "/" + totalPages +
                                  " (Total cards: " + totalCards + ", Limit: " + limit + ")");

                // Safety check: if data array is empty, stop importing
                JsonNode dataArray = jsonResponse.path("data");
                if (dataArray.isArray() && dataArray.size() == 0) {
                    System.out.println("One Piece API: Page " + currentPage + " has empty data array, stopping import");
                    progress.setComplete(true);
                    progress.setLastCheckDate(LocalDateTime.now());
                    importProgressRepository.save(progress);
                    break;
                }

                // If this is an update check and we have all cards, mark as checked and stop
                if (progress.isComplete() && progress.getTotalPagesKnown() != null &&
                    totalPages <= progress.getLastProcessedPage()) {
                    System.out.println("No new One Piece cards available");
                    progress.setLastCheckDate(LocalDateTime.now());
                    importProgressRepository.save(progress);
                    break;
                }

                // Parse cards from current page (saves directly to database)
                parseOnePieceCards(response);

                // Update progress for this page
                progress.setLastProcessedPage(currentPage);
                importProgressRepository.save(progress);

                // If this is the last page, mark as complete
                if (currentPage >= totalPages) {
                    progress.setComplete(true);
                    progress.setLastCheckDate(LocalDateTime.now());
                    importProgressRepository.save(progress);
                    System.out.println("One Piece import completed! All " + totalCards + " cards imported.");
                    break;
                }

                // Move to next page
                currentPage++;

            } catch (Exception e) {
                System.err.println("Error processing One Piece page " + currentPage + ": " + e.getMessage());
                break;
            }
        }
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

        // Delete all existing card templates for this TCG type
        cardTemplateRepository.deleteByTcgType(tcgType);

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

    private void fetchPokemonCardsFromPageSync(int startPage, ImportProgress progress) {
        int currentPage = startPage;
        boolean continueImport = true;

        while (continueImport) {
            System.out.println("Pokemon: Starting processing of page " + currentPage);
            try {
                // Fetch current page
                System.out.println("Pokemon: Fetching data from API for page " + currentPage);
                String response = fetchPokemonCardsFromAPI(currentPage).block();
                if (response == null) {
                    System.err.println("Failed to fetch Pokemon cards for page " + currentPage);
                    break;
                }

                System.out.println("Pokemon: Received response for page " + currentPage + ", parsing JSON...");

                JsonNode jsonResponse = objectMapper.readTree(response);
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
                    break;
                }

                // If this is an update check and we have all cards, mark as checked and stop
                if (progress.isComplete() && progress.getTotalPagesKnown() != null &&
                    totalPages <= progress.getLastProcessedPage()) {
                    System.out.println("No new Pokemon cards available");
                    progress.setLastCheckDate(LocalDateTime.now());
                    importProgressRepository.save(progress);
                    break;
                }

                // Parse cards from current page (saves directly to database)
                System.out.println("Pokemon: Starting to parse page " + currentPage + " response (length: " + response.length() + " chars)");
                parsePokemonCards(response);
                System.out.println("Pokemon: Successfully parsed page " + currentPage);

                // Update progress for this page
                progress.setLastProcessedPage(currentPage);
                importProgressRepository.save(progress);

                // If this is the last page, mark as complete
                if (currentPage >= totalPages) {
                    progress.setComplete(true);
                    progress.setLastCheckDate(LocalDateTime.now());
                    importProgressRepository.save(progress);
                    System.out.println("Pokemon import completed! All " + totalCount + " cards imported.");
                    break;
                }

                // Move to next page
                currentPage++;

            } catch (Exception e) {
                System.err.println("Error processing Pokemon page " + currentPage + ": " + e.getMessage());
                System.err.println("Error type: " + e.getClass().getSimpleName());
                System.err.println("Stack trace:");
                e.printStackTrace();
                break;
            }
        }
    }

    private Mono<String> fetchPokemonCardsFromAPI(int page) {
        System.out.println("Pokemon: fetchPokemonCardsFromAPI called for page " + page);
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/cards")
                        .queryParam("page", page)
                        .queryParam("pageSize", 50)  // Further reduced to 50 for faster downloads
                        .queryParam("orderBy", "set.releaseDate")
                        .build())
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(120))  // Increased to 120 seconds for very slow connections
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(5)))  // Retry with exponential backoff
                .doOnNext(response -> System.out.println("Pokemon: API call successful for page " + page + ", response length: " + response.length()))
                .doOnError(error -> System.err.println("Pokemon: API call failed for page " + page + ": " + error.getMessage() + " (type: " + error.getClass().getSimpleName() + ")"));
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

    private Flux<Card> parsePokemonCards(String jsonResponse) throws com.fasterxml.jackson.core.JsonProcessingException {
        System.out.println("Pokemon: parsePokemonCards called with response length: " + jsonResponse.length());
        List<CardTemplate> templates = new ArrayList<>();
        try {
            System.out.println("Pokemon: Parsing JSON response...");
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data");

            System.out.println("Pokemon: Found " + data.size() + " cards in data array");

            for (int i = 0; i < data.size(); i++) {
                JsonNode cardNode = data.get(i);
                System.out.println("Pokemon: Processing card " + (i + 1) + "/" + data.size() + " - Name: " + cardNode.path("name").asText());
                CardTemplate template = parsePokemonCardTemplate(cardNode);
                if (template != null) {
                    templates.add(template);
                    System.out.println("Pokemon: Successfully parsed card template: " + template.getName());
                } else {
                    System.err.println("Pokemon: Failed to parse card template for card " + (i + 1));
                }
            }

            System.out.println("Parsed " + templates.size() + " Pokemon card templates from API response");

            if (!templates.isEmpty()) {
                // Save templates in smaller batches to reduce memory usage and database load
                final int BATCH_SIZE = 50;
                System.out.println("Saving " + templates.size() + " Pokemon card templates in batches of " + BATCH_SIZE);

                for (int i = 0; i < templates.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, templates.size());
                    List<CardTemplate> batch = templates.subList(i, endIndex);
                    System.out.println("Pokemon: Saving batch " + (i / BATCH_SIZE + 1) + "/" +
                                      ((templates.size() + BATCH_SIZE - 1) / BATCH_SIZE) +
                                      " (" + batch.size() + " templates)");
                    try {
                        cardTemplateRepository.saveAll(batch);
                        System.out.println("Pokemon: Successfully saved batch " + (i / BATCH_SIZE + 1));
                    } catch (Exception e) {
                        System.err.println("Pokemon: Error saving batch " + (i / BATCH_SIZE + 1) + ": " + e.getMessage());
                        throw e;
                    }
                }

                System.out.println("Successfully saved all " + templates.size() + " Pokemon card templates");
            } else {
                System.out.println("Pokemon: No templates to save");
            }
        } catch (Exception e) {
            System.err.println("Error parsing Pokemon cards: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-throw to propagate the error
        }

        return Flux.empty(); // Return empty since we're saving templates, not cards
    }

    private CardTemplate parsePokemonCardTemplate(JsonNode cardNode) {
        try {
            System.out.println("Pokemon: parsePokemonCardTemplate - Processing card: " + cardNode.path("name").asText());
            CardTemplate template = new CardTemplate();

            // Basic info
            String name = cardNode.path("name").asText();
            System.out.println("Pokemon: Setting name: " + name);
            template.setName(name);
            template.setTcgType(TCGType.POKEMON);

            // Set and card number
            JsonNode setNode = cardNode.path("set");
            if (!setNode.isMissingNode()) {
                String setCode = setNode.path("id").asText();
                String cardNumber = cardNode.path("number").asText();
                System.out.println("Pokemon: Setting setCode: " + setCode + ", cardNumber: " + cardNumber);
                template.setSetCode(setCode);
                template.setCardNumber(cardNumber);
            } else {
                System.out.println("Pokemon: No set information found for card");
            }

            // Rarity
            String rarityStr = cardNode.path("rarity").asText();
            Rarity rarity = mapPokemonRarity(rarityStr);
            System.out.println("Pokemon: Setting rarity: " + rarityStr + " -> " + rarity);
            template.setRarity(rarity);

            // Images
            JsonNode images = cardNode.path("images");
            if (!images.isMissingNode()) {
                template.setImageUrl(images.path("large").asText());
            }

            // Description/flavor text
            template.setDescription(cardNode.path("flavorText").asText());

            // Market price (simplified - would need additional API call)
            template.setMarketPrice(1.0); // Default price

            // Other fields
            template.setManaCost(cardNode.path("convertedEnergyCost").asInt());

            // Set creation date
            template.setDateCreated(LocalDateTime.now());

            System.out.println("Pokemon: Successfully created template for: " + template.getName());
            return template;
        } catch (Exception e) {
            System.err.println("Error parsing individual Pokemon card template: " + e.getMessage());
            System.err.println("Card data: " + cardNode.toString());
            e.printStackTrace();
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

    public Mono<Void> fetchMagicCards() {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.MAGIC);

        // Get or create import progress for Magic
        ImportProgress progress = getOrCreateProgress(TCGType.MAGIC);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping Magic import - recently completed and no need to check for updates yet");
            return Mono.empty();
        }

        // Define categories to import in smaller batches (~50-100 cards per category)
        List<String> categories = List.of(
            "color:red",      // Red cards
            "color:blue",     // Blue cards
            "color:black",    // Black cards
            "color:white",    // White cards
            "color:green",    // Green cards
            "colorless",      // Colorless cards
            "type:land",      // Lands
            "type:artifact",  // Artifacts
            "type:instant",   // Instants
            "type:sorcery",   // Sorceries
            "type:creature",  // Creatures
            "type:planeswalker" // Planeswalkers
        );

        System.out.println("Starting Magic import with " + categories.size() + " categories (~50-100 cards per category)");

        // Use synchronous sequential processing
        return Mono.fromRunnable(() -> {
            try {
                for (String category : categories) {
                    System.out.println("Processing Magic category: " + category);
                    fetchMagicCardsByCategorySync(category, progress);
                    System.out.println("Completed category: " + category);
                }

                // Mark as complete
                progress.setComplete(true);
                progress.setLastCheckDate(LocalDateTime.now());
                importProgressRepository.save(progress);
                System.out.println("Magic import completed! All categories processed.");

            } catch (Exception e) {
                System.err.println("Error during Magic import: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    private void fetchMagicCardsByCategorySync(String category, ImportProgress progress) {
        int page = 1;
        boolean hasMore = true;

        while (hasMore) {
            try {
                // Fetch page synchronously
                String response = fetchMagicCardsFromAPIByCategory(category, page).block();

                if (response == null) {
                    System.out.println("No response received for category " + category + ", page " + page);
                    break;
                }

                // Parse JSON response
                JsonNode jsonResponse;
                try {
                    jsonResponse = objectMapper.readTree(response);
                } catch (Exception e) {
                    System.err.println("Error parsing JSON response for category " + category + ", page " + page + ": " + e.getMessage());
                    break;
                }

                hasMore = jsonResponse.path("has_more").asBoolean();
                int totalCards = jsonResponse.path("total_cards").asInt();

                System.out.println("Magic API [" + category + "]: Page " + page +
                                  " (Total cards in category: " + totalCards + ", Has more: " + hasMore + ")");

                // Check for empty data
                JsonNode dataArray = jsonResponse.path("data");
                if (dataArray.isArray() && dataArray.size() == 0) {
                    System.out.println("Magic API [" + category + "]: Page " + page + " has empty data array, moving to next category");
                    break;
                }

                // Parse and save cards synchronously
                parseMagicCardsSync(response);

                page++;

            } catch (Exception e) {
                System.err.println("Error processing category " + category + ", page " + page + ": " + e.getMessage());
                break;
            }
        }
    }

    private void parseMagicCardsSync(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode data = root.path("data");

        List<CardTemplate> templates = new ArrayList<>();
        for (JsonNode cardNode : data) {
            CardTemplate template = parseMagicCardTemplate(cardNode);
            if (template != null) {
                templates.add(template);
            }
        }

        System.out.println("Parsed " + templates.size() + " Magic card templates from API response");

        if (!templates.isEmpty()) {
            // Save templates in smaller batches to reduce memory usage and database load
            final int BATCH_SIZE = 50;
            System.out.println("Saving " + templates.size() + " Magic card templates in batches of " + BATCH_SIZE);

            for (int i = 0; i < templates.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, templates.size());
                List<CardTemplate> batch = templates.subList(i, endIndex);
                System.out.println("Saving batch " + (i / BATCH_SIZE + 1) + "/" +
                                  ((templates.size() + BATCH_SIZE - 1) / BATCH_SIZE) +
                                  " (" + batch.size() + " templates)");
                cardTemplateRepository.saveAll(batch);
            }

            System.out.println("Successfully saved all " + templates.size() + " Magic card templates");
        }
    }

    public Mono<Void> fetchOnePieceCards() {
        // Reset progress and clear existing cards in demo environment
        resetProgressForDemo(TCGType.ONE_PIECE);

        // Get or create import progress for One Piece
        ImportProgress progress = getOrCreateProgress(TCGType.ONE_PIECE);

        // Check if we should skip import entirely
        if (shouldSkipImport(progress)) {
            System.out.println("Skipping One Piece import - recently completed and no need to check for updates yet");
            return Mono.empty();
        }

        // Determine starting page based on progress
        int startPage = progress.getLastProcessedPage() + 1;
        System.out.println("Starting One Piece import from page " + startPage +
                          " (previously processed: " + progress.getLastProcessedPage() + " pages)");

        // If we need to check for updates (complete but old), start from page 1 to get current total
        if (progress.isComplete() && needsUpdateCheck(progress)) {
            startPage = 1;
            System.out.println("Checking for One Piece card updates...");
        }

        // Start fetching from the determined page
        final ImportProgress importProgress = progress;
        final int startPageFinal = startPage;
        return Mono.fromRunnable(() -> {
            try {
                fetchOnePieceCardsFromPageSync(startPageFinal, importProgress);
            } catch (Exception e) {
                System.err.println("Error during One Piece import: " + e.getMessage());
                throw new RuntimeException(e);
            }
        });
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

    private Mono<Void> fetchMagicCardsFromPageByCategory(String category, int page, ImportProgress progress) {
        return fetchMagicCardsFromAPIByCategory(category, page)
                .flatMap(response -> {
                    try {
                        JsonNode jsonResponse = objectMapper.readTree(response);

                        // Scryfall pagination structure
                        boolean hasMore = jsonResponse.path("has_more").asBoolean();
                        int totalCards = jsonResponse.path("total_cards").asInt();

                        System.out.println("Magic API [" + category + "]: Page " + page +
                                          " (Total cards in category: " + totalCards + ", Has more: " + hasMore + ")");

                        // Safety check: if data array is empty, stop this category
                        JsonNode dataArray = jsonResponse.path("data");
                        if (dataArray.isArray() && dataArray.size() == 0) {
                            System.out.println("Magic API [" + category + "]: Page " + page + " has empty data array, moving to next category");
                            return Mono.empty();
                        }

                        // Parse and save cards from current page
                        return parseMagicCards(response)
                                .then(hasMore ? fetchMagicCardsFromPageByCategory(category, page + 1, progress) : Mono.empty());

                    } catch (Exception e) {
                        System.err.println("Error parsing Magic API response for category " + category + ": " + e.getMessage());
                        return Mono.empty();
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

    private Mono<String> fetchMagicCardsFromAPIByCategory(String category, int page) {
        // Scryfall API: Use /cards/search endpoint with specific category query
        // This reduces the number of cards per request (typically ~50-100 instead of 175)
        return scryfallWebClient.get()
                .uri("https://api.scryfall.com/cards/search?q=" + category + "&page=" + page)
                .exchangeToMono(response -> {
                    if (response.statusCode().isError()) {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException("HTTP error " + response.statusCode() + ": " + body)));
                    }
                    return response.bodyToMono(String.class);
                })
                .doOnNext(response -> System.out.println("Scryfall response received for [" + category + "], length: " + response.length() + ", starts with: " + response.substring(0, Math.min(100, response.length()))))
                .doOnError(e -> System.out.println("Error in fetchMagicCardsFromAPIByCategory [" + category + "]: " + e.getMessage() + ", type: " + e.getClass().getSimpleName()))
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

    private Mono<Void> parseMagicCards(String jsonResponse) {
        return Mono.fromCallable(() -> {
            try {
                JsonNode root = objectMapper.readTree(jsonResponse);
                JsonNode data = root.path("data");

                List<CardTemplate> templates = new ArrayList<>();
                for (JsonNode cardNode : data) {
                    CardTemplate template = parseMagicCardTemplate(cardNode);
                    if (template != null) {
                        templates.add(template);
                    }
                }

                System.out.println("Parsed " + templates.size() + " Magic card templates from API response");

                if (!templates.isEmpty()) {
                    // Save templates in smaller batches to reduce memory usage and database load
                    final int BATCH_SIZE = 50;
                    System.out.println("Saving " + templates.size() + " Magic card templates in batches of " + BATCH_SIZE);

                    for (int i = 0; i < templates.size(); i += BATCH_SIZE) {
                        int endIndex = Math.min(i + BATCH_SIZE, templates.size());
                        List<CardTemplate> batch = templates.subList(i, endIndex);
                        System.out.println("Saving batch " + (i / BATCH_SIZE + 1) + "/" +
                                          ((templates.size() + BATCH_SIZE - 1) / BATCH_SIZE) +
                                          " (" + batch.size() + " templates)");
                        cardTemplateRepository.saveAll(batch);
                    }

                    System.out.println("Successfully saved all " + templates.size() + " Magic card templates");
                }

                return null;
            } catch (Exception e) {
                System.err.println("Error parsing Magic cards: " + e.getMessage());
                throw e;
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(v -> System.out.println("Magic card template processing completed"))
        .doOnError(e -> System.err.println("Error in Magic card template processing: " + e.getMessage()))
        .then();
    }

    private Flux<Card> parseOnePieceCards(String jsonResponse) {
        List<CardTemplate> templates = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.path("data");

            for (JsonNode cardNode : data) {
                CardTemplate template = parseOnePieceCardTemplate(cardNode);
                if (template != null) {
                    templates.add(template);
                }
            }

            System.out.println("Parsed " + templates.size() + " One Piece card templates from API response");

            if (!templates.isEmpty()) {
                // Save templates in smaller batches to reduce memory usage and database load
                final int BATCH_SIZE = 50;
                System.out.println("Saving " + templates.size() + " One Piece card templates in batches of " + BATCH_SIZE);

                for (int i = 0; i < templates.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, templates.size());
                    List<CardTemplate> batch = templates.subList(i, endIndex);
                    System.out.println("Saving batch " + (i / BATCH_SIZE + 1) + "/" +
                                      ((templates.size() + BATCH_SIZE - 1) / BATCH_SIZE) +
                                      " (" + batch.size() + " templates)");
                    cardTemplateRepository.saveAll(batch);
                }

                System.out.println("Successfully saved all " + templates.size() + " One Piece card templates");
            }
        } catch (Exception e) {
            System.err.println("Error parsing One Piece cards: " + e.getMessage());
        }

        return Flux.empty(); // Return empty since we're saving templates, not cards
    }

    private CardTemplate parseMagicCardTemplate(JsonNode cardNode) {
        try {
            CardTemplate template = new CardTemplate();
            template.setTcgType(TCGType.MAGIC);

            // Map Scryfall fields to our CardTemplate model
            template.setName(cardNode.path("name").asText());
            template.setSetCode(cardNode.path("set").asText()); // set code

            // Card number - use collector number from Scryfall
            String collectorNumber = cardNode.path("collector_number").asText();
            template.setCardNumber(collectorNumber);

            // Map rarity to our enum
            String rarityStr = cardNode.path("rarity").asText();
            try {
                template.setRarity(Rarity.valueOf(rarityStr.toUpperCase()));
            } catch (IllegalArgumentException e) {
                template.setRarity(Rarity.COMMON); // Default fallback
            }

            // Image URLs - use normal size as default
            JsonNode imageUris = cardNode.path("image_uris");
            if (!imageUris.isMissingNode()) {
                template.setImageUrl(imageUris.path("normal").asText());
            }

            // Description - use oracle text
            template.setDescription(cardNode.path("oracle_text").asText());

            // Mana cost - convert CMC to Integer
            double cmc = cardNode.path("cmc").asDouble();
            template.setManaCost((int) Math.round(cmc));

            // Prices - use USD price
            JsonNode prices = cardNode.path("prices");
            if (!prices.isMissingNode()) {
                String usdPrice = prices.path("usd").asText();
                if (!usdPrice.isEmpty() && !"null".equals(usdPrice)) {
                    try {
                        template.setMarketPrice(Double.parseDouble(usdPrice));
                    } catch (NumberFormatException e) {
                        // Ignore invalid price
                    }
                }
            }

            // Set creation date
            template.setDateCreated(LocalDateTime.now());

            return template;
        } catch (Exception e) {
            System.err.println("Error parsing individual Magic card template: " + e.getMessage());
            return null;
        }
    }

    private CardTemplate parseOnePieceCardTemplate(JsonNode cardNode) {
        try {
            CardTemplate template = new CardTemplate();
            template.setTcgType(TCGType.ONE_PIECE);

            // Map One Piece TCG fields to our CardTemplate model
            String cardName = cardNode.path("name").asText();
            if (cardName == null || cardName.trim().isEmpty()) {
                cardName = "Unknown Card";
            }
            template.setName(cardName);

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
            template.setCardNumber(cardNumber);

            // Map rarity - One Piece uses various codes
            String rarityStr = cardNode.path("rarity").asText();
            try {
                switch (rarityStr.toUpperCase()) {
                    case "L":
                        template.setRarity(Rarity.RARE); // Leader cards are rare
                        break;
                    case "R":
                        template.setRarity(Rarity.RARE);
                        break;
                    case "SR":
                    case "SEC":
                        template.setRarity(Rarity.SECRET);
                        break;
                    case "UR":
                        template.setRarity(Rarity.ULTRA_SECRET);
                        break;
                    case "UC":
                        template.setRarity(Rarity.UNCOMMON);
                        break;
                    case "C":
                        template.setRarity(Rarity.COMMON);
                        break;
                    case "P":
                        template.setRarity(Rarity.RARE); // Promo cards as rare
                        break;
                    default:
                        template.setRarity(Rarity.COMMON); // Default fallback
                        break;
                }
            } catch (Exception e) {
                template.setRarity(Rarity.COMMON); // Default fallback
            }

            // Images - use large as default, small as alternative
            JsonNode images = cardNode.path("images");
            if (!images.isMissingNode()) {
                template.setImageUrl(images.path("large").asText());
                if (template.getImageUrl() == null || template.getImageUrl().isEmpty()) {
                    template.setImageUrl(images.path("small").asText());
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

            template.setDescription(description.toString().trim());

            // Cost as mana cost
            if (cardNode.has("cost") && !cardNode.path("cost").isNull()) {
                template.setManaCost(cardNode.path("cost").asInt());
            }

            // Set code from the set information
            JsonNode setNode = cardNode.path("set");
            if (!setNode.isMissingNode()) {
                String setName = setNode.path("name").asText();
                if (!setName.isEmpty()) {
                    template.setSetCode(setName);
                } else {
                    // Fallback: try to extract set code from card code
                    String cardCode = cardNode.path("code").asText();
                    if (!cardCode.isEmpty()) {
                        // Extract set code from card code (e.g., "OP01-001" -> "OP01")
                        String[] parts = cardCode.split("-");
                        if (parts.length > 0) {
                            template.setSetCode(parts[0]);
                        } else {
                            template.setSetCode("UNKNOWN");
                        }
                    } else {
                        template.setSetCode("UNKNOWN");
                    }
                }
            } else {
                // Fallback: try to extract set code from card code
                String cardCode = cardNode.path("code").asText();
                if (!cardCode.isEmpty()) {
                    // Extract set code from card code (e.g., "OP01-001" -> "OP01")
                    String[] parts = cardCode.split("-");
                    if (parts.length > 0) {
                        template.setSetCode(parts[0]);
                    } else {
                        template.setSetCode("UNKNOWN");
                    }
                } else {
                    template.setSetCode("UNKNOWN");
                }
            }

            // Set creation date
            template.setDateCreated(LocalDateTime.now());

            return template;
        } catch (Exception e) {
            System.err.println("Error parsing One Piece card template: " + e.getMessage());
            return null;
        }
    }

    // Legacy method for backward compatibility
    public Double getMarketPrice(String cardName, String setCode) {
        // Stub: return a random price
        return Math.random() * 100 + 1;
    }
}