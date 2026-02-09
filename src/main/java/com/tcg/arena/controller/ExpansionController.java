package com.tcg.arena.controller;

import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.ExpansionService;
import com.tcg.arena.service.CardTemplateService;
import com.tcg.arena.dto.ExpansionDTO;
import com.tcg.arena.dto.TCGStatsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expansions")
public class ExpansionController {

    @Autowired
    private ExpansionService expansionService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @GetMapping
    public List<ExpansionDTO> getAllExpansions(
            @RequestParam(required = false) List<Integer> years,
            @RequestParam(required = false) String tcgType) {
        // OPTIMIZED: Load all counts in 2 batch queries instead of N queries per
        // expansion
        Map<String, Long> setCodeCounts = cardTemplateService.getAllCardCountsBySetCode();
        Map<Long, Long> expansionIdCounts = cardTemplateService.getAllCardCountsByExpansionId();

        List<Expansion> expansions;
        if (tcgType != null && !tcgType.isEmpty()) {
            // Filter by TCG type
            expansions = expansionService
                    .getExpansionsByTcgType(com.tcg.arena.model.TCGType.valueOf(tcgType.toUpperCase()));
        } else {
            // Use year-filtered query (defaults to current year if no years specified)
            expansions = expansionService.getExpansionsByYears(years);
        }

        return expansions.stream()
                .map(expansion -> new ExpansionDTO(expansion, setCodeCounts, expansionIdCounts))
                .filter(expansionDTO -> {
                    // Exclude expansions with 0 total cards
                    int totalCards = expansionDTO.getSets().stream()
                            .mapToInt(set -> set.getCardCount() != null ? set.getCardCount() : 0)
                            .sum();
                    return totalCards > 0;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/list")
    public org.springframework.data.domain.Page<ExpansionDTO> getExpansionsPaginated(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String tcgType,
            @RequestParam(required = false) List<Integer> years,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // Prepare Pageable
        // Sort by release date descending (newest first) by default
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size,
                org.springframework.data.domain.Sort.by("id").descending()); // Fallback sort, Service applies complex
                                                                             // date sort logic or SPEC does it?
        // Wait, Specification doesn't sort by computed dates.
        // Let's rely on default ID sort or we need to join sets to sort by release
        // date?
        // The current implementation sorts in memory.
        // For pagination, we should sort in DB.
        // Let's sort by ID DESC for now as a proxy for "recent added", or we can try to
        // sort by title.
        // NOTE: Sorting by "releaseDate" which is computed from sets is hard in DB
        // without a real column.
        // Assuming ID is roughly chronological for expansions.

        // Convert TCG Type
        com.tcg.arena.model.TCGType type = null;
        if (tcgType != null && !tcgType.isEmpty()) {
            try {
                type = com.tcg.arena.model.TCGType.valueOf(tcgType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // Ignore invalid type
            }
        }

        // Fetch Page
        org.springframework.data.domain.Page<Expansion> expansionPage = expansionService.getExpansionsPaginated(query,
                type, years, pageable);

        // Batch load counts for DTOs
        Map<String, Long> setCodeCounts = cardTemplateService.getAllCardCountsBySetCode();
        Map<Long, Long> expansionIdCounts = cardTemplateService.getAllCardCountsByExpansionId();

        // Convert to DTOs
        return expansionPage.map(expansion -> new ExpansionDTO(expansion, setCodeCounts, expansionIdCounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpansionDTO> getExpansionById(@PathVariable Long id) {
        // For single expansion, batch loading is still more efficient
        Map<String, Long> setCodeCounts = cardTemplateService.getAllCardCountsBySetCode();
        Map<Long, Long> expansionIdCounts = cardTemplateService.getAllCardCountsByExpansionId();

        return expansionService.getExpansionById(id)
                .map(expansion -> ResponseEntity.ok(new ExpansionDTO(expansion, setCodeCounts, expansionIdCounts)))
                .orElse(ResponseEntity.notFound().build());
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
        // OPTIMIZED: Load all counts in 2 batch queries
        Map<String, Long> setCodeCounts = cardTemplateService.getAllCardCountsBySetCode();
        Map<Long, Long> expansionIdCounts = cardTemplateService.getAllCardCountsByExpansionId();

        return expansionService.getRecentExpansions(limit).stream()
                .map(expansion -> new ExpansionDTO(expansion, setCodeCounts, expansionIdCounts))
                .filter(expansionDTO -> {
                    // Exclude expansions with 0 total cards
                    int totalCards = expansionDTO.getSets().stream()
                            .mapToInt(set -> set.getCardCount() != null ? set.getCardCount() : 0)
                            .sum();
                    return totalCards > 0;
                })
                .collect(Collectors.toList());
    }

    @GetMapping("/stats")
    public List<TCGStatsDTO> getTCGStatistics() {
        return expansionService.getTCGStatistics();
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
    public ResponseEntity<?> deleteExpansion(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            expansionService.deleteExpansion(id, force);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            String message = e.getMessage();
            // Check if confirmation is required
            if (message != null && message.startsWith("CONFIRM_REQUIRED:")) {
                String[] parts = message.split(":", 4);
                return ResponseEntity.status(409).body(java.util.Map.of(
                        "confirmRequired", true,
                        "setCount", Integer.parseInt(parts[1]),
                        "cardCount", Integer.parseInt(parts[2]),
                        "message", parts[3]));
            }
            return ResponseEntity.badRequest().body(java.util.Map.of("error", message));
        }
    }
}