package com.example.tcgbackend.service;

import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.model.UserCard;
import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.DeckCard;
import com.example.tcgbackend.model.GradeService;
import com.example.tcgbackend.repository.CardTemplateRepository;
import com.example.tcgbackend.repository.UserCardRepository;
import com.example.tcgbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserCardService {
    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private DeckService deckService;

    public List<UserCard> getAllUserCards() {
        return userCardRepository.findAll();
    }

    public Optional<UserCard> getUserCardById(Long id) {
        return userCardRepository.findById(id);
    }

    public List<UserCard> getUserCardsByUserId(Long userId) {
        return userCardRepository.findByOwnerId(userId);
    }

    public List<UserCard> getUserCardsByCardTemplateId(Long cardTemplateId) {
        return userCardRepository.findByCardTemplateId(cardTemplateId);
    }

    public UserCard saveUserCard(UserCard userCard) {
        return userCardRepository.save(userCard);
    }

    public Optional<UserCard> updateUserCard(Long id, UserCard userCardDetails) {
        return userCardRepository.findById(id).map(userCard -> {
            userCard.setCondition(userCardDetails.getCondition());
            userCard.setIsGraded(userCardDetails.getIsGraded());
            userCard.setGradeService(userCardDetails.getGradeService());
            userCard.setGradeScore(userCardDetails.getGradeScore());
            userCard.setPurchasePrice(userCardDetails.getPurchasePrice());
            userCard.setDateAcquired(userCardDetails.getDateAcquired());
            UserCard savedUserCard = userCardRepository.save(userCard);

            // Sync with collection deck card
            syncUserCardToCollectionDeck(savedUserCard);

            return savedUserCard;
        });
    }

    public boolean deleteUserCard(Long id) {
        Optional<UserCard> userCardOpt = userCardRepository.findById(id);
        if (userCardOpt.isPresent()) {
            UserCard userCard = userCardOpt.get();

            // Remove from collection deck if it exists
            try {
                Optional<Deck> collectionDeckOpt = deckService.getCollectionDeckByUserId(userCard.getOwner().getId());
                if (collectionDeckOpt.isPresent()) {
                    Deck collectionDeck = collectionDeckOpt.get();
                    // Find and remove the corresponding DeckCard
                    Optional<DeckCard> deckCardOpt = collectionDeck.getCards().stream()
                        .filter(deckCard -> deckCard.getCardId().equals(userCard.getCardTemplate().getId()))
                        .findFirst();
                    
                    if (deckCardOpt.isPresent()) {
                        deckService.removeDeckCardByCardId(deckCardOpt.get().getId(), userCard.getOwner().getId());
                    }
                }
            } catch (Exception e) {
                // Log error but don't fail the user card deletion
                System.err.println("Error removing card from collection deck: " + e.getMessage());
            }

            // Log activity before deletion
            userActivityService.logActivity(userCard.getOwner().getId(),
                com.example.tcgbackend.model.ActivityType.CARD_REMOVED_FROM_COLLECTION,
                "Removed " + userCard.getCardTemplate().getName() + " from collection");

            userCardRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public UserCard addCardToUserCollection(CardTemplate cardTemplate, User owner, CardCondition condition) {
        UserCard userCard = new UserCard();
        userCard.setCardTemplate(cardTemplate);
        userCard.setOwner(owner);
        userCard.setCondition(condition);
        userCard.setIsGraded(false);
        userCard.setDateAdded(LocalDateTime.now());
        UserCard savedCard = userCardRepository.save(userCard);

        // Add to collection deck if it exists
        try {
            Optional<Deck> collectionDeckOpt = deckService.getCollectionDeckByUserId(owner.getId());
            if (collectionDeckOpt.isPresent()) {
                Deck collectionDeck = collectionDeckOpt.get();
                // Check if card already exists in collection deck
                boolean cardExists = collectionDeck.getCards().stream()
                    .anyMatch(deckCard -> deckCard.getCardId().equals(cardTemplate.getId()));
                
                if (!cardExists) {
                    // Add card to collection deck
                    deckService.addCardTemplateToDeck(collectionDeck.getId(), cardTemplate.getId(), owner.getId());
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the user card creation
            System.err.println("Error adding card to collection deck: " + e.getMessage());
        }

        // Log activity
        userActivityService.logActivity(owner.getId(),
            com.example.tcgbackend.model.ActivityType.CARD_ADDED_TO_COLLECTION,
            "Added " + cardTemplate.getName() + " to collection");

        return savedCard;
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<CardTemplate> getCardTemplateById(Long id) {
        return cardTemplateRepository.findById(id);
    }

    private void syncUserCardToCollectionDeck(UserCard userCard) {
        try {
            Long userId = userCard.getOwner().getId();
            Long cardTemplateId = userCard.getCardTemplate().getId();

            // Find the collection deck for this user
            Optional<Deck> collectionDeckOpt = deckService.getCollectionDeckByUserId(userId);
            if (collectionDeckOpt.isEmpty()) {
                return; // No collection deck found, skip sync
            }

            Deck collectionDeck = collectionDeckOpt.get();

            // Find the corresponding DeckCard in the collection deck
            Optional<DeckCard> deckCardOpt = collectionDeck.getCards().stream()
                .filter(deckCard -> deckCard.getCardId().equals(cardTemplateId))
                .findFirst();

            if (deckCardOpt.isPresent()) {
                DeckCard deckCard = deckCardOpt.get();
                boolean hasChanges = false;

                // Check if data actually changed before updating
                if (!deckCard.getCondition().equals(userCard.getCondition())) {
                    deckCard.setCondition(userCard.getCondition());
                    hasChanges = true;
                }
                if (!deckCard.getIsGraded().equals(userCard.getIsGraded())) {
                    deckCard.setIsGraded(userCard.getIsGraded());
                    hasChanges = true;
                }
                if (!java.util.Objects.equals(deckCard.getGradeService(), userCard.getGradeService())) {
                    deckCard.setGradeService(userCard.getGradeService());
                    hasChanges = true;
                }

                // Convert gradeScore to grade string for DeckCard
                String newGrade = userCard.getGradeScore() != null ? userCard.getGradeScore().toString() : null;
                if (!java.util.Objects.equals(deckCard.getGrade(), newGrade)) {
                    deckCard.setGrade(newGrade);
                    hasChanges = true;
                }

                // Only save if there are actual changes
                if (hasChanges) {
                    deckService.saveDeckCard(deckCard);

                    // Update deck modification date
                    collectionDeck.setDateModified(java.time.LocalDateTime.now());
                    deckService.saveDeck(collectionDeck);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the UserCard update
            System.err.println("Error syncing UserCard to collection deck: " + e.getMessage());
        }
    }
}