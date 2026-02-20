package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CardTemplateService {
    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    public Page<CardTemplate> getAllCardTemplates(Pageable pageable) {
        return cardTemplateRepository.findAll(pageable);
    }

    @Deprecated
    public List<CardTemplate> getAllCardTemplates() {
        return cardTemplateRepository.findAll();
    }

    @Cacheable(value = CacheConfig.CARD_TEMPLATE_BY_ID_CACHE, key = "#id")
    public Optional<CardTemplate> getCardTemplateById(Long id) {
        return cardTemplateRepository.findById(id);
    }

    @Cacheable(value = CacheConfig.CARD_TEMPLATES_CACHE, key = "'tcgType_' + #tcgType")
    public List<CardTemplate> getCardTemplatesByTcgType(String tcgType) {
        return cardTemplateRepository.findByTcgType(tcgType);
    }

    @Cacheable(value = CacheConfig.EXPANSION_CARDS_CACHE, key = "'expansion_' + #expansionId")
    public List<CardTemplate> getCardTemplatesByExpansion(Long expansionId) {
        return cardTemplateRepository.findByExpansionId(expansionId);
    }

    @Cacheable(value = CacheConfig.EXPANSION_CARDS_CACHE, key = "'expansion_' + #expansionId + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<CardTemplate> getCardTemplatesByExpansionId(Long expansionId, Pageable pageable) {
        return cardTemplateRepository.findAllByExpansionId(expansionId, pageable);
    }

    @Cacheable(value = CacheConfig.SET_CARDS_CACHE, key = "'setCode_' + #setCode + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<CardTemplate> getCardTemplatesBySetCode(String setCode, Pageable pageable) {
        return cardTemplateRepository.findAllBySetCode(setCode, pageable);
    }

    public long countCardsBySetCode(String setCode) {
        return cardTemplateRepository.countAllBySetCode(setCode);
    }

    public long countCardsByExpansionId(Long expansionId) {
        return cardTemplateRepository.countAllByExpansionId(expansionId);
    }

    public List<CardTemplate> getCardTemplatesByRarity(String rarity) {
        return cardTemplateRepository.findByRarity(rarity);
    }

    public List<CardTemplate> searchCardTemplates(String query) {
        return searchCardTemplatesPaginated(query, PageRequest.of(0, 100)).getContent();
    }

    public Page<CardTemplate> searchCardTemplatesPaginated(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty();
        }

        String strippedQuery = stripQuery(query);

        // Smart search: Check for "Name Number" pattern
        String[] parts = query.trim().split("\\s+");
        if (parts.length >= 2) {
            // Try to treat the last part as a card number
            String potentialNumber = parts[parts.length - 1];
            // Reconstruct the name part (everything except last token)
            String potentialName = query.substring(0, query.lastIndexOf(potentialNumber)).trim();

            if (!potentialName.isEmpty()) {
                String strippedPotentialName = stripQuery(potentialName);
                Page<CardTemplate> smartResults = cardTemplateRepository.searchByNameAndCardNumber(
                        strippedPotentialName,
                        potentialNumber, pageable);
                if (!smartResults.isEmpty()) {
                    return smartResults;
                }
            }
        }

        // Fallback to standard search
        return cardTemplateRepository.searchByNameOrSetCode(query, strippedQuery, pageable);
    }

    /**
     * Helper to strip special characters for fuzzy matching
     */
    private String stripQuery(String query) {
        if (query == null)
            return "";
        return query.replaceAll("[\\s\\-'/.]", "");
    }

    public List<CardTemplate> smartScan(List<String> rawTexts, String tcgType) {
        if (rawTexts == null || rawTexts.isEmpty()) {
            System.out.println("DEBUG: SmartScan - Input is empty");
            return List.of();
        }

        System.out.println("DEBUG: SmartScan - Raw Texts: " + rawTexts);
        System.out.println("DEBUG: SmartScan - TCG Type Filter: " + tcgType);

        // 1. Flatten, Tokenize and Filter
        // Filter: Keep only alphabetic strings with length >= 4
        List<String> tokens = rawTexts.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+")))
                .map(String::trim)
                .filter(s -> s.matches("[a-zA-Z]{4,}"))
                .distinct()
                .collect(java.util.stream.Collectors.toList());

        System.out.println("DEBUG: SmartScan - Filtered Alphabetic Tokens: " + tokens);

        if (tokens.isEmpty()) {
            return List.of();
        }

        // 2. Query DB for each token and collect unique results
        java.util.Set<CardTemplate> resultSet = new java.util.HashSet<>();

        for (String token : tokens) {
            // Simple LIKE %token% search
            List<CardTemplate> matches = cardTemplateRepository.findByNameContainingIgnoreCase(token);

            // Filter by TCG Type if provided
            if (tcgType != null && !tcgType.isBlank()) {
                matches = matches.stream()
                        .filter(card -> card.getTcgType() != null && card.getTcgType().name().equalsIgnoreCase(tcgType))
                        .collect(java.util.stream.Collectors.toList());
            }

            resultSet.addAll(matches);

            // Safety break if too many results?
            // For now, respect user request "simple like"
            if (resultSet.size() > 100) {
                System.out.println("DEBUG: SmartScan - Hit result limit (100) with token: " + token);
                break;
            }
        }

        List<CardTemplate> results = new java.util.ArrayList<>(resultSet);
        System.out.println("DEBUG: SmartScan - Found " + results.size() + " unique matches");
        return results;
    }

    @Cacheable(value = CacheConfig.CARD_SEARCH_CACHE, key = "'filters_smart_' + #tcgType + '_' + #expansionId + '_' + #setCode + '_' + #rarity + '_' + #searchQuery + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<CardTemplate> searchCardTemplatesWithFilters(
            String tcgType,
            Long expansionId,
            String setCode,
            String rarity,
            String searchQuery,
            Pageable pageable) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return cardTemplateRepository.findWithFilters(tcgType, expansionId, setCode, rarity, null, null, pageable);
        }

        String strippedQuery = stripQuery(searchQuery);

        return cardTemplateRepository.findWithFilters(tcgType, expansionId, setCode, rarity, searchQuery, strippedQuery,
                pageable);
    }

    @CacheEvict(value = { CacheConfig.CARD_TEMPLATES_CACHE, CacheConfig.CARD_TEMPLATE_BY_ID_CACHE,
            CacheConfig.CARD_SEARCH_CACHE, CacheConfig.EXPANSION_CARDS_CACHE,
            CacheConfig.SET_CARDS_CACHE }, allEntries = true)
    public CardTemplate saveCardTemplate(CardTemplate cardTemplate) {
        return cardTemplateRepository.save(cardTemplate);
    }

    @CacheEvict(value = { CacheConfig.CARD_TEMPLATES_CACHE, CacheConfig.CARD_TEMPLATE_BY_ID_CACHE,
            CacheConfig.CARD_SEARCH_CACHE, CacheConfig.EXPANSION_CARDS_CACHE,
            CacheConfig.SET_CARDS_CACHE }, allEntries = true)
    public List<CardTemplate> saveAllCardTemplates(List<? extends CardTemplate> cardTemplates) {
        return cardTemplateRepository.saveAll(cardTemplates.stream().map(card -> (CardTemplate) card).toList());
    }

    @CacheEvict(value = { CacheConfig.CARD_TEMPLATES_CACHE, CacheConfig.CARD_TEMPLATE_BY_ID_CACHE,
            CacheConfig.CARD_SEARCH_CACHE, CacheConfig.EXPANSION_CARDS_CACHE,
            CacheConfig.SET_CARDS_CACHE }, allEntries = true)
    public Optional<CardTemplate> updateCardTemplate(Long id, CardTemplate cardDetails) {
        return cardTemplateRepository.findById(id).map(card -> {
            card.setName(cardDetails.getName());
            card.setTcgType(cardDetails.getTcgType());
            card.setSetCode(cardDetails.getSetCode());
            card.setExpansion(cardDetails.getExpansion());
            card.setRarity(cardDetails.getRarity());
            card.setCardNumber(cardDetails.getCardNumber());
            card.setDescription(cardDetails.getDescription());
            card.setImageUrl(cardDetails.getImageUrl());
            card.setMarketPrice(cardDetails.getMarketPrice());
            card.setManaCost(cardDetails.getManaCost());
            return cardTemplateRepository.save(card);
        });
    }

    @CacheEvict(value = { CacheConfig.CARD_TEMPLATES_CACHE, CacheConfig.CARD_TEMPLATE_BY_ID_CACHE,
            CacheConfig.CARD_SEARCH_CACHE, CacheConfig.EXPANSION_CARDS_CACHE,
            CacheConfig.SET_CARDS_CACHE }, allEntries = true)
    public boolean deleteCardTemplate(Long id) {
        if (cardTemplateRepository.existsById(id)) {
            cardTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }

    @CacheEvict(value = { CacheConfig.CARD_TEMPLATES_CACHE, CacheConfig.CARD_TEMPLATE_BY_ID_CACHE,
            CacheConfig.CARD_SEARCH_CACHE, CacheConfig.EXPANSION_CARDS_CACHE,
            CacheConfig.SET_CARDS_CACHE }, allEntries = true)
    @Transactional
    public void deleteByTcgType(TCGType tcgType) {
        cardTemplateRepository.deleteByTcgType(tcgType);
    }

    public Expansion getExpansionByName(String name) {
        return expansionRepository.findByTitle(name);
    }

    public Expansion saveExpansion(Expansion expansion) {
        return expansionRepository.save(expansion);
    }

    /**
     * OPTIMIZED: Get all card counts grouped by setCode in a single query
     * 
     * @return Map of setCode -> count
     */
    public java.util.Map<String, Long> getAllCardCountsBySetCode() {
        List<Object[]> results = cardTemplateRepository.countAllGroupedBySetCode();
        java.util.Map<String, Long> countsMap = new java.util.HashMap<>();
        for (Object[] row : results) {
            String setCode = (String) row[0];
            Long count = (Long) row[1];
            if (setCode != null) {
                countsMap.put(setCode, count);
            }
        }
        return countsMap;
    }

    /**
     * OPTIMIZED: Get all card counts grouped by expansionId in a single query
     * 
     * @return Map of expansionId -> count
     */
    public java.util.Map<Long, Long> getAllCardCountsByExpansionId() {
        List<Object[]> results = cardTemplateRepository.countAllGroupedByExpansionId();
        java.util.Map<Long, Long> countsMap = new java.util.HashMap<>();
        for (Object[] row : results) {
            Long expansionId = (Long) row[0];
            Long count = (Long) row[1];
            if (expansionId != null) {
                countsMap.put(expansionId, count);
            }
        }
        return countsMap;
    }

    /**
     * Get random card templates from recent years
     * Used for the "Explore Cards" section in the home screen
     *
     * @param years Number of years to look back (default: 2)
     * @param limit Maximum number of cards to return (default: 20)
     * @return List of random recent card templates
     */
    public List<CardTemplate> getRandomRecentCards(int years, int limit) {
        return cardTemplateRepository.findRandomRecentCards(years, limit);
    }
}