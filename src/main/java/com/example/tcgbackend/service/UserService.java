package com.example.tcgbackend.service;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.User;
import com.example.tcgbackend.model.TCGType;
import com.example.tcgbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckService deckService;

    @Autowired
    private UserActivityService userActivityService;

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByDateJoinedDesc();
    }

    public List<User> getLeaderboard() {
        return userRepository.findAll().stream()
            .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
            .limit(50)
            .collect(java.util.stream.Collectors.toList());
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User saveUser(User user) {
        // Set default values
        if (user.getIsPremium() == null) {
            user.setIsPremium(false);
        }
        if (user.getIsMerchant() == null) {
            user.setIsMerchant(false);
        }
        // Save the user first
        User savedUser = userRepository.save(user);

        // Log user registration activity
        userActivityService.logActivity(savedUser.getId(),
            com.example.tcgbackend.model.ActivityType.USER_REGISTERED,
            "Joined TCG Arena");

        // Get list of favorite TCGs - use new favoriteTCGs if available, otherwise fall back to favoriteGame
        List<TCGType> favoriteTCGTypes = savedUser.getFavoriteTCGTypes();
        if (favoriteTCGTypes.isEmpty() && savedUser.getFavoriteGame() != null) {
            // Backward compatibility: if no favoriteTCGs but favoriteGame exists, use it
            favoriteTCGTypes = List.of(savedUser.getFavoriteGame());
        }

        // Create default decks for each favorite TCG
        for (TCGType tcgType : favoriteTCGTypes) {
            // Create "My Collection" deck for this TCG
            Deck defaultDeck = new Deck();
            defaultDeck.setName("My Collection");
            defaultDeck.setDescription("Default collection deck for " + savedUser.getUsername() + " - " + tcgType.name());
            defaultDeck.setOwnerId(savedUser.getId());
            defaultDeck.setIsPublic(true);
            defaultDeck.setDeckType(com.example.tcgbackend.model.DeckType.LISTA);
            defaultDeck.setTcgType(tcgType);
            defaultDeck.setDateCreated(LocalDateTime.now());
            defaultDeck.setDateModified(LocalDateTime.now());
            deckService.saveDeck(defaultDeck);

            // Create "Wishlist" deck for this TCG
            Deck wishlistDeck = new Deck();
            wishlistDeck.setName("Wishlist");
            wishlistDeck.setDescription("Wishlist deck for " + savedUser.getUsername() + " - " + tcgType.name());
            wishlistDeck.setOwnerId(savedUser.getId());
            wishlistDeck.setIsPublic(false);
            wishlistDeck.setDeckType(com.example.tcgbackend.model.DeckType.LISTA);
            wishlistDeck.setTcgType(tcgType);
            wishlistDeck.setDateCreated(LocalDateTime.now());
            wishlistDeck.setDateModified(LocalDateTime.now());
            deckService.saveDeck(wishlistDeck);
        }

        // Migrate existing decks to have default deck type
        deckService.migrateExistingDecksToDefaultType();

        return savedUser;
    }

    public Optional<User> updateUser(Long id, User userDetails) {
        return userRepository.findById(id).map(user -> {
            user.setEmail(userDetails.getEmail());
            user.setUsername(userDetails.getUsername());
            user.setDisplayName(userDetails.getDisplayName());
            user.setProfileImageUrl(userDetails.getProfileImageUrl());
            user.setIsPremium(userDetails.getIsPremium());
            user.setFavoriteGame(userDetails.getFavoriteGame());
            user.setLocation(userDetails.getLocation());
            User updatedUser = userRepository.save(user);

            // Log profile update activity
            userActivityService.logActivity(id,
                com.example.tcgbackend.model.ActivityType.USER_PROFILE_UPDATED,
                "Updated profile information");

            return updatedUser;
        });
    }

    public boolean deleteUser(Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<User> getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            return getUserByUsername(username);
        }
        return Optional.empty();
    }
}