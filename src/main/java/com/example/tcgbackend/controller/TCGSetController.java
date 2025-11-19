package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.model.TCGSet;
import com.example.tcgbackend.service.CardService;
import com.example.tcgbackend.service.TCGSetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sets")
public class TCGSetController {

    @Autowired
    private TCGSetService tcgSetService;

    @Autowired
    private CardService cardService;

    @GetMapping
    public List<TCGSet> getAllSets() {
        return tcgSetService.getAllSets();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TCGSet> getSetById(@PathVariable Long id) {
        return tcgSetService.getSetById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{setCode}/cards")
    public List<Card> getCardsBySetCode(@PathVariable String setCode) {
        return cardService.getAllCards().stream()
            .filter(card -> setCode.equals(card.getSetCode()))
            .toList();
    }

    @PostMapping
    public TCGSet createSet(@RequestBody TCGSet set) {
        return tcgSetService.saveSet(set);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TCGSet> updateSet(@PathVariable Long id, @RequestBody TCGSet set) {
        return tcgSetService.updateSet(id, set)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSet(@PathVariable Long id) {
        if (tcgSetService.deleteSet(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}