package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.model.ProDeck;
import com.tcg.arena.repository.ProDeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProDeckService {

    @Autowired
    private ProDeckRepository proDeckRepository;

    @Cacheable(value = CacheConfig.PRO_DECKS_CACHE, key = "'all'")
    public List<ProDeck> getAllProDecks() {
        return proDeckRepository.findAll();
    }

    @Cacheable(value = CacheConfig.PRO_DECK_BY_ID_CACHE, key = "#id")
    public Optional<ProDeck> getProDeckById(Long id) {
        return proDeckRepository.findById(id);
    }

    @Cacheable(value = CacheConfig.RECENT_PRO_DECKS_CACHE, key = "'recent'")
    public List<ProDeck> getRecentProDecks() {
        return proDeckRepository.findTop10ByOrderByCreatedAtDesc();
    }
}