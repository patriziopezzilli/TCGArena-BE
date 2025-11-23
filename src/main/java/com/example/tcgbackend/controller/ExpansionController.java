package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.service.ExpansionService;
import com.example.tcgbackend.service.CardTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expansions")
public class ExpansionController {

    @Autowired
    private ExpansionService expansionService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @GetMapping
    public List<Expansion> getAllExpansions() {
        return expansionService.getAllExpansions();
    }

    @GetMapping("/{id}/cards")
    public ResponseEntity<List<CardTemplate>> getCardsForExpansion(@PathVariable Long id) {
        if (!expansionService.getExpansionById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<CardTemplate> cards = cardTemplateService.getCardTemplatesByExpansion(id);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/recent")
    public List<Expansion> getRecentExpansions() {
        return expansionService.getRecentExpansions();
    }

    @PostMapping
    public Expansion createExpansion(@RequestBody Expansion expansion) {
        return expansionService.saveExpansion(expansion);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Expansion> updateExpansion(@PathVariable Long id, @RequestBody Expansion expansion) {
        return expansionService.updateExpansion(id, expansion)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpansion(@PathVariable Long id) {
        if (expansionService.deleteExpansion(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}