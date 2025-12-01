package com.tcg.arena.controller;

import com.tcg.arena.model.ProDeck;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.ProDeckService;
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

    @GetMapping("/recent")
    public List<ProDeck> getRecentProDecks() {
        return proDeckService.getRecentProDecks();
    }
}