package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGSet;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.service.ExpansionService;
import com.example.tcgbackend.service.CardTemplateService;
import com.example.tcgbackend.dto.ExpansionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expansions")
public class ExpansionController {

    @Autowired
    private ExpansionService expansionService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @GetMapping
    public List<ExpansionDTO> getAllExpansions() {
        return expansionService.getAllExpansions().stream()
            .map(ExpansionDTO::new)
            .collect(Collectors.toList());
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
    public List<ExpansionDTO> getRecentExpansions(@RequestParam(defaultValue = "5") int limit) {
        return expansionService.getRecentExpansions(limit).stream()
            .map(ExpansionDTO::new)
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}/sets")
    public ResponseEntity<List<TCGSet>> getSetsForExpansion(@PathVariable Long id) {
        if (!expansionService.getExpansionById(id).isPresent()) {
            return ResponseEntity.notFound().build();
        }
        List<TCGSet> sets = expansionService.getSetsByExpansionId(id);
        return ResponseEntity.ok(sets);
    }

    @PostMapping
    public ExpansionDTO createExpansion(@RequestBody Expansion expansion) {
        Expansion saved = expansionService.saveExpansion(expansion);
        return new ExpansionDTO(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpansionDTO> updateExpansion(@PathVariable Long id, @RequestBody Expansion expansion) {
        return expansionService.updateExpansion(id, expansion)
            .map(e -> ResponseEntity.ok(new ExpansionDTO(e)))
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