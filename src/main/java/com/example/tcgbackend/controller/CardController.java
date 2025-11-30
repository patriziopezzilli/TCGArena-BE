package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.service.CardTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards/templates")
@Tag(name = "Card Templates", description = "API for managing card templates in the TCG Arena system")
public class CardController {

    @Autowired
    private CardTemplateService cardTemplateService;

    @GetMapping
    @Operation(summary = "Get all card templates", description = "Retrieves a paginated list of all available card templates in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved paginated list of card templates")
    })
    public Page<CardTemplate> getAllCards(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of items per page") @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return cardTemplateService.getAllCardTemplates(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card template by ID", description = "Retrieves a specific card template by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card template found and returned"),
            @ApiResponse(responseCode = "404", description = "Card template not found")
    })
    public ResponseEntity<CardTemplate> getCardById(
            @Parameter(description = "Unique identifier of the card template") @PathVariable Long id) {
        return cardTemplateService.getCardTemplateById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new card template", description = "Creates a new card template in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card template created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid card template data provided")
    })
    public CardTemplate createCard(
            @Parameter(description = "Card template object to be created") @RequestBody CardTemplate cardTemplate) {
        return cardTemplateService.saveCardTemplate(cardTemplate);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing card template", description = "Updates the details of an existing card template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card template updated successfully"),
            @ApiResponse(responseCode = "404", description = "Card template not found")
    })
    public ResponseEntity<CardTemplate> updateCard(
            @Parameter(description = "Unique identifier of the card template to update") @PathVariable Long id,
            @Parameter(description = "Updated card template object") @RequestBody CardTemplate cardTemplate) {
        return cardTemplateService.updateCardTemplate(id, cardTemplate)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a card template", description = "Deletes a card template from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card template deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Card template not found")
    })
    public ResponseEntity<Void> deleteCard(
            @Parameter(description = "Unique identifier of the card template to delete") @PathVariable Long id) {
        if (cardTemplateService.deleteCardTemplate(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/search")
    @Operation(summary = "Search card templates", description = "Search for card templates by name or set code (minimum 2 characters required)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully, returns matching card templates"),
            @ApiResponse(responseCode = "400", description = "Query string is too short (minimum 2 characters required)")
    })
    public ResponseEntity<?> searchCards(
            @Parameter(description = "Search query (minimum 2 characters)") @RequestParam(name = "q") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().body("Search query must be at least 2 characters long");
        }
        List<CardTemplate> results = cardTemplateService.searchCardTemplates(query.trim());
        return ResponseEntity.ok(results);
    }

    @GetMapping("/market-price/{id}")
    @Operation(summary = "Get market price of a card template", description = "Retrieves the current market price of a specific card template")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Market price retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Card template not found")
    })
    public ResponseEntity<Double> getMarketPrice(
            @Parameter(description = "Unique identifier of the card template") @PathVariable Long id) {
        return cardTemplateService.getCardTemplateById(id)
                .map(card -> ResponseEntity.ok(card.getMarketPrice()))
                .orElse(ResponseEntity.notFound().build());
    }
}