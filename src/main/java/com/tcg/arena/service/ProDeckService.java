package com.tcg.arena.service;

import com.tcg.arena.model.ProDeck;
import com.tcg.arena.repository.ProDeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProDeckService {

    @Autowired
    private ProDeckRepository proDeckRepository;

    public List<ProDeck> getAllProDecks() {
        return proDeckRepository.findAll();
    }

    public Optional<ProDeck> getProDeckById(Long id) {
        return proDeckRepository.findById(id);
    }

    public List<ProDeck> getRecentProDecks() {
        return proDeckRepository.findTop10ByOrderByCreatedAtDesc();
    }
}