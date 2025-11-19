package com.example.tcgbackend.service;

import com.example.tcgbackend.model.*;
import com.example.tcgbackend.repository.CardRepository;
import com.example.tcgbackend.repository.DeckCardRepository;
import com.example.tcgbackend.repository.DeckRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DeckService {
    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private DeckCardRepository deckCardRepository;

    @Autowired
    private CardRepository cardRepository;

    public List<Deck> getAllDecks() {
        return deckRepository.findAll();
    }

    public Optional<Deck> getDeckById(Long id) {
        return deckRepository.findById(id);
    }

    public Deck saveDeck(Deck deck) {
        return deckRepository.save(deck);
    }

    public Optional<Deck> updateDeck(Long id, Deck deckDetails) {
        return deckRepository.findById(id).map(deck -> {
            deck.setName(deckDetails.getName());
            deck.setTcgType(deckDetails.getTcgType());
            deck.setDateModified(LocalDateTime.now());
            deck.setIsPublic(deckDetails.getIsPublic());
            deck.setDescription(deckDetails.getDescription());
            deck.setTags(deckDetails.getTags());
            return deckRepository.save(deck);
        });
    }

    public boolean deleteDeck(Long id) {
        if (deckRepository.existsById(id)) {
            deckRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Deck createDeck(String name, TCGType tcgType, Long ownerId) {
        Deck deck = new Deck();
        deck.setName(name);
        deck.setTcgType(tcgType);
        deck.setOwnerId(ownerId);
        deck.setDateCreated(LocalDateTime.now());
        deck.setDateModified(LocalDateTime.now());
        return deckRepository.save(deck);
    }

    public Deck addCardToDeck(Long deckId, Long cardId, int quantity) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck not found"));

        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCardId(cardId);
        deckCard.setQuantity(quantity);
        deckCard.setCardName(card.getName());
        deckCard.setCardImageUrl(card.getImageUrl());

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        return deckRepository.save(deck);
    }

    public boolean removeCardFromDeck(Long deckId, Long cardId) {
        List<DeckCard> deckCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard deckCard : deckCards) {
            if (deckCard.getCardId().equals(cardId)) {
                deckCardRepository.delete(deckCard);
                return true;
            }
        }
        return false;
    }

    public List<Deck> getPublicDecks() {
        return deckRepository.findByIsPublicTrue();
    }
}