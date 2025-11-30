package com.example.tcgbackend.service;

import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGSet;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ExpansionService {

    @Autowired
    private ExpansionRepository expansionRepository;

    public List<Expansion> getAllExpansions() {
        return expansionRepository.findAll();
    }

    public Optional<Expansion> getExpansionById(Long id) {
        return expansionRepository.findById(id);
    }

    public List<Expansion> getExpansionsByTcgType(TCGType tcgType) {
        return expansionRepository.findByTcgType(tcgType);
    }

    public List<Expansion> getRecentExpansions() {
        // Implement logic to get recent expansions
        return getRecentExpansions(5);
    }

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