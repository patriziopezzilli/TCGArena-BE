package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.service.CardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    @Autowired
    private CardService cardService;

    @GetMapping
    public List<Card> getAllCards() {
        return cardService.getAllCards();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Card> getCardById(@PathVariable Long id) {
        return cardService.getCardById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Card createCard(@RequestBody Card card) {
        return cardService.saveCard(card);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Card> updateCard(@PathVariable Long id, @RequestBody Card card) {
        return cardService.updateCard(id, card)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long id) {
        if (cardService.deleteCard(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/collection/{userId}")
    public List<Card> getUserCollection(@PathVariable Long userId) {
        return cardService.getUserCollection(userId);
    }

    @PostMapping("/{id}/add-to-collection")
    public ResponseEntity<Card> addToCollection(@PathVariable Long id, @RequestParam Long userId) {
        try {
            Card card = cardService.getCardById(id).orElseThrow();
            return ResponseEntity.ok(cardService.addCardToCollection(card, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/market-price/{id}")
    public ResponseEntity<Double> getMarketPrice(@PathVariable Long id) {
        return cardService.getCardById(id)
            .map(card -> ResponseEntity.ok(card.getMarketPrice()))
            .orElse(ResponseEntity.notFound().build());
    }
}