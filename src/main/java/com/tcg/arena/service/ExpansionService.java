package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGSet;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.dto.TCGStatsDTO;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.ExpansionRepository;
import com.tcg.arena.repository.TCGSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class ExpansionService {

    @Autowired
    private ExpansionRepository expansionRepository;

    // Removed cache to avoid LazyInitializationException with detached entities
    public List<Expansion> getAllExpansions() {
        return expansionRepository.findAllWithSets().stream()
                .sorted((e1, e2) -> e2.getReleaseDate().compareTo(e1.getReleaseDate()))
                .collect(Collectors.toList());
    }

    /**
     * Get expansions filtered by year(s)
     * If no years specified, defaults to current year
     * 
     * @param years List of years to filter by (e.g., [2025, 2024])
     * @return Filtered and sorted expansions
     */
    public List<Expansion> getExpansionsByYears(List<Integer> years) {
        List<Expansion> expansions;

        if (years == null || years.isEmpty()) {
            // Default: current year only
            int currentYear = java.time.Year.now().getValue();
            expansions = expansionRepository.findAllWithSetsByYear(currentYear);
        } else if (years.size() == 1) {
            expansions = expansionRepository.findAllWithSetsByYear(years.get(0));
        } else {
            expansions = expansionRepository.findAllWithSetsByYears(years);
        }

        return expansions.stream()
                .sorted((e1, e2) -> e2.getReleaseDate().compareTo(e1.getReleaseDate()))
                .collect(Collectors.toList());
    }

    public Optional<Expansion> getExpansionById(Long id) {
        return expansionRepository.findById(id);
    }

    @Cacheable(value = CacheConfig.EXPANSIONS_CACHE, key = "'tcgType_' + #tcgType.name()")
    public List<Expansion> getExpansionsByTcgType(TCGType tcgType) {
        return expansionRepository.findByTcgType(tcgType).stream()
                .sorted((e1, e2) -> e2.getReleaseDate().compareTo(e1.getReleaseDate()))
                .collect(Collectors.toList());
    }

    public List<Expansion> getRecentExpansions() {
        // Implement logic to get recent expansions
        return getRecentExpansions(5);
    }

    // Removed cache to avoid LazyInitializationException
    public List<Expansion> getRecentExpansions(int limit) {
        return expansionRepository.findAllWithSets().stream()
                .sorted((e1, e2) -> e2.getReleaseDate().compareTo(e1.getReleaseDate()))
                .limit(limit).collect(Collectors.toList());
    }

    public List<TCGSet> getSetsByExpansionId(Long id) {
        return expansionRepository.findByIdWithSets(id).map(Expansion::getSets).orElse(List.of());
    }

    public Expansion saveExpansion(Expansion expansion) {
        return expansionRepository.save(expansion);
    }

    /**
     * Search expansions by title (case-insensitive)
     * 
     * @param query Search query string
     * @return List of matching expansions, ordered by most recent first
     */
    public List<Expansion> searchExpansions(String query) {
        if (query == null || query.trim().length() < 2) {
            return List.of();
        }
        return expansionRepository.searchByTitle(query.trim());
    }

    public Optional<Expansion> updateExpansion(Long id, Expansion expansionDetails) {
        return expansionRepository.findById(id).map(expansion -> {
            expansion.setTitle(expansionDetails.getTitle());
            expansion.setTcgType(expansionDetails.getTcgType());
            expansion.setImageUrl(expansionDetails.getImageUrl());
            return expansionRepository.save(expansion);
        });
    }

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private TCGSetRepository tcgSetRepository;

    /**
     * Get deletion info for an expansion (how many sets and cards would be deleted)
     */
    public Map<String, Object> getExpansionDeletionInfo(Long id) {
        Expansion expansion = expansionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espansione non trovata"));

        List<CardTemplate> associatedCards = cardTemplateRepository.findByExpansionId(id);
        int setCount = expansion.getSets() != null ? expansion.getSets().size() : 0;

        Map<String, Object> info = new HashMap<>();
        info.put("expansionId", id);
        info.put("expansionTitle", expansion.getTitle());
        info.put("setCount", setCount);
        info.put("cardCount", associatedCards.size());
        info.put("hasAssociatedData", setCount > 0 || !associatedCards.isEmpty());

        return info;
    }

    /**
     * Delete expansion with optional cascade
     * 
     * @param id    Expansion ID
     * @param force If true, delete all associated sets and cards
     */
    @Transactional
    public void deleteExpansion(Long id, boolean force) {
        Expansion expansion = expansionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Espansione non trovata"));

        List<CardTemplate> associatedCards = cardTemplateRepository.findByExpansionId(id);
        int setCount = expansion.getSets() != null ? expansion.getSets().size() : 0;

        if (!force && (setCount > 0 || !associatedCards.isEmpty())) {
            throw new RuntimeException(
                    "CONFIRM_REQUIRED:" + setCount + ":" + associatedCards.size() + ":" +
                            "L'espansione '" + expansion.getTitle() + "' contiene " + setCount + " set e " +
                            associatedCards.size() + " carte. Vuoi eliminarli tutti?");
        }

        if (force) {
            // Delete all associated card templates first
            if (!associatedCards.isEmpty()) {
                cardTemplateRepository.deleteAll(associatedCards);
            }

            // Delete all associated sets
            if (expansion.getSets() != null && !expansion.getSets().isEmpty()) {
                tcgSetRepository.deleteAll(expansion.getSets());
            }
        }

        expansionRepository.deleteById(id);
    }

    // Removed cache to avoid LazyInitializationException
    public List<TCGStatsDTO> getTCGStatistics() {
        List<Expansion> allExpansions = expansionRepository.findAllWithSets();

        Map<TCGType, TCGStatsDTO> statsMap = new HashMap<>();

        // Initialize stats for all TCG types
        for (TCGType tcgType : TCGType.values()) {
            statsMap.put(tcgType, new TCGStatsDTO(tcgType, 0, 0, 0));
        }

        // Calculate statistics
        for (Expansion expansion : allExpansions) {
            TCGStatsDTO stats = statsMap.get(expansion.getTcgType());
            if (stats != null) {
                stats.setExpansions(stats.getExpansions() + 1);
                stats.setSets(stats.getSets() + expansion.getSets().size());
                stats.setCards(stats.getCards() + expansion.getSets().stream()
                        .mapToInt(set -> set.getCardCount() != null ? set.getCardCount() : 0)
                        .sum());
            }
        }

        return statsMap.values().stream().collect(Collectors.toList());
    }
}