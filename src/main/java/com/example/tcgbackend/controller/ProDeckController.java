package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.ProDeck;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.service.ProDeckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pro-decks")
public class ProDeckController {

    @Autowired
    private ProDeckService proDeckService;

    @GetMapping
    public List<ProDeck> getAllProDecks() {
        return proDeckService.getAllProDecks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProDeck> getProDeckById(@PathVariable Long id) {
        return proDeckService.getProDeckById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/tcg-type/{tcgType}")
    public List<ProDeck> getProDecksByTcgType(@PathVariable TCGType tcgType) {
        return proDeckService.getProDecksByTcgType(tcgType);
    }

    @GetMapping("/author/{author}")
    public List<ProDeck> getProDecksByAuthor(@PathVariable String author) {
        return proDeckService.getProDecksByAuthor(author);
    }

    @GetMapping("/tournament/{tournament}")
    public List<ProDeck> getProDecksByTournament(@PathVariable String tournament) {
        return proDeckService.getProDecksByTournament(tournament);
    }

    @GetMapping("/recent")
    public List<ProDeck> getRecentProDecks() {
        return proDeckService.getRecentProDecks();
    }

    @PostMapping
    public ProDeck createProDeck(@RequestBody ProDeck proDeck) {
        return proDeckService.saveProDeck(proDeck);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProDeck> updateProDeck(@PathVariable Long id, @RequestBody ProDeck proDeck) {
        return proDeckService.getProDeckById(id)
            .map(existingDeck -> {
                proDeck.setId(id);
                return ResponseEntity.ok(proDeckService.saveProDeck(proDeck));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProDeck(@PathVariable Long id) {
        if (proDeckService.deleteProDeck(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}