package com.example.tcgbackend.service;

import com.example.tcgbackend.model.ProDeck;
import com.example.tcgbackend.model.ProDeckCard;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.ProDeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ProDeck> getProDecksByTcgType(TCGType tcgType) {
        return proDeckRepository.findByTcgType(tcgType);
    }

    public List<ProDeck> getProDecksByAuthor(String author) {
        return proDeckRepository.findByAuthor(author);
    }

    public List<ProDeck> getProDecksByTournament(String tournament) {
        return proDeckRepository.findByTournament(tournament);
    }

    public List<ProDeck> getRecentProDecks() {
        return proDeckRepository.findTop10ByOrderByCreatedAtDesc();
    }

    @Transactional
    public ProDeck saveProDeck(ProDeck proDeck) {
        proDeck.setUpdatedAt(java.time.LocalDateTime.now());
        return proDeckRepository.save(proDeck);
    }

    @Transactional
    public boolean deleteProDeck(Long id) {
        if (proDeckRepository.existsById(id)) {
            proDeckRepository.deleteById(id);
            return true;
        }
        return false;
    }
}