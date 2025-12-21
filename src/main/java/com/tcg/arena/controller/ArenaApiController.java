package com.tcg.arena.controller;

import com.tcg.arena.dto.ArenaCardDTO;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Public API for Arena dataset.
 * Protected by API key authentication (X-Arena-Api-Key header).
 */
@RestController
@RequestMapping("/api/arena")
public class ArenaApiController {

    @Autowired
    private ArenaGameRepository gameRepository;

    @Autowired
    private ArenaSetRepository setRepository;

    @Autowired
    private ArenaCardRepository cardRepository;

    @Autowired
    private ArenaCardVariantRepository variantRepository;

    // ==================== Games ====================

    /**
     * GET /api/arena/games - List all games
     */
    @GetMapping("/games")
    public ResponseEntity<Map<String, Object>> getGames() {
        List<ArenaGame> games = gameRepository.findAllByOrderByNameAsc();

        List<Map<String, Object>> gameList = games.stream().map(g -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", g.getId());
            map.put("name", g.getName());
            map.put("lastSync", g.getLastSync());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "data", gameList,
                "count", games.size()));
    }

    /**
     * GET /api/arena/games/{id} - Get game details with set count
     */
    @GetMapping("/games/{id}")
    public ResponseEntity<?> getGame(@PathVariable String id) {
        return gameRepository.findById(id)
                .map(game -> {
                    long setCount = setRepository.countByGameId(id);
                    long cardCount = cardRepository.countByGameId(id);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", game.getId());
                    response.put("name", game.getName());
                    response.put("setsCount", setCount);
                    response.put("cardsCount", cardCount);
                    response.put("lastSync", game.getLastSync());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Sets ====================

    /**
     * GET /api/arena/sets - List sets (optionally filtered by game)
     */
    @GetMapping("/sets")
    public ResponseEntity<Map<String, Object>> getSets(
            @RequestParam(required = false) String game,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<ArenaSet> setsPage;

        if (game != null && !game.isBlank()) {
            setsPage = setRepository.findByGameId(game, pageable);
        } else {
            setsPage = setRepository.findAll(pageable);
        }

        List<Map<String, Object>> setList = setsPage.getContent().stream().map(s -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", s.getId());
            map.put("name", s.getName());
            map.put("gameId", s.getGameId());
            map.put("cardsCount", s.getCardsCount());
            map.put("releaseDate", s.getReleaseDate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "data", setList,
                "page", page,
                "size", setsPage.getNumberOfElements(),
                "totalPages", setsPage.getTotalPages(),
                "totalElements", setsPage.getTotalElements()));
    }

    /**
     * GET /api/arena/sets/{id} - Get set details
     */
    @GetMapping("/sets/{id}")
    public ResponseEntity<?> getSet(@PathVariable String id) {
        return setRepository.findById(id)
                .map(set -> {
                    long cardCount = cardRepository.countBySetId(id);

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("id", set.getId());
                    response.put("name", set.getName());
                    response.put("gameId", set.getGameId());
                    response.put("cardsCount", cardCount);
                    response.put("releaseDate", set.getReleaseDate());
                    response.put("lastSync", set.getLastSync());

                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Cards ====================

    /**
     * GET /api/arena/cards - Search/list cards
     */
    @GetMapping("/cards")
    public ResponseEntity<Map<String, Object>> getCards(
            @RequestParam(required = false) String game,
            @RequestParam(required = false) String set,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {

        // Get max batch size from API key
        ArenaApiKey apiKey = (ArenaApiKey) request.getAttribute("arenaApiKey");
        int maxSize = apiKey != null ? apiKey.getPlan().getMaxBatchSize() : 20;

        Pageable pageable = PageRequest.of(page, Math.min(size, maxSize));
        Page<ArenaCard> cardsPage;

        if (q != null && !q.isBlank()) {
            if (game != null && !game.isBlank()) {
                cardsPage = cardRepository.searchByNameAndGame(q, game, pageable);
            } else {
                cardsPage = cardRepository.searchByName(q, pageable);
            }
        } else if (set != null && !set.isBlank()) {
            cardsPage = cardRepository.findBySetId(set, pageable);
        } else if (game != null && !game.isBlank()) {
            cardsPage = cardRepository.findByGameId(game, pageable);
        } else {
            cardsPage = cardRepository.findAll(pageable);
        }

        List<ArenaCardDTO> cardList = cardsPage.getContent().stream()
                .map(ArenaCardDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "data", cardList,
                "page", page,
                "size", cardsPage.getNumberOfElements(),
                "totalPages", cardsPage.getTotalPages(),
                "totalElements", cardsPage.getTotalElements()));
    }

    /**
     * GET /api/arena/cards/{id} - Get card with variants
     */
    @GetMapping("/cards/{id}")
    public ResponseEntity<?> getCard(@PathVariable String id) {
        return cardRepository.findById(id)
                .map(card -> {
                    // Fetch variants
                    List<ArenaCardVariant> variants = variantRepository.findByCardId(id);
                    card.setVariants(variants);

                    return ResponseEntity.ok(new ArenaCardDTO(card));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/arena/cards/batch - Batch lookup by IDs
     */
    @PostMapping("/cards/batch")
    public ResponseEntity<?> batchLookup(
            @RequestBody BatchLookupRequest request,
            HttpServletRequest httpRequest) {

        if (request.ids == null || request.ids.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Missing ids array in request body"));
        }

        // Get max batch size from API key
        ArenaApiKey apiKey = (ArenaApiKey) httpRequest.getAttribute("arenaApiKey");
        int maxSize = apiKey != null ? apiKey.getPlan().getMaxBatchSize() : 20;

        if (request.ids.size() > maxSize) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", String.format("Batch size exceeds limit. Max: %d, requested: %d",
                            maxSize, request.ids.size()),
                    "maxBatchSize", maxSize));
        }

        List<ArenaCard> cards = cardRepository.findAllById(request.ids);

        // Fetch variants for each card
        cards.forEach(card -> {
            List<ArenaCardVariant> variants = variantRepository.findByCardId(card.getId());
            card.setVariants(variants);
        });

        List<ArenaCardDTO> cardDTOs = cards.stream()
                .map(ArenaCardDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of(
                "data", cardDTOs,
                "found", cards.size(),
                "requested", request.ids.size()));
    }

    // ==================== Lookup by external IDs ====================

    /**
     * GET /api/arena/cards/tcgplayer/{tcgplayerId} - Lookup by TCGPlayer ID
     */
    @GetMapping("/cards/tcgplayer/{tcgplayerId}")
    public ResponseEntity<?> getCardByTcgplayerId(@PathVariable String tcgplayerId) {
        return cardRepository.findByTcgplayerId(tcgplayerId)
                .map(card -> {
                    List<ArenaCardVariant> variants = variantRepository.findByCardId(card.getId());
                    card.setVariants(variants);
                    return ResponseEntity.ok(new ArenaCardDTO(card));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/arena/cards/scryfall/{scryfallId} - Lookup by Scryfall ID (MTG)
     */
    @GetMapping("/cards/scryfall/{scryfallId}")
    public ResponseEntity<?> getCardByScryfallId(@PathVariable String scryfallId) {
        return cardRepository.findByScryfallId(scryfallId)
                .map(card -> {
                    List<ArenaCardVariant> variants = variantRepository.findByCardId(card.getId());
                    card.setVariants(variants);
                    return ResponseEntity.ok(new ArenaCardDTO(card));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== Request DTOs ====================

    public static class BatchLookupRequest {
        public List<String> ids;
    }
}
