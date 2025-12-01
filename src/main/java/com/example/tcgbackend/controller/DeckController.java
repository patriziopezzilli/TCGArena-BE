package com.example.tcgbackend.controller;

import com.example.tcgbackend.dto.DeckCardUpdateDTO;
import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.DeckType;
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
    @Operation(summary = "Get user's decks", description = "Retrieves all decks owned by the specified user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of user's decks")
    })
    public List<Deck> getAllDecks(@Parameter(description = "User ID to filter decks") @RequestParam Long userId) {
        return deckService.getDecksByOwnerId(userId);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deck by ID", description = "Retrieves a specific deck by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deck found and returned"),
            @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Deck> getDeckById(
            @Parameter(description = "Unique identifier of the deck") @PathVariable Long id) {
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
    public ResponseEntity<Deck> updateDeck(
            @Parameter(description = "Unique identifier of the deck to update") @PathVariable Long id,
            @Parameter(description = "Updated deck object") @RequestBody Deck deck) {
        return deckService.updateDeck(id, deck)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a deck", description = "Deletes an existing deck from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Deck deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Void> deleteDeck(
            @Parameter(description = "Unique identifier of the deck to delete") @PathVariable Long id,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        if (deckService.deleteDeck(id, userId)) {
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
    public ResponseEntity<Deck> addCardToDeck(
            @Parameter(description = "Unique identifier of the deck") @PathVariable Long id,
            @Parameter(description = "Unique identifier of the card") @RequestParam Long cardId,
            @Parameter(description = "Quantity of cards to add") @RequestParam int quantity,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        try {
            return ResponseEntity.ok(deckService.addCardToDeck(id, cardId, quantity, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/add-card-template")
    @Operation(summary = "Add card template to deck", description = "Adds a card to deck from a card template (used in discover new card flow)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card template added to deck successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request or deck/template not found"),
            @ApiResponse(responseCode = "404", description = "Deck or card template not found")
    })
    public ResponseEntity<Deck> addCardTemplateToDeck(
            @Parameter(description = "Unique identifier of the deck") @PathVariable Long id,
            @Parameter(description = "Unique identifier of the card template") @RequestParam Long templateId,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        try {
            return ResponseEntity.ok(deckService.addCardTemplateToDeck(id, templateId, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/remove-card")
    @Operation(summary = "Remove card from deck", description = "Removes a card from an existing deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card removed from deck successfully"),
            @ApiResponse(responseCode = "404", description = "Deck or card not found")
    })
    public ResponseEntity<Void> removeCardFromDeck(
            @Parameter(description = "Unique identifier of the deck") @PathVariable Long id,
            @Parameter(description = "Unique identifier of the card") @RequestParam Long cardId,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        if (deckService.removeCardFromDeck(id, cardId, userId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}/cards/{deckCardId}/condition")
    @Operation(summary = "Update card condition in deck", description = "Updates the condition of a specific card in an existing deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card condition updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid condition value"),
            @ApiResponse(responseCode = "403", description = "User not authorized to update this deck"),
            @ApiResponse(responseCode = "404", description = "Deck or card not found")
    })
    public ResponseEntity<Void> updateDeckCardCondition(
            @Parameter(description = "Unique identifier of the deck") @PathVariable Long id,
            @Parameter(description = "Unique identifier of the deck card") @PathVariable Long deckCardId,
            @Parameter(description = "New condition for the card") @RequestParam String condition,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        try {
            CardCondition cardCondition = CardCondition.valueOf(condition.toUpperCase());
            if (deckService.updateDeckCardCondition(id, deckCardId, cardCondition, userId)) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/cards/{cardId}/condition")
    @Operation(summary = "Update card condition by card ID", description = "Updates the condition of a card using only the card ID (direct endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card condition updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid condition value"),
            @ApiResponse(responseCode = "403", description = "User not authorized to update this card"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> updateDeckCardConditionByCardId(
            @Parameter(description = "Unique identifier of the deck card") @PathVariable Long cardId,
            @Parameter(description = "New condition for the card") @RequestParam String condition,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        try {
            CardCondition cardCondition = CardCondition.valueOf(condition.toUpperCase());
            if (deckService.updateDeckCardConditionByCardId(cardId, cardCondition, userId)) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/cards/{cardId}")
    @Operation(summary = "Update deck card by card ID", description = "Updates a deck card using only the card ID (direct endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid update data"),
            @ApiResponse(responseCode = "403", description = "User not authorized to update this card"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> updateDeckCardByCardId(
            @Parameter(description = "Unique identifier of the deck card") @PathVariable Long cardId,
            @Parameter(description = "Update data for the card") @RequestBody DeckCardUpdateDTO updateDTO,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        try {
            if (deckService.updateDeckCardByCardId(cardId, updateDTO, userId)) {
                return ResponseEntity.ok().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/cards/{cardId}")
    @Operation(summary = "Remove card by card ID", description = "Removes a card from its deck using only the card ID (direct endpoint)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card removed successfully"),
            @ApiResponse(responseCode = "403", description = "User not authorized to remove this card"),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<Void> removeDeckCardByCardId(
            @Parameter(description = "Unique identifier of the deck card") @PathVariable Long cardId,
            @Parameter(description = "Unique identifier of the user performing the action") @RequestParam Long userId) {
        if (deckService.removeDeckCardByCardId(cardId, userId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/collection")
    @Operation(summary = "Get user's collection deck", description = "Retrieves the user's collection deck (LISTA type deck containing all owned cards)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Collection deck found and returned"),
            @ApiResponse(responseCode = "404", description = "Collection deck not found")
    })
    public ResponseEntity<Deck> getCollectionDeck(
            @Parameter(description = "User ID to get collection for") @RequestParam Long userId) {
        return deckService.getCollectionDeckByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/public")

    @PostMapping("/create")
    @Operation(summary = "Create a new deck with parameters", description = "Creates a new deck with specified name, TCG type, and deck type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deck created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters provided")
    })
    public ResponseEntity<Deck> createDeckWithParams(
            @Parameter(description = "Name of the deck") @RequestParam String name,
            @Parameter(description = "TCG type of the deck") @RequestParam TCGType tcgType,
            @Parameter(description = "Type of the deck (DECK or LISTA)") @RequestParam DeckType deckType,
            @Parameter(description = "Unique identifier of the user creating the deck") @RequestParam Long userId) {
        try {
            Deck deck = deckService.createDeck(name, tcgType, deckType, userId);
            return ResponseEntity.ok(deck);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}