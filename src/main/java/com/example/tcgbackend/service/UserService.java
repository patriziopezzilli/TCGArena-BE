package com.example.tcgbackend.service;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.User;
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

    public List<User> getAllUsers() {
        return userRepository.findAll();
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
        // Create default "My Collection" deck
        Deck defaultDeck = new Deck();
        defaultDeck.setName("My Collection");
        defaultDeck.setDescription("Default collection deck for " + savedUser.getUsername());
        defaultDeck.setOwnerId(savedUser.getId());
        defaultDeck.setIsPublic(true);
        defaultDeck.setDateCreated(LocalDateTime.now());
        defaultDeck.setDateModified(LocalDateTime.now());
        if (savedUser.getFavoriteGame() != null) {
            defaultDeck.setTcgType(savedUser.getFavoriteGame());
        }
        deckService.saveDeck(defaultDeck);
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
            return userRepository.save(user);
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