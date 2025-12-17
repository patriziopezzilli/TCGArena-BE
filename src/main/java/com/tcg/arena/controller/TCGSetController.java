package com.tcg.arena.controller;

import com.tcg.arena.model.Card;
import com.tcg.arena.model.TCGSet;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.service.CardService;
import com.tcg.arena.service.TCGSetService;
import com.tcg.arena.service.CardTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/sets")
public class TCGSetController {

    @Autowired
    private TCGSetService tcgSetService;

    @Autowired
    private CardService cardService;

    @Autowired
    private CardTemplateService cardTemplateService;

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

    @GetMapping("/{id}/cards")
    public ResponseEntity<Page<CardTemplate>> getCardTemplatesBySetId(@PathVariable Long id,
            @PageableDefault(size = 10, sort = "dateCreated", direction = Sort.Direction.DESC) Pageable pageable) {
        return tcgSetService.getSetById(id)
                .map(set -> {
                    // First try to get cards by set code
                    Page<CardTemplate> cards = cardTemplateService.getCardTemplatesBySetCode(set.getSetCode(), pageable);
                    
                    // If no cards found by set code and set has an expansion, try by expansion ID
                    if (cards.isEmpty() && set.getExpansionId() != null) {
                        cards = cardTemplateService.getCardTemplatesByExpansionId(set.getExpansionId(), pageable);
                    }
                    
                    return ResponseEntity.ok(cards);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{setCode}/cards")
    public List<Card> getCardsBySetCode(@PathVariable String setCode) {
        return cardService.getAllCards().stream()
                .filter(card -> setCode.equals(card.getCardTemplate().getSetCode()))
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
    public ResponseEntity<?> deleteSet(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            tcgSetService.deleteSet(id, force);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String message = e.getMessage();
            // Check if confirmation is required
            if (message != null && message.startsWith("CONFIRM_REQUIRED:")) {
                String[] parts = message.split(":", 3);
                return ResponseEntity.status(409).body(java.util.Map.of(
                        "confirmRequired", true,
                        "cardCount", Integer.parseInt(parts[1]),
                        "message", parts[2]));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", message));
        }
    }
}