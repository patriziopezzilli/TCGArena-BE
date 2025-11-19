package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.DeckService;
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
@RequestMapping("/api/decks")
@Tag(name = "Decks", description = "API for managing card decks in the TCG Arena system")
public class DeckController {

    @Autowired
    private DeckService deckService;

    @GetMapping
    @Operation(summary = "Get all decks", description = "Retrieves a list of all available decks")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of decks")
    })
    public List<Deck> getAllDecks() {
        return deckService.getAllDecks();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deck by ID", description = "Retrieves a specific deck by its unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck found and returned"),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Deck> getDeckById(@Parameter(description = "Unique identifier of the deck") @PathVariable Long id) {
        return deckService.getDeckById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Create a new deck", description = "Creates a new deck in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid deck data provided")
    })
    public Deck createDeck(@Parameter(description = "Deck object to be created") @RequestBody Deck deck) {
        return deckService.saveDeck(deck);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing deck", description = "Updates the details of an existing deck")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck updated successfully"),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Deck> updateDeck(@Parameter(description = "Unique identifier of the deck to update") @PathVariable Long id, @Parameter(description = "Updated deck object") @RequestBody Deck deck) {
        return deckService.updateDeck(id, deck)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deck", description = "Deletes a deck from the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Deck deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Void> deleteDeck(@Parameter(description = "Unique identifier of the deck to delete") @PathVariable Long id) {
        if (deckService.deleteDeck(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/add-card")
    @Operation(summary = "Add card to deck", description = "Adds a specified quantity of a card to an existing deck")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card added to deck successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or deck/card not found")
    })
    public ResponseEntity<Deck> addCardToDeck(@Parameter(description = "Unique identifier of the deck") @PathVariable Long id, @Parameter(description = "Unique identifier of the card") @RequestParam Long cardId, @Parameter(description = "Quantity of cards to add") @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(deckService.addCardToDeck(id, cardId, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/remove-card")
    @Operation(summary = "Remove card from deck", description = "Removes a card from an existing deck")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card removed from deck successfully"),
        @ApiResponse(responseCode = "404", description = "Deck or card not found")
    })
    public ResponseEntity<Void> removeCardFromDeck(@Parameter(description = "Unique identifier of the deck") @PathVariable Long id, @Parameter(description = "Unique identifier of the card") @RequestParam Long cardId) {
        if (deckService.removeCardFromDeck(id, cardId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/public")
    @Operation(summary = "Get public decks", description = "Retrieves a list of all public decks available for viewing")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of public decks")
    })
    public List<Deck> getPublicDecks() {
        return deckService.getPublicDecks();
    }
}