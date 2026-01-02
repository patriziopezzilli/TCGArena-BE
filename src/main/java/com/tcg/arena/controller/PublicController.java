package com.tcg.arena.controller;

import com.tcg.arena.dto.CommunityEventDTO;
import com.tcg.arena.dto.ShopDTO;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.Tournament;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.CommunityEventRepository;
import com.tcg.arena.service.ShopService;
import com.tcg.arena.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Public API endpoints for sharing content without authentication.
 * These endpoints are used by the web share pages (tcgarena.it/share/*)
 * to display shared content to users who don't have the app.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Public", description = "Public API endpoints for sharing content (no authentication required)")
public class PublicController {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private CommunityEventRepository communityEventRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    /**
     * Get shop details for sharing (verified shops only)
     */
    @GetMapping("/shops/{id}")
    @Operation(summary = "Get shop for sharing", description = "Retrieves public shop details for the share page (verified shops only)")
    public ResponseEntity<?> getShopForSharing(
            @Parameter(description = "ID of the shop") @PathVariable Long id) {
        return shopService.getShopById(id)
                .filter(shop -> Boolean.TRUE.equals(shop.getIsVerified()))
                .map(shop -> {
                    ShopDTO dto = new ShopDTO(shop);
                    // Add share-specific metadata
                    Map<String, Object> response = new HashMap<>();
                    response.put("shop", dto);
                    response.put("shareUrl", "https://tcgarena.it/share/shop/" + id);
                    response.put("deepLink", "tcgarena://shop/" + id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get tournament details for sharing
     */
    @GetMapping("/tournaments/{id}")
    @Operation(summary = "Get tournament for sharing", description = "Retrieves public tournament details for the share page")
    public ResponseEntity<?> getTournamentForSharing(
            @Parameter(description = "ID of the tournament") @PathVariable Long id) {
        return tournamentService.getTournamentById(id)
                .map(tournament -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("tournament", buildTournamentResponse(tournament));
                    response.put("shareUrl", "https://tcgarena.it/share/tournament/" + id);
                    response.put("deepLink", "tcgarena://tournament/" + id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get community event details for sharing
     */
    @GetMapping("/community-events/{id}")
    @Operation(summary = "Get community event for sharing", description = "Retrieves public community event details for the share page")
    public ResponseEntity<?> getCommunityEventForSharing(
            @Parameter(description = "ID of the community event") @PathVariable Long id) {
        return communityEventRepository.findById(id)
                .map(event -> {
                    // Use null for currentUserId since this is public access
                    CommunityEventDTO dto = CommunityEventDTO.fromEntity(event, null);
                    Map<String, Object> response = new HashMap<>();
                    response.put("event", dto);
                    response.put("shareUrl", "https://tcgarena.it/share/event/" + id);
                    response.put("deepLink", "tcgarena://event/" + id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get card template details for sharing
     */
    @GetMapping("/cards/{id}")
    @Operation(summary = "Get card for sharing", description = "Retrieves public card template details for the share page")
    public ResponseEntity<?> getCardForSharing(
            @Parameter(description = "ID of the card template") @PathVariable Long id) {
        return cardTemplateRepository.findById(id)
                .map(card -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("card", buildCardResponse(card));
                    response.put("shareUrl", "https://tcgarena.it/share/card/" + id);
                    response.put("deepLink", "tcgarena://card/" + id);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Build tournament response map with relevant fields
     */
    private Map<String, Object> buildTournamentResponse(Tournament tournament) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tournament.getId());
        map.put("title", tournament.getTitle());
        map.put("description", tournament.getDescription());
        map.put("tcgType", tournament.getTcgType());
        map.put("type", tournament.getType());
        map.put("status", tournament.getStatus());
        map.put("startDate", tournament.getStartDate());
        map.put("endDate", tournament.getEndDate());
        map.put("maxParticipants", tournament.getMaxParticipants());
        map.put("currentParticipants", tournament.getCurrentParticipants());
        map.put("entryFee", tournament.getEntryFee());
        map.put("prizePool", tournament.getPrizePool());
        map.put("isRanked", tournament.getIsRanked());
        map.put("externalRegistrationUrl", tournament.getExternalRegistrationUrl());

        // Location info
        if (tournament.getLocation() != null) {
            Map<String, Object> location = new HashMap<>();
            location.put("venueName", tournament.getLocation().getVenueName());
            location.put("address", tournament.getLocation().getAddress());
            location.put("city", tournament.getLocation().getCity());
            location.put("country", tournament.getLocation().getCountry());
            location.put("latitude", tournament.getLocation().getLatitude());
            location.put("longitude", tournament.getLocation().getLongitude());
            map.put("location", location);
        }

        return map;
    }

    /**
     * Build card response map with relevant fields
     */
    private Map<String, Object> buildCardResponse(CardTemplate card) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", card.getId());
        map.put("name", card.getName());
        map.put("tcgType", card.getTcgType());
        map.put("setCode", card.getSetCode());
        map.put("cardNumber", card.getCardNumber());
        map.put("rarity", card.getRarity());
        map.put("imageUrl", card.getImageUrl());
        map.put("description", card.getDescription());
        map.put("marketPrice", card.getMarketPrice());

        // Add expansion info if available
        if (card.getExpansion() != null) {
            Map<String, Object> expansion = new HashMap<>();
            expansion.put("id", card.getExpansion().getId());
            expansion.put("name", card.getExpansion().getTitle());
            expansion.put("imageUrl", card.getExpansion().getImageUrl());
            expansion.put("releaseDate", card.getExpansion().getReleaseDate());
            map.put("expansion", expansion);
        }

        return map;
    }
}
