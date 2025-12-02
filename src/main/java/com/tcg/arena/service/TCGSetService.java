package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.TCGSet;
import com.tcg.arena.repository.TCGSetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TCGSetService {

    @Autowired
    private TCGSetRepository tcgSetRepository;

    @Cacheable(value = CacheConfig.SETS_CACHE, key = "'all'")
    public List<TCGSet> getAllSets() {
        return tcgSetRepository.findAllByOrderByReleaseDateDesc();
    }

    public Optional<TCGSet> getSetById(Long id) {
        return tcgSetRepository.findById(id);
    }

    public Optional<TCGSet> getSetBySetCode(String setCode) {
        return tcgSetRepository.findBySetCode(setCode);
    }

    public TCGSet saveSet(TCGSet set) {
        return tcgSetRepository.save(set);
    }

    public Optional<TCGSet> updateSet(Long id, TCGSet setDetails) {
        return tcgSetRepository.findById(id).map(set -> {
            set.setName(setDetails.getName());
            set.setSetCode(setDetails.getSetCode());
            set.setImageUrl(setDetails.getImageUrl());
            set.setReleaseDate(setDetails.getReleaseDate());
            set.setCardCount(setDetails.getCardCount());
            set.setDescription(setDetails.getDescription());
            set.setExpansion(setDetails.getExpansion());
            return tcgSetRepository.save(set);
        });
    }

    public boolean deleteSet(Long id) {
        if (tcgSetRepository.existsById(id)) {
            tcgSetRepository.deleteById(id);
            return true;
        }
        return false;
    }
}