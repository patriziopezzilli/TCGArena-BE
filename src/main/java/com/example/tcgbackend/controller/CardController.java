package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "Cards", description = "API for managing trading cards in the TCG Arena system")
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    @Operation(summary = "Get all cards", description = "Retrieves a list of all available trading cards in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of cards")
    })
    public List<Card> getAllCards() {
        return cardService.getAllCards();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID", description = "Retrieves a specific trading card by its unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found and returned"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Card> getCardById(@Parameter(description = "Unique identifier of the card") @PathVariable Long id) {
        return cardService.getCardById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new card", description = "Creates a new trading card in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid card data provided")
    })
    public Card createCard(@Parameter(description = "Card object to be created") @RequestBody Card card) {
        return cardService.saveCard(card);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing card", description = "Updates the details of an existing trading card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card updated successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Card> updateCard(@Parameter(description = "Unique identifier of the card to update") @PathVariable Long id, @Parameter(description = "Updated card object") @RequestBody Card card) {
        return cardService.updateCard(id, card)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a card", description = "Deletes a trading card from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> deleteCard(@Parameter(description = "Unique identifier of the card to delete") @PathVariable Long id) {
        if (cardService.deleteCard(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/collection/{userId}")
    @Operation(summary = "Get user's card collection", description = "Retrieves all cards owned by a specific user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's card collection")
    })
    public List<Card> getUserCollection(@Parameter(description = "Unique identifier of the user") @PathVariable Long userId) {
        return cardService.getUserCollection(userId);
    }

    @PostMapping("/{id}/add-to-collection")
    @Operation(summary = "Add card to user's collection", description = "Adds a specific card to a user's collection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card added to collection successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or card not found"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Card> addToCollection(@Parameter(description = "Unique identifier of the card") @PathVariable Long id, @Parameter(description = "Unique identifier of the user") @RequestParam Long userId) {
        try {
            Card card = cardService.getCardById(id).orElseThrow();
            return ResponseEntity.ok(cardService.addCardToCollection(card, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/market-price/{id}")
    @Operation(summary = "Get market price of a card", description = "Retrieves the current market price of a specific trading card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Market price retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Double> getMarketPrice(@Parameter(description = "Unique identifier of the card") @PathVariable Long id) {
        return cardService.getCardById(id)
            .map(card -> ResponseEntity.ok(card.getMarketPrice()))
            .orElse(ResponseEntity.notFound().build());
    }
}