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
import org.springframework.cache.annotation.CacheEvict;
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
            // Default: ALL expansions (requested by user)
            expansions = expansionRepository.findAllWithSets();
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

    @CacheEvict(value = CacheConfig.EXPANSIONS_CACHE, allEntries = true)
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

    @CacheEvict(value = CacheConfig.EXPANSIONS_CACHE, allEntries = true)
    public Optional<Expansion> updateExpansion(Long id, Expansion expansionDetails) {
        return expansionRepository.findById(id).map(expansion -> {
            expansion.setTitle(expansionDetails.getTitle());
            expansion.setTcgType(expansionDetails.getTcgType());
            expansion.setImageUrl(expansionDetails.getImageUrl());
            // Mark as manually modified to preserve during import
            expansion.setModifiedManually(true);
            return expansionRepository.save(expansion);
        });
    }

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private TCGSetRepository tcgSetRepository;

    @Autowired
    private com.tcg.arena.repository.TradeListEntryRepository tradeListEntryRepository;

    @Autowired
    private com.tcg.arena.repository.CardVoteRepository cardVoteRepository;

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
    @CacheEvict(value = { CacheConfig.EXPANSIONS_CACHE, CacheConfig.CARD_TEMPLATES_CACHE,
            CacheConfig.SETS_CACHE }, allEntries = true)
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
            // Delete associated data first to prevent foreign key violations
            tradeListEntryRepository.deleteByCardTemplateExpansionId(id);
            cardVoteRepository.deleteByCardTemplateExpansionId(id);

            // Delete all associated card templates first
            cardTemplateRepository.deleteByExpansionId(id);

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

        // Only include TCG types that are fully supported in mobile apps
        List<TCGType> supportedTypes = List.of(
                TCGType.POKEMON,
                TCGType.ONE_PIECE,
                TCGType.MAGIC,
                TCGType.YUGIOH,
                TCGType.DIGIMON,
                TCGType.LORCANA,
                TCGType.RIFTBOUND,
                TCGType.UNION_ARENA,
                TCGType.DRAGON_BALL_SUPER_FUSION_WORLD,
                TCGType.POKEMON_JAPAN,
                TCGType.FLESH_AND_BLOOD);

        Map<TCGType, TCGStatsDTO> statsMap = new HashMap<>();

        // Initialize stats only for supported TCG types
        for (TCGType tcgType : supportedTypes) {
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

    /**
     * Get expansions with pagination and dynamic filtering
     * 
     * @param query    Search query (optional)
     * @param tcgType  Filter by TCG Type (optional)
     * @param years    Filter by release years (optional)
     * @param pageable Pagination info
     * @return Page of expansions
     */
    public org.springframework.data.domain.Page<Expansion> getExpansionsPaginated(
            String query,
            TCGType tcgType,
            List<Integer> years,
            org.springframework.data.domain.Pageable pageable) {

        org.springframework.data.jpa.domain.Specification<Expansion> spec = (root, querySpec, cb) -> {
            java.util.List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();

            // Filter by TCG Type
            if (tcgType != null) {
                predicates.add(cb.equal(root.get("tcgType"), tcgType));
            }

            // Filter by Years (joins with Sets)
            if (years != null && !years.isEmpty()) {
                jakarta.persistence.criteria.Join<Expansion, TCGSet> setsJoin = root.join("sets",
                        jakarta.persistence.criteria.JoinType.LEFT);

                // PostgreSQL 'date_part' returns Double/Float
                jakarta.persistence.criteria.Expression<Double> yearExpr = cb.function("date_part", Double.class,
                        cb.literal("year"), setsJoin.get("releaseDate"));

                // Convert input integers to doubles for comparison
                java.util.List<Double> doubleYears = years.stream()
                        .map(Integer::doubleValue)
                        .collect(java.util.stream.Collectors.toList());

                predicates.add(yearExpr.in(doubleYears));
                querySpec.distinct(true);
            }

            // Filter by Search Query
            if (query != null && !query.trim().isEmpty()) {
                String searchLike = "%" + query.trim().toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("title")), searchLike));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return expansionRepository.findAll(spec, pageable);
    }

    /**
     * Get distinct release years for expansions of a specific TCG type
     * based on the sets within those expansions.
     */
    public List<Integer> getExpansionYears(TCGType tcgType) {
        return tcgSetRepository.findDistinctReleaseYearsByTcgType(tcgType);
    }
}