package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.DeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
public class DeckController {

    @Autowired
    private DeckService deckService;

    @GetMapping
    public List<Deck> getAllDecks() {
        return deckService.getAllDecks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Deck> getDeckById(@PathVariable Long id) {
        return deckService.getDeckById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Deck createDeck(@RequestBody Deck deck) {
        return deckService.saveDeck(deck);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Deck> updateDeck(@PathVariable Long id, @RequestBody Deck deck) {
        return deckService.updateDeck(id, deck)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(@PathVariable Long id) {
        if (deckService.deleteDeck(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/add-card")
    public ResponseEntity<Deck> addCardToDeck(@PathVariable Long id, @RequestParam Long cardId, @RequestParam int quantity) {
        try {
            return ResponseEntity.ok(deckService.addCardToDeck(id, cardId, quantity));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/remove-card")
    public ResponseEntity<Void> removeCardFromDeck(@PathVariable Long id, @RequestParam Long cardId) {
        if (deckService.removeCardFromDeck(id, cardId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/public")
    public List<Deck> getPublicDecks() {
        return deckService.getPublicDecks();
    }
}