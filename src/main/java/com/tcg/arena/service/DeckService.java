package com.tcg.arena.service;

import com.tcg.arena.dto.DeckCardUpdateDTO;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.DeckCardRepository;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.UserCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DeckService {
    private static final Logger logger = LoggerFactory.getLogger(DeckService.class);

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private DeckCardRepository deckCardRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Autowired
    private UserCardRepository userCardRepository;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private RewardService rewardService;

    @Autowired
    private com.tcg.arena.repository.UserRepository userRepository;

    @Autowired
    private com.tcg.arena.repository.DeckLikeRepository deckLikeRepository;

    @Autowired
    private NotificationService notificationService;

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
                "Creato mazzo '" + deck.getName() + "'");

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
                    "Aggiornato mazzo '" + deck.getName() + "'");

            return updatedDeck;
        });
    }

    public boolean deleteDeck(Long id, Long userId) {
        Optional<Deck> deckOpt = deckRepository.findById(id);
        if (deckOpt.isPresent()) {
            Deck deck = deckOpt.get();
            userActivityService.logActivity(userId, ActivityType.DECK_DELETED, "Eliminato mazzo: " + deck.getName());
            deckRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Deck createDeck(String name, TCGType tcgType, DeckType deckType, Long ownerId) {
        return createDeck(name, null, tcgType, deckType, ownerId);
    }

    public Deck createDeck(String name, String description, TCGType tcgType, DeckType deckType, Long ownerId) {
        Deck deck = new Deck();
        deck.setName(name);
        deck.setDescription(description);
        deck.setTcgType(tcgType);
        deck.setDeckType(deckType != null ? deckType : DeckType.LISTA);
        deck.setOwnerId(ownerId);
        deck.setDateCreated(LocalDateTime.now());
        deck.setDateModified(LocalDateTime.now());
        Deck savedDeck = deckRepository.save(deck);

        userActivityService.logActivity(ownerId, ActivityType.DECK_CREATED, "Creato nuovo mazzo: " + name);

        // Award points for deck creation (+50 for first deck only)
        // Only for LISTA type decks (not system Collection/Wishlist decks)
        if (deckType == DeckType.LISTA) {
            List<Deck> userDecks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(ownerId);
            long userListaDecks = userDecks.stream().filter(d -> d.getDeckType() == DeckType.LISTA).count();

            if (userListaDecks == 1) {
                rewardService.earnPoints(ownerId, 50, "Primo mazzo creato: " + name);
            }
            // Removed +10 points for additional decks to prevent abuse
        }

        return savedDeck;
    }

    public Deck addCardToDeck(Long deckId, Long cardId, int quantity, String section, Long userId) {
        System.out.println("DeckService: Adding card " + cardId + " to deck " + deckId + " in section " + section
                + " with quantity " + quantity + " for user " + userId);

        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found"));

        System.out.println("DeckService: Found deck " + deck.getName());

        UserCard card = userCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found with ID: " + cardId
                        + ". Available cards for user " + userId + ": "
                        + userCardRepository.findByOwnerId(userId).stream().map(uc -> uc.getId().toString()).toList()));

        System.out.println(
                "DeckService: Found card " + card.getCardTemplate().getName() + " with ownerId "
                        + card.getOwner().getId());

        // Verify the card belongs to the user
        if (!card.getOwner().getId().equals(userId)) {
            throw new RuntimeException("Card with ID " + cardId + " does not belong to user " + userId
                    + " (belongs to user " + card.getOwner().getId() + ")");
        }

        System.out.println("DeckService: Card ownership verified");

        Long templateId = card.getCardTemplate().getId();
        System.out.println("DeckService: Using templateId " + templateId + " for duplicate checks and storage");

        // Check if card exists in the same section
        List<DeckCard> existingCards = deckCardRepository.findByDeckId(deckId);
        System.out.println("DeckService: Found " + existingCards.size() + " existing cards in deck");

        for (DeckCard existing : existingCards) {
            System.out.println("DeckService: Checking existing card " + existing.getCardId() + " in section "
                    + existing.getSection() + " vs templateId " + templateId + " in section " + section);
            if (existing.getCardId().equals(templateId) &&
                    (existing.getSection() == null ? section == null : existing.getSection().equals(section))) {
                System.out.println("DeckService: Found existing card, incrementing quantity from "
                        + existing.getQuantity() + " to " + (existing.getQuantity() + quantity));
                existing.setQuantity(existing.getQuantity() + quantity);
                deckCardRepository.save(existing);

                deck.setDateModified(LocalDateTime.now());
                return deckRepository.save(deck);
            }
        }

        System.out.println("DeckService: Creating new deck card");
        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCardId(templateId);
        deckCard.setQuantity(quantity);
        deckCard.setCardName(card.getCardTemplate().getName());
        deckCard.setCardImageUrl(card.getCardTemplate().getImageUrl());
        deckCard.setSection(section != null ? section : "MAIN");

        // Copy properties from the specific User Card
        deckCard.setCondition(card.getCondition());
        deckCard.setIsGraded(card.getIsGraded());
        deckCard.setGradeService(card.getGradeService());
        if (card.getGradeScore() != null) {
            deckCard.setGrade(card.getGradeScore().toString());
        }
        deckCard.setNationality(card.getNationality() != null ? card.getNationality() : CardNationality.EN);

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                "Aggiunte " + quantity + "x " + card.getCardTemplate().getName() + " al mazzo '" + deck.getName()
                        + "' (" + deckCard.getSection() + ")");

        System.out.println("DeckService: Successfully added card to deck");
        return savedDeck;
    }

    public Deck addCardTemplateToDeck(Long deckId, Long templateId, String section, Long userId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found"));

        CardTemplate template = cardTemplateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Card template not found"));

        // Check if a card with this template already exists in the deck AND in the same
        // section
        List<DeckCard> existingCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard existingCard : existingCards) {
            if (existingCard.getCardId().equals(templateId) &&
                    (existingCard.getSection() == null ? section == null : existingCard.getSection().equals(section))) {
                // Card already exists in this section, increment quantity
                existingCard.setQuantity(existingCard.getQuantity() + 1);
                deckCardRepository.save(existingCard);
                deck.setDateModified(LocalDateTime.now());
                Deck savedDeck = deckRepository.save(deck);

                userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                        "Aggiunta 1x " + template.getName() + " al mazzo '" + deck.getName() + "'" +
                                (section != null ? " (" + section + ")" : ""));

                return savedDeck;
            }
        }

        // Card doesn't exist in this section, create new DeckCard entry
        DeckCard deckCard = new DeckCard();
        deckCard.setDeck(deck);
        deckCard.setCardId(templateId);
        deckCard.setQuantity(1);
        deckCard.setCardName(template.getName());
        deckCard.setCardImageUrl(template.getImageUrl());
        deckCard.setSection(section != null ? section : "MAIN");
        deckCard.setCondition(CardCondition.MINT); // Default condition

        // Check if user has this card in their personal collection and copy grading
        // info
        List<UserCard> userCards = userCardRepository.findByCardTemplateId(templateId)
                .stream()
                .filter(uc -> uc.getCardTemplate() != null)
                .collect(Collectors.toList());
        UserCard existingUserCard = userCards.stream()
                .filter(uc -> uc.getOwner().getId().equals(userId))
                .findFirst()
                .orElse(null);

        if (existingUserCard != null) {
            // Copy grading information from user's personal collection
            deckCard.setIsGraded(existingUserCard.getIsGraded());
            deckCard.setGradeService(existingUserCard.getGradeService());
            deckCard.setGrade(
                    existingUserCard.getGradeScore() != null ? existingUserCard.getGradeScore().toString() : null);
            deckCard.setCondition(existingUserCard.getCondition()); // Also copy condition
            deckCard.setNationality(existingUserCard.getNationality()); // Copy nationality
        } else {
            deckCard.setIsGraded(false); // Default grading status
            deckCard.setNationality(CardNationality.EN); // Default nationality
        }

        deckCardRepository.save(deckCard);
        deck.getCards().add(deckCard);
        deck.setDateModified(LocalDateTime.now());

        Deck savedDeck = deckRepository.save(deck);

        // Log deck update activity
        userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                "Aggiunta 1x " + template.getName() + " al mazzo '" + deck.getName() + "'" +
                        (section != null ? " (" + section + ")" : ""));

        // Award points for adding to wishlist (+2 points)
        if (deck.getName() != null && deck.getName().toLowerCase().contains("wishlist")) {
            rewardService.earnPoints(userId, 2, "Carta aggiunta alla wishlist: " + template.getName());
        }

        return savedDeck;
    }

    public boolean removeCardFromDeck(Long deckId, Long cardId, Long userId) {
        System.out.println("DeckService: Attempting to remove card template " + cardId + " from deck " + deckId
                + " for user " + userId);

        Deck deck = deckRepository.findById(deckId).orElse(null);
        if (deck == null) {
            System.out.println("DeckService: Deck " + deckId + " not found");
            return false;
        }

        // Verify the user owns the deck
        if (!deck.getOwnerId().equals(userId)) {
            System.out.println("DeckService: User " + userId + " does not own deck " + deckId);
            return false;
        }

        List<DeckCard> deckCards = deckCardRepository.findByDeckId(deckId);
        System.out.println("DeckService: Found " + deckCards.size() + " cards in deck " + deckId);

        for (DeckCard deckCard : deckCards) {
            System.out.println(
                    "DeckService: Checking deckCard with cardId " + deckCard.getCardId() + " against target " + cardId);
            if (deckCard.getCardId().equals(cardId)) {
                System.out.println("DeckService: Found matching deckCard with quantity " + deckCard.getQuantity());
                if (deckCard.getQuantity() > 1) {
                    // Decrement quantity instead of deleting
                    System.out.println("DeckService: Decrementing quantity from " + deckCard.getQuantity() + " to "
                            + (deckCard.getQuantity() - 1));
                    deckCard.setQuantity(deckCard.getQuantity() - 1);
                    deckCardRepository.save(deckCard);
                } else {
                    // Remove the deck card entirely
                    System.out.println("DeckService: Deleting deckCard as quantity is 1");
                    deckCardRepository.delete(deckCard);
                }

                deck.setDateModified(LocalDateTime.now());
                deckRepository.save(deck);

                // Log deck update activity
                userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                        "Rimosse 1x " + deckCard.getCardName() + " dal mazzo '" + deck.getName() + "'");

                return true;
            }
        }

        System.out
                .println("DeckService: No matching deckCard found for card template " + cardId + " in deck " + deckId);
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
        if (updateDTO.getNationality() != null) {
            deckCard.setNationality(updateDTO.getNationality());
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
                "Rimossa carta dal mazzo '" + deck.getName() + "'");

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
            List<UserCard> userCards = userCardRepository.findByCardTemplateId(cardTemplateId)
                    .stream()
                    .filter(uc -> uc.getCardTemplate() != null)
                    .collect(Collectors.toList());
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
                if (!java.util.Objects.equals(userCard.getNationality(), deckCard.getNationality())) {
                    userCard.setNationality(deckCard.getNationality());
                    hasChanges = true;
                }

                // Only save if there are actual changes
                if (hasChanges) {
                    userCardRepository.save(userCard);
                }
            }
        } catch (Exception e) {
            // Log error but don't fail the DeckCard update
            logger.error("Error syncing DeckCard to UserCard: {}", e.getMessage());
        }
    }

    /**
     * Creates starter decks for a new user based on their favorite TCG types
     * Creates "Collezione" and "Wishlist" decks for each favorite TCG
     * 
     * @param userId           The ID of the user
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

            logger.info("Created collection deck for user {}: {}", userId, collectionDeck.getName());

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

            logger.info("Created wishlist deck for user {}: {}", userId, wishlistDeck.getName());
        }
    }

    /**
     * Toggle the hidden status of a deck.
     * Hidden decks are not visible on public profiles - useful for competitive
     * players
     * who want to keep their strategies secret.
     */
    public Optional<Deck> toggleDeckHidden(Long deckId, Boolean isHidden, Long userId) {
        return deckRepository.findById(deckId).map(deck -> {
            // Verify ownership
            if (!deck.getOwnerId().equals(userId)) {
                throw new SecurityException("User not authorized to modify this deck");
            }

            deck.setIsHidden(isHidden);
            deck.setDateModified(LocalDateTime.now());
            Deck updatedDeck = deckRepository.save(deck);

            String action = isHidden ? "nascosto" : "reso visibile";
            userActivityService.logActivity(userId, ActivityType.DECK_UPDATED,
                    "Mazzo '" + deck.getName() + "' " + action + " dal profilo pubblico");

            return updatedDeck;
        });
    }

    /**
     * Get public decks for a user profile, excluding hidden decks.
     */
    public List<Deck> getPublicDecksForProfile(Long userId) {
        return deckRepository.findByOwnerIdAndIsHiddenFalseOrderByDateCreatedDesc(userId);
    }

    /**
     * Toggles the like status for a deck by a user.
     * Returns true if liked, false if unliked.
     */
    public boolean toggleLike(Long deckId, Long userId) {
        Deck deck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Deck not found"));

        Optional<DeckLike> existingLike = deckLikeRepository.findByDeckIdAndUserId(deckId, userId);

        if (existingLike.isPresent()) {
            deckLikeRepository.delete(existingLike.get());
            if (deck.getLikes() > 0) {
                deck.setLikes(deck.getLikes() - 1);
                deckRepository.save(deck);
            }
            return false;
        } else {
            DeckLike newLike = new DeckLike(deckId, userId);
            deckLikeRepository.save(newLike);

            deck.setLikes(deck.getLikes() + 1);
            deckRepository.save(deck);

            // Send notification to deck owner
            // Find liker name
            userRepository.findById(userId).ifPresent(liker -> {
                notificationService.sendDeckLikeNotification(deck.getOwnerId(), deck.getName(), liker.getUsername());
            });

            // Log activity
            userActivityService.logActivity(userId, ActivityType.DECK_LIKED,
                    "Messo mi piace al mazzo '" + deck.getName() + "'");

            return true;
        }
    }

    public long getLikeCount(Long deckId) {
        return deckLikeRepository.countByDeckId(deckId);
    }

    public boolean isLikedBy(Long deckId, Long userId) {
        return deckLikeRepository.existsByDeckIdAndUserId(deckId, userId);
    }

    public Deck duplicateDeck(Long deckId, String newName, Long userId) {
        Deck sourceDeck = deckRepository.findById(deckId)
                .orElseThrow(() -> new RuntimeException("Source deck not found"));

        Deck newDeck = new Deck();
        newDeck.setName(newName);
        newDeck.setDescription(sourceDeck.getDescription());
        newDeck.setTcgType(sourceDeck.getTcgType());
        newDeck.setDeckType(sourceDeck.getDeckType());
        newDeck.setOwnerId(userId);
        newDeck.setDateCreated(LocalDateTime.now());
        newDeck.setDateModified(LocalDateTime.now());
        newDeck.setIsPublic(false);
        newDeck.setIsHidden(sourceDeck.getIsHidden());
        if (sourceDeck.getTags() != null) {
            newDeck.setTags(new java.util.ArrayList<>(sourceDeck.getTags()));
        }

        Deck savedDeck = deckRepository.save(newDeck);

        List<DeckCard> sourceCards = deckCardRepository.findByDeckId(deckId);
        for (DeckCard sourceCard : sourceCards) {
            DeckCard newCard = new DeckCard();
            newCard.setDeck(savedDeck);
            newCard.setCardId(sourceCard.getCardId());
            newCard.setQuantity(sourceCard.getQuantity());
            newCard.setCardName(sourceCard.getCardName());
            newCard.setCardImageUrl(sourceCard.getCardImageUrl());
            newCard.setSection(sourceCard.getSection());
            newCard.setCondition(sourceCard.getCondition());
            newCard.setIsGraded(sourceCard.getIsGraded());
            newCard.setGradeService(sourceCard.getGradeService());
            newCard.setGrade(sourceCard.getGrade());
            newCard.setCertificateNumber(sourceCard.getCertificateNumber());
            newCard.setNationality(sourceCard.getNationality());
            deckCardRepository.save(newCard);
        }

        userActivityService.logActivity(userId, ActivityType.DECK_CREATED,
                "Duplicato mazzo '" + sourceDeck.getName() + "' come '" + newName + "'");

        // Trigger deck import notification if duplicating someone else's deck
        if (!userId.equals(sourceDeck.getOwnerId())) {
            userRepository.findById(userId).ifPresent(importer -> {
                notificationService.sendDeckImportNotification(sourceDeck.getOwnerId(), sourceDeck.getName(),
                        importer.getUsername());
            });
        }

        return savedDeck;
    }

    /**
     * Get popular decks (top 10 by likes), optionally filtered by TCG.
     * Only includes public, non-hidden decks.
     */
    public List<Deck> getPopularDecks(Optional<TCGType> tcgType) {
        if (tcgType.isPresent()) {
            return deckRepository.findTop10ByTcgTypeAndIsPublicTrueAndIsHiddenFalseOrderByLikesDesc(tcgType.get());
        } else {
            return deckRepository.findTop10ByIsPublicTrueAndIsHiddenFalseOrderByLikesDesc();
        }
    }
}
