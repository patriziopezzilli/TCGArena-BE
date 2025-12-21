package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
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
        return cardTemplateRepository.findByExpansionId(expansionId, pageable);
    }

    @Cacheable(value = CacheConfig.SET_CARDS_CACHE, key = "'setCode_' + #setCode + '_page_' + #pageable.pageNumber + '_size_' + #pageable.pageSize")
    public Page<CardTemplate> getCardTemplatesBySetCode(String setCode, Pageable pageable) {
        return cardTemplateRepository.findBySetCode(setCode, pageable);
    }

    public long countCardsBySetCode(String setCode) {
        return cardTemplateRepository.countBySetCode(setCode);
    }

    public long countCardsByExpansionId(Long expansionId) {
        return cardTemplateRepository.countByExpansionId(expansionId);
    }

    public List<CardTemplate> getCardTemplatesByRarity(String rarity) {
        return cardTemplateRepository.findByRarity(rarity);
    }

    public List<CardTemplate> searchCardTemplates(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        // Smart search: Check for "Name Number" pattern
        String[] parts = query.trim().split("\\s+");
        if (parts.length >= 2) {
            // Try to treat the last part as a card number
            String potentialNumber = parts[parts.length - 1];
            // Reconstruct the name part (everything except last token)
            String potentialName = query.substring(0, query.lastIndexOf(potentialNumber)).trim();

            if (!potentialName.isEmpty()) {
                List<CardTemplate> smartResults = cardTemplateRepository.searchByNameAndCardNumber(potentialName,
                        potentialNumber);
                if (!smartResults.isEmpty()) {
                    return smartResults;
                }
            }
        }

        // Fallback to standard search
        return cardTemplateRepository.searchByNameOrSetCode(query);
    }

    public List<CardTemplate> smartScan(List<String> rawTexts) {
        if (rawTexts == null || rawTexts.isEmpty()) {
            return List.of();
        }

        // 1. Flatten and Tokenize
        // Split strings by spaces and clean them
        List<String> tokens = rawTexts.stream()
                .filter(s -> s != null && !s.isBlank())
                .flatMap(s -> java.util.Arrays.stream(s.split("\\s+")))
                .map(String::trim)
                .filter(s -> s.length() > 1) // Ignore single chars
                .collect(java.util.stream.Collectors.toList());

        // 2. Identify "Long Tokens" (potential names) - keep original phrases for fuzzy
        // name match
        String longestPhrase = rawTexts.stream()
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse("");

        // Add potential names to tokens to check exact match
        tokens.addAll(rawTexts);

        // 4. Query DB
        // We pass the token list to check against card_number and accurate name
        // We pass the longestPhrase to check partial name match
        return cardTemplateRepository.findBySmartScanTokens(tokens, longestPhrase);
    }

    public Page<CardTemplate> searchCardTemplatesWithFilters(
            String tcgType,
            Long expansionId,
            String setCode,
            String rarity,
            String searchQuery,
            Pageable pageable) {
        return cardTemplateRepository.findWithFilters(tcgType, expansionId, setCode, rarity, searchQuery, pageable);
    }

    public CardTemplate saveCardTemplate(CardTemplate cardTemplate) {
        return cardTemplateRepository.save(cardTemplate);
    }

    public List<CardTemplate> saveAllCardTemplates(List<? extends CardTemplate> cardTemplates) {
        return cardTemplateRepository.saveAll(cardTemplates.stream().map(card -> (CardTemplate) card).toList());
    }

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

    public boolean deleteCardTemplate(Long id) {
        if (cardTemplateRepository.existsById(id)) {
            cardTemplateRepository.deleteById(id);
            return true;
        }
        return false;
    }

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
}