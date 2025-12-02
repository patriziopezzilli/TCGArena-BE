package com.tcg.arena.service;

import com.tcg.arena.dto.DeckCardUpdateDTO;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.CardRepository;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.DeckCardRepository;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.UserCardRepository;
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
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private UserActivityService userActivityService;

    public List<Deck> getAllDecks() {
        return deckRepository.findAll();
    }

    public List<Deck> getDecksByOwnerId(Long ownerId) {
        return deckRepository.findByOwnerIdOrderByDateCreatedDesc(ownerId);
    }

    public Optional<Deck> getDeckById(Long id) {
        return deckRepository.findById(id);
    }

    public Deck saveDeck(Deck deck) {
        Deck savedDeck = deckRepository.save(deck);

        // Log deck creation activity
        userActivityService.logActivity(deck.getOwnerId(),
                com.tcg.arena.model.ActivityType.DECK_CREATED,
                "Created deck '" + deck.getName() + "'");

        return savedDeck;
    }

    public Optional<Deck> updateDeck(Long id, Deck deckDetails) {
        return deckRepository.findById(id).map(deck -> {
            deck.setName(deckDetails.getName());
            deck.setTcgType(deckDetails.getTcgType());
            deck.setDeckType(deckDetails.getDeckType());
            deck.setDateModified(LocalDateTime.now());
            deck.setIsPublic(deckDetails.getIsPublic());
            deck.setDescription(deckDetails.getDescription());
            deck.setTags(deckDetails.getTags());
            Deck updatedDeck = deckRepository.save(deck);

            userActivityService.logActivity(deck.getOwnerId(),
                    com.tcg.arena.model.ActivityType.DECK_UPDATED,
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

    public Deck createDeck(String name, TCGType tcgType, DeckType deckType, Long ownerId) {
        Deck deck = new Deck();
        deck.setName(name);
        deck.setTcgType(tcgType);
        deck.setDeckType(deckType != null ? deckType : DeckType.LISTA);
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
        deckCard.setCondition(CardCondition.MINT); // Default condition
        deckCard.setIsGraded(false); // Default grading status

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                "Added " + quantity + "x " + card.getCardTemplate().getName() + " to deck '" + deck.getName() + "'");

        return savedDeck;
    }

    public Deck addCardTemplateToDeck(Long deckId, Long templateId, Long userId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found"));

        CardTemplate template = cardTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Card template not found"));

        // Check if a card with this template already exists in the deck
        List<DeckCard> existingCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard existingCard : existingCards) {
            if (existingCard.getCardId().equals(templateId)) {
                // Card already exists, increment quantity
                existingCard.setQuantity(existingCard.getQuantity() + 1);
                deckCardRepository.save(existingCard);
                deck.setDateModified(LocalDateTime.now());
                Deck savedDeck = deckRepository.save(deck);

                userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                        "Added 1x " + template.getName() + " to deck '" + deck.getName() + "'");

                return savedDeck;
            }
        }

        // Card doesn't exist, create new DeckCard entry
        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCardId(templateId);
        deckCard.setQuantity(1);
        deckCard.setCardName(template.getName());
        deckCard.setCardImageUrl(template.getImageUrl());
        deckCard.setCondition(CardCondition.MINT); // Default condition
        
        // Check if user has this card in their personal collection and copy grading info
        List<UserCard> userCards = userCardRepository.findByCardTemplateId(templateId);
        UserCard existingUserCard = userCards.stream()
                .filter(uc -> uc.getOwner().getId().equals(userId))
                .findFirst()
                .orElse(null);
        
        if (existingUserCard != null) {
            // Copy grading information from user's personal collection
            deckCard.setIsGraded(existingUserCard.getIsGraded());
            deckCard.setGradeService(existingUserCard.getGradeService());
            deckCard.setGrade(existingUserCard.getGradeScore() != null ? existingUserCard.getGradeScore().toString() : null);
            deckCard.setCondition(existingUserCard.getCondition()); // Also copy condition
        } else {
            deckCard.setIsGraded(false); // Default grading status
        }

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                "Added 1x " + template.getName() + " to deck '" + deck.getName() + "'");

        return savedDeck;
    }

    public boolean removeCardFromDeck(Long deckId, Long cardId, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null)
            return false;

        Card card = cardRepository.findById(cardId).orElse(null);
        if (card == null)
            return false;

        List<DeckCard> deckCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard deckCard : deckCards) {
            if (deckCard.getCardId().equals(cardId)) {
                deckCardRepository.delete(deckCard);
                deck.setDateModified(LocalDateTime.now());
                deckRepository.save(deck);

                // Log deck update activity
                userActivityService.logActivity(userId, ActivityType.DECK_UPDATED, "Removed " + deckCard.getQuantity()
                        + "x " + card.getCardTemplate().getName() + " from deck '" + deck.getName() + "'");

                return true;
            }
        }
        return false;
    }

    public boolean updateDeckCardCondition(Long deckId, Long deckCardId, CardCondition condition, Long userId) {
        Deck deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null || !deck.getOwnerId().equals(userId)) {
            return false;
        }

        DeckCard deckCard = deckCardRepository.findById(deckCardId).orElse(null);
        if (deckCard == null || !deckCard.getDeck().getId().equals(deckId)) {
            return false;
        }

        deckCard.setCondition(condition);
        deckCardRepository.save(deckCard);
        deck.setDateModified(LocalDateTime.now());
        deckRepository.save(deck);

        return true;
    }

    public boolean updateDeckCardConditionByCardId(Long cardId, CardCondition condition, Long userId) {
        // Find the DeckCard by its ID (which is the deck_card.id, not card_template.id)
        DeckCard deckCard = deckCardRepository.findById(cardId).orElse(null);
        if (deckCard == null) {
            return false;
        }

        // Verify the user owns the deck
        Deck deck = deckCard.getDeck();
        if (!deck.getOwnerId().equals(userId)) {
            return false;
        }

        deckCard.setCondition(condition);
        deckCardRepository.save(deckCard);
        deck.setDateModified(LocalDateTime.now());
        deckRepository.save(deck);

        return true;
    }

    public boolean updateDeckCardByCardId(Long cardId, DeckCardUpdateDTO updateDTO, Long userId) {
        // Find the DeckCard by its ID
        DeckCard deckCard = deckCardRepository.findById(cardId).orElse(null);
        if (deckCard == null) {
            return false;
        }

        // Verify the user owns the deck
        Deck deck = deckCard.getDeck();
        if (!deck.getOwnerId().equals(userId)) {
            return false;
        }

        // Update fields if provided
        boolean hasChanges = false;
        if (updateDTO.getQuantity() != null) {
            deckCard.setQuantity(updateDTO.getQuantity());
            hasChanges = true;
        }
        if (updateDTO.getCondition() != null) {
            deckCard.setCondition(updateDTO.getCondition());
            hasChanges = true;
        }
        
        // Handle grading fields - if isGraded is provided, update all grading fields
        if (updateDTO.getIsGraded() != null) {
            deckCard.setIsGraded(updateDTO.getIsGraded());
            deckCard.setGradeService(updateDTO.getGradeService());
            deckCard.setGrade(updateDTO.getGrade());
            deckCard.setCertificateNumber(updateDTO.getCertificateNumber());
            hasChanges = true;
        }

        if (hasChanges) {
            deckCardRepository.save(deckCard);
            deck.setDateModified(LocalDateTime.now());
            deckRepository.save(deck);

            // If this is the collection deck, sync changes to the corresponding UserCard
            if ("Collection".equals(deck.getName()) && DeckType.LISTA.equals(deck.getDeckType())) {
                syncDeckCardToUserCard(deckCard, userId);
            }
        }

        return true;
    }

    public boolean removeDeckCardByCardId(Long cardId, Long userId) {
        // Find the DeckCard by its ID
        DeckCard deckCard = deckCardRepository.findById(cardId).orElse(null);
        if (deckCard == null) {
            return false;
        }

        // Verify the user owns the deck
        Deck deck = deckCard.getDeck();
        if (!deck.getOwnerId().equals(userId)) {
            return false;
        }

        deckCardRepository.delete(deckCard);
        deck.setDateModified(LocalDateTime.now());
        deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                "Removed card from deck '" + deck.getName() + "'");

        return true;
    }

    public List<Deck> getPublicDecks() {
        return deckRepository.findByIsPublicTrueOrderByDateCreatedDesc();
    }

    public Optional<Deck> getCollectionDeckByUserId(Long userId) {
        List<Deck> userDecks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(userId);
        return userDecks.stream()
                .filter(deck -> "Collection".equals(deck.getName()) && DeckType.LISTA.equals(deck.getDeckType()))
                .findFirst();
    }

    // Migration method to update existing decks with null deckType
    public void migrateExistingDecksToDefaultType() {
        List<Deck> decks = deckRepository.findAll();
        for (Deck deck : decks) {
            if (deck.getDeckType() == null) {
                deck.setDeckType(com.tcg.arena.model.DeckType.LISTA);
                deckRepository.save(deck);
            }
        }
    }

    public DeckCard saveDeckCard(DeckCard deckCard) {
        return deckCardRepository.save(deckCard);
    }

    private void syncDeckCardToUserCard(DeckCard deckCard, Long userId) {
        try {
            Long cardTemplateId = deckCard.getCardId();

            // Find the corresponding UserCard
            List<UserCard> userCards = userCardRepository.findByCardTemplateId(cardTemplateId);
            UserCard userCard = userCards.stream()
                .filter(uc -> uc.getOwner().getId().equals(userId))
                .findFirst()
                .orElse(null);

            if (userCard != null) {
                boolean hasChanges = false;

                // Check if data actually changed before updating
                if (!userCard.getCondition().equals(deckCard.getCondition())) {
                    userCard.setCondition(deckCard.getCondition());
                    hasChanges = true;
                }
                if (!userCard.getIsGraded().equals(deckCard.getIsGraded())) {
                    userCard.setIsGraded(deckCard.getIsGraded());
                    hasChanges = true;
                }
                if (!java.util.Objects.equals(userCard.getGradeService(), deckCard.getGradeService())) {
                    userCard.setGradeService(deckCard.getGradeService());
                    hasChanges = true;
                }

                // Convert grade string to gradeScore for UserCard
                Integer newGradeScore = null;
                if (deckCard.getGrade() != null) {
                    try {
                        newGradeScore = Integer.parseInt(deckCard.getGrade());
                    } catch (NumberFormatException e) {
                        newGradeScore = null;
                    }
                }
                if (!java.util.Objects.equals(userCard.getGradeScore(), newGradeScore)) {
                    userCard.setGradeScore(newGradeScore);
                    hasChanges = true;
                }

                // Only save if there are actual changes
                if (hasChanges) {
                    userCardRepository.save(userCard);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the DeckCard update
            System.err.println("Error syncing DeckCard to UserCard: " + e.getMessage());
        }
    }

    /**
     * Creates starter decks for a new user based on their favorite TCG types
     * Creates "Collezione" and "Wishlist" decks for each favorite TCG
     * @param userId The ID of the user
     * @param favoriteTCGTypes List of favorite TCG types
     */
    public void createStarterDecksForUser(Long userId, List<TCGType> favoriteTCGTypes) {
        if (favoriteTCGTypes == null || favoriteTCGTypes.isEmpty()) {
            return;
        }

        for (TCGType tcgType : favoriteTCGTypes) {
            // Create "Collezione" deck
            Deck collectionDeck = new Deck();
            collectionDeck.setOwnerId(userId);
            collectionDeck.setName("Collezione " + tcgType.getDisplayName());
            collectionDeck.setDescription("La tua collezione di carte " + tcgType.getDisplayName());
            collectionDeck.setTcgType(tcgType);
            collectionDeck.setDeckType(DeckType.LISTA);
            collectionDeck.setDateCreated(LocalDateTime.now());
            collectionDeck.setDateModified(LocalDateTime.now());
            collectionDeck.setIsPublic(false);
            deckRepository.save(collectionDeck);
            
            System.out.println("Created collection deck for user " + userId + ": " + collectionDeck.getName());
            
            // Create "Wishlist" deck
            Deck wishlistDeck = new Deck();
            wishlistDeck.setOwnerId(userId);
            wishlistDeck.setName("Wishlist " + tcgType.getDisplayName());
            wishlistDeck.setDescription("Le carte che desideri per " + tcgType.getDisplayName());
            wishlistDeck.setTcgType(tcgType);
            wishlistDeck.setDeckType(DeckType.LISTA);
            wishlistDeck.setDateCreated(LocalDateTime.now());
            wishlistDeck.setDateModified(LocalDateTime.now());
            wishlistDeck.setIsPublic(false);
            deckRepository.save(wishlistDeck);
            
            System.out.println("Created wishlist deck for user " + userId + ": " + wishlistDeck.getName());
        }
    }
}