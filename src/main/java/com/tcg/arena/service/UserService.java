package com.tcg.arena.service;

import com.tcg.arena.config.CacheConfig;
import com.tcg.arena.dto.UserWithStatsDTO;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserStats;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DeckService deckService;

    @Autowired
    private UserActivityService userActivityService;

    @Autowired
    private UserStatsService userStatsService;

    @Autowired
    private com.tcg.arena.repository.UserAppreciationRepository userAppreciationRepository;

    @Autowired
    private NotificationService notificationService;

    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByDateJoinedDesc();
    }

    /**
     * Get all users with their stats embedded for display in lists/leaderboards
     */
    public List<UserWithStatsDTO> getAllUsersWithStats() {
        List<User> users = userRepository.findAllByOrderByDateJoinedDesc();
        return users.stream()
                .map(user -> {
                    UserStats stats = userStatsService.getOrCreateUserStats(user);
                    return UserWithStatsDTO.fromUserAndStats(user, stats);
                })
                .collect(Collectors.toList());
    }

    @Cacheable(value = CacheConfig.LEADERBOARD_CACHE, key = "'points'")
    public List<User> getLeaderboard() {
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .limit(50)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Get leaderboard with stats - sorted by points, includes full stats
     */
    @Cacheable(value = CacheConfig.LEADERBOARD_CACHE, key = "'pointsWithStats'")
    public List<UserWithStatsDTO> getLeaderboardWithStats() {
        return userRepository.findAll().stream()
                .sorted((u1, u2) -> Integer.compare(u2.getPoints(), u1.getPoints()))
                .limit(50)
                .map(user -> {
                    UserStats stats = userStatsService.getOrCreateUserStats(user);
                    return UserWithStatsDTO.fromUserAndStats(user, stats);
                })
                .collect(Collectors.toList());
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

    public boolean isUsernameAvailable(String username) {
        return !userRepository.findByUsername(username).isPresent();
    }

    public User saveUser(User user) {
        // Set default values
        if (user.getIsPremium() == null) {
            user.setIsPremium(false);
        }
        if (user.getIsMerchant() == null) {
            user.setIsMerchant(false);
        }
        // Check if new user
        boolean isNewUser = user.getId() == null;

        // Save the user first
        User savedUser = userRepository.save(user);

        // Log user registration activity only for new users
        if (isNewUser) {
            userActivityService.logActivity(savedUser.getId(),
                    com.tcg.arena.model.ActivityType.USER_REGISTERED,
                    "Si è iscritto a TCG Arena");

            // Monitor: Notify Patrizio for new registrations
            userRepository.findByEmail("patriziopezzilli@gmail.com").ifPresent(patrizio -> {
                if (!patrizio.getId().equals(savedUser.getId())) {
                    notificationService.sendPushNotification(
                            patrizio.getId(),
                            "Nuovo Utente Registrato 👤",
                            savedUser.getUsername() + " si è appena iscritto!");
                }
            });
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
                    com.tcg.arena.model.ActivityType.USER_PROFILE_UPDATED,
                    "Aggiornato profilo");

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

    /**
     * Toggles appreciation for a user.
     * Returns true if appreciated, false if un-appreciated.
     */
    public boolean toggleAppreciation(Long targetUserId, Long likerUserId) {
        if (targetUserId.equals(likerUserId)) {
            throw new IllegalArgumentException("Users cannot appreciate themselves");
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        UserStats stats = userStatsService.getOrCreateUserStats(targetUser);

        Optional<com.tcg.arena.model.UserAppreciation> existingAppreciation = userAppreciationRepository
                .findByTargetUserIdAndLikerUserId(targetUserId, likerUserId);

        if (existingAppreciation.isPresent()) {
            userAppreciationRepository.delete(existingAppreciation.get());
            if (stats.getAppreciationCount() > 0) {
                stats.setAppreciationCount(stats.getAppreciationCount() - 1);
                userStatsService.saveUserStats(stats);
            }
            return false;
        } else {
            com.tcg.arena.model.UserAppreciation newAppreciation = new com.tcg.arena.model.UserAppreciation(
                    targetUserId, likerUserId);
            userAppreciationRepository.save(newAppreciation); // Removed cast, direct instantiation

            stats.setAppreciationCount(stats.getAppreciationCount() + 1);
            userStatsService.saveUserStats(stats);

            // Send notification
            userRepository.findById(likerUserId).ifPresent(liker -> {
                notificationService.sendProfileAppreciationNotification(targetUserId, liker.getUsername());
            });

            // Log activity
            userActivityService.logActivity(likerUserId, com.tcg.arena.model.ActivityType.PROFILE_APPRECIATED,
                    "Apprezzato il profilo di " + targetUser.getUsername());

            return true;
        }
    }

    public boolean isAppreciatedBy(Long targetUserId, Long likerUserId) {
        return userAppreciationRepository.existsByTargetUserIdAndLikerUserId(targetUserId, likerUserId);
    }
}