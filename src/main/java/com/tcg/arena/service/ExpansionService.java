package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGSet;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpansionService {

    @Autowired
    private ExpansionRepository expansionRepository;

    @Cacheable(value = CacheConfig.EXPANSIONS_CACHE, key = "'all'")
    public List<Expansion> getAllExpansions() {
        return expansionRepository.findAllByOrderByReleaseDateDesc();
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

    @Cacheable(value = CacheConfig.RECENT_EXPANSIONS_CACHE, key = "'limit_' + #limit")
    public List<Expansion> getRecentExpansions(int limit) {
        return expansionRepository.findAllByOrderByReleaseDateDesc().stream().limit(limit).collect(Collectors.toList());
    }

    public List<TCGSet> getSetsByExpansionId(Long id) {
        return expansionRepository.findByIdWithSets(id).map(Expansion::getSets).orElse(List.of());
    }

    public Expansion saveExpansion(Expansion expansion) {
        return expansionRepository.save(expansion);
    }

    public Optional<Expansion> updateExpansion(Long id, Expansion expansionDetails) {
        return expansionRepository.findById(id).map(expansion -> {
            expansion.setTitle(expansionDetails.getTitle());
            expansion.setTcgType(expansionDetails.getTcgType());
            expansion.setImageUrl(expansionDetails.getImageUrl());
            return expansionRepository.save(expansion);
        });
    }

    public boolean deleteExpansion(Long id) {
        if (expansionRepository.existsById(id)) {
            expansionRepository.deleteById(id);
            return true;
        }
        return false;
    }
}