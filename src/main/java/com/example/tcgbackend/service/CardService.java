package com.example.tcgbackend.service;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.repository.CardRepository;
import com.example.tcgbackend.repository.ExpansionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CardService {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private ExpansionRepository expansionRepository;

    @Autowired
    private TCGApiClient tcgApiClient; // External API client

    public List<Card> getAllCards() {
        return cardRepository.findAll();
    }

    public Optional<Card> getCardById(Long id) {
        return cardRepository.findById(id);
    }

    public Card saveCard(Card card) {
        return cardRepository.save(card);
    }

    public Optional<Card> updateCard(Long id, Card cardDetails) {
        return cardRepository.findById(id).map(card -> {
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
            card.setCondition(cardDetails.getCondition());
            card.setIsGraded(cardDetails.getIsGraded());
            card.setGradeService(cardDetails.getGradeService());
            card.setGradeScore(cardDetails.getGradeScore());
            return cardRepository.save(card);
        });
    }

    public boolean deleteCard(Long id) {
        if (cardRepository.existsById(id)) {
            cardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public List<Card> getUserCollection(Long userId) {
        return cardRepository.findByOwnerId(userId);
    }

    public Card addCardToCollection(Card card, Long userId) {
        card.setOwnerId(userId);
        card.setDateAdded(LocalDateTime.now());
        return cardRepository.save(card);
    }

    public void updateMarketPrices() {
        // Update market prices from external APIs
        List<Card> cards = cardRepository.findAll();
        for (Card card : cards) {
            Double marketPrice = tcgApiClient.getMarketPrice(card.getName(), card.getSetCode());
            card.setMarketPrice(marketPrice);
        }
        cardRepository.saveAll(cards);
    }

    // Expansion management methods
    public Expansion getExpansionByName(String name) {
        return expansionRepository.findByTitle(name);
    }

    public Expansion saveExpansion(Expansion expansion) {
        return expansionRepository.save(expansion);
    }

    public List<Expansion> getAllExpansions() {
        return expansionRepository.findAll();
    }
}