package com.tcg.arena.service;

import com.tcg.arena.model.CardCondition;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserCard;
import com.tcg.arena.model.Deck;
import com.tcg.arena.model.DeckCard;
import com.tcg.arena.model.InventoryCard;
import com.tcg.arena.model.Shop;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.UserCardRepository;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.repository.InventoryCardRepository;
import com.tcg.arena.repository.ShopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserCardService {
    private static final Logger logger = LoggerFactory.getLogger(UserCardService.class);

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

    @Autowired
    private InventoryCardRepository inventoryCardRepository;

    @Autowired
    private ShopRepository shopRepository;

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

            // If user is a merchant, sync to shop inventory
            syncUserCardToShopInventory(savedUserCard, savedUserCard.getOwner());

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
                logger.error("Error removing card from collection deck: " + e.getMessage());
            }

            // Log activity before deletion
            userActivityService.logActivity(userCard.getOwner().getId(),
                com.tcg.arena.model.ActivityType.CARD_REMOVED_FROM_COLLECTION,
                "Rimosso " + userCard.getCardTemplate().getName() + " dall'inventario");

            // If user is a merchant, remove from shop inventory
            removeFromShopInventory(userCard);

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
            logger.error("Error adding card to collection deck: " + e.getMessage());
        }

        // Log activity
        userActivityService.logActivity(owner.getId(),
            com.tcg.arena.model.ActivityType.CARD_ADDED_TO_COLLECTION,
            "Aggiunto " + cardTemplate.getName() + " all'inventario");

        // If user is a merchant, sync to shop inventory
        syncUserCardToShopInventory(savedCard, owner);

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
            logger.error("Error syncing UserCard to collection deck: " + e.getMessage());
        }
    }

    private void syncUserCardToShopInventory(UserCard userCard, User owner) {
        try {
            // Check if user is a merchant and has a shop
            if (!owner.getIsMerchant()) {
                return;
            }

            Optional<Shop> shopOpt = shopRepository.findByOwnerId(owner.getId());
            if (shopOpt.isEmpty()) {
                return;
            }

            Shop shop = shopOpt.get();
            Long cardTemplateId = userCard.getCardTemplate().getId();
            Long shopId = shop.getId();

            // Check if card already exists in shop inventory
            List<InventoryCard> existingCards = inventoryCardRepository.findByShopIdAndCardTemplateId(shopId, cardTemplateId);
            InventoryCard inventoryCard = existingCards.stream()
                .filter(ic -> ic.getCondition() == userCard.getCondition())
                .findFirst()
                .orElse(null);

            if (inventoryCard != null) {
                // Update existing inventory card
                inventoryCard.setQuantity(inventoryCard.getQuantity() + 1);
                if (userCard.getPurchasePrice() != null && userCard.getPurchasePrice() > 0) {
                    // Use purchase price as base, merchant can adjust later
                    inventoryCard.setPrice(userCard.getPurchasePrice() * 1.2); // 20% markup
                }
                inventoryCardRepository.save(inventoryCard);
                logger.info("Updated shop inventory card quantity for card {} in shop {}", cardTemplateId, shopId);
            } else {
                // Create new inventory card
                inventoryCard = new InventoryCard();
                inventoryCard.setCardTemplateId(cardTemplateId);
                inventoryCard.setShopId(shopId);
                inventoryCard.setCondition(userCard.getCondition());
                inventoryCard.setNationality(userCard.getNationality());
                inventoryCard.setQuantity(1);
                // Set default price based on purchase price or market value
                double defaultPrice = 0.0;
                if (userCard.getPurchasePrice() != null && userCard.getPurchasePrice() > 0) {
                    defaultPrice = userCard.getPurchasePrice() * 1.2; // 20% markup
                } else {
                    // Fallback to a default price - merchant should update this
                    defaultPrice = 1.0;
                }
                inventoryCard.setPrice(defaultPrice);
                inventoryCard.setCreatedAt(LocalDateTime.now());
                inventoryCard.setUpdatedAt(LocalDateTime.now());
                inventoryCardRepository.save(inventoryCard);
                logger.info("Created new shop inventory card for card {} in shop {}", cardTemplateId, shopId);
            }
        } catch (Exception e) {
            logger.error("Error syncing UserCard to shop inventory: " + e.getMessage(), e);
        }
    }

    private void removeFromShopInventory(UserCard userCard) {
        try {
            User owner = userCard.getOwner();
            if (!owner.getIsMerchant()) {
                return;
            }

            Optional<Shop> shopOpt = shopRepository.findByOwnerId(owner.getId());
            if (shopOpt.isEmpty()) {
                return;
            }

            Shop shop = shopOpt.get();
            Long cardTemplateId = userCard.getCardTemplate().getId();
            Long shopId = shop.getId();

            // Find matching inventory card
            List<InventoryCard> existingCards = inventoryCardRepository.findByShopIdAndCardTemplateId(shopId, cardTemplateId);
            InventoryCard inventoryCard = existingCards.stream()
                .filter(ic -> ic.getCondition() == userCard.getCondition())
                .findFirst()
                .orElse(null);

            if (inventoryCard != null) {
                if (inventoryCard.getQuantity() > 1) {
                    // Decrease quantity
                    inventoryCard.setQuantity(inventoryCard.getQuantity() - 1);
                    inventoryCardRepository.save(inventoryCard);
                    logger.info("Decreased shop inventory card quantity for card {} in shop {}", cardTemplateId, shopId);
                } else {
                    // Remove card from inventory
                    inventoryCardRepository.delete(inventoryCard);
                    logger.info("Removed shop inventory card for card {} from shop {}", cardTemplateId, shopId);
                }
            }
        } catch (Exception e) {
            logger.error("Error removing card from shop inventory: " + e.getMessage(), e);
        }
    }
}