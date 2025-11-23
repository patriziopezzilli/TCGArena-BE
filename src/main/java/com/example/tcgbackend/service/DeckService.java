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

    @Autowired
    private UserActivityService userActivityService;

    public List<Deck> getAllDecks() {
        return deckRepository.findAll();
    }

    public Optional<Deck> getDeckById(Long id) {
        return deckRepository.findById(id);
    }

    public Deck saveDeck(Deck deck) {
        Deck savedDeck = deckRepository.save(deck);

        // Log deck creation activity
        userActivityService.logActivity(deck.getOwnerId(),
            com.example.tcgbackend.model.ActivityType.DECK_CREATED,
            "Created deck '" + deck.getName() + "'");

        return savedDeck;
    }

    public Optional<Deck> updateDeck(Long id, Deck deckDetails) {
        return deckRepository.findById(id).map(deck -> {
            deck.setName(deckDetails.getName());
            deck.setTcgType(deckDetails.getTcgType());
            deck.setDateModified(LocalDateTime.now());
            deck.setIsPublic(deckDetails.getIsPublic());
            deck.setDescription(deckDetails.getDescription());
            deck.setTags(deckDetails.getTags());
            Deck updatedDeck = deckRepository.save(deck);

            userActivityService.logActivity(deck.getOwnerId(),
                com.example.tcgbackend.model.ActivityType.DECK_UPDATED,
                "Updated deck '" + deck.getName() + "'");

            return updatedDeck;
        });
    }

    public boolean deleteDeck(Long id, Long userId) {
        Optional<Deck> deckOpt = deckRepository.findById(id);
        if (deckOpt.isPresent()) {
            Deck deck = deckOpt.get();
            userActivityService.logActivity(userId, ActivityType.DECK_DELETED, "Deleted deck: " + deck.getName());
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
        Deck savedDeck = deckRepository.save(deck);

        userActivityService.logActivity(ownerId, ActivityType.DECK_CREATED, "Created new deck: " + name);

        return savedDeck;
    }

    public Deck addCardToDeck(Long deckId, Long cardId, int quantity, Long userId) {
        Deck deck = deckRepository.findById(deckId)
            .orElseThrow(() -> new RuntimeException("Deck not found"));

        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new RuntimeException("Card not found"));

        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCardId(cardId);
        deckCard.setQuantity(quantity);
        deckCard.setCardName(card.getCardTemplate().getName());
        deckCard.setCardImageUrl(card.getCardTemplate().getImageUrl());

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED, "Added " + quantity + "x " + card.getCardTemplate().getName() + " to deck '" + deck.getName() + "'");

        return savedDeck;
    }

    public boolean removeCardFromDeck(Long deckId, Long cardId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null) return false;

        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null) return false;

        List<DeckCard> deckCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard deckCard : deckCards) {
            if (deckCard.getCardId().equals(cardId)) {
                deckCardRepository.delete(deckCard);
                deck.setDateModified(LocalDateTime.now());
                deckRepository.save(deck);

                // Log deck update activity
                userActivityService.logActivity(userId, ActivityType.DECK_UPDATED, "Removed " + deckCard.getQuantity() + "x " + card.getCardTemplate().getName() + " from deck '" + deck.getName() + "'");

                return true;
            }
        }
        return false;
    }

    public List<Deck> getPublicDecks() {
        return deckRepository.findByIsPublicTrue();
    }
}