package com.tcg.arena.service;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserStats;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.Deck;
import com.tcg.arena.repository.UserStatsRepository;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.TournamentParticipantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserStatsService {

    @Autowired
    private UserStatsRepository userStatsRepository;


    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private TournamentParticipantRepository tournamentParticipantRepository;

    @Cacheable(value = "userStats", key = "#user.id")
    public UserStats getOrCreateUserStats(User user) {
        Optional<UserStats> existingStats = userStatsRepository.findByUser(user);
        if (existingStats.isPresent()) {
            UserStats stats = existingStats.get();
            // Check if stats need refresh (older than 1 hour)
            if (stats.getLastActivity().isBefore(LocalDateTime.now().minusHours(1))) {
                return updateUserStats(stats);
            }
            return stats;
        } else {
            return createUserStats(user);
        }
    }

    private UserStats createUserStats(User user) {
        UserStats stats = new UserStats();
        stats.setUser(user);
        stats.setJoinDate(user.getDateJoined());
        return updateUserStats(stats);
    }

    private UserStats updateUserStats(UserStats stats) {
        User user = stats.getUser();


        // Calculate total cards in all decks (sum of DeckCard quantities)
        List<Deck> userDecks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(user.getId());
        int totalCards = userDecks.stream()
            .flatMap(deck -> deck.getCards().stream())
            .mapToInt(deckCard -> deckCard.getQuantity() != null ? deckCard.getQuantity() : 0)
            .sum();
        stats.setTotalCards(totalCards);

        // Calculate total decks
        int totalDecks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(user.getId()).size();
        stats.setTotalDecks(totalDecks);

        // Calculate tournament stats
        var tournamentStats = calculateTournamentStats(user.getId());
        stats.setTotalTournaments(tournamentStats.get("total"));
        stats.setTotalWins(tournamentStats.get("wins"));
        stats.setTotalLosses(tournamentStats.get("losses"));

        // Calculate win rate
        int totalGames = stats.getTotalWins() + stats.getTotalLosses();
        double winRate = totalGames > 0 ? (double) stats.getTotalWins() / totalGames : 0.0;
        stats.setWinRate(winRate);

        // Determine favorite TCG type
        TCGType favoriteType = determineFavoriteTCGType(user);
        stats.setFavoriteTCGType(favoriteType);

        // Update last activity
        stats.setLastActivity(LocalDateTime.now());

        return userStatsRepository.save(stats);
    }

    private Map<String, Integer> calculateTournamentStats(Long userId) {
        var participants = tournamentParticipantRepository.findByUserId(userId);

        int total = participants.size();
        int wins = (int) participants.stream()
                .filter(p -> p.getPlacement() != null && p.getPlacement() == 1)
                .count();
        int losses = total - wins; // All tournaments except wins are losses

        return Map.of("total", total, "wins", wins, "losses", losses);
    }

    private TCGType determineFavoriteTCGType(User user) {
        // Count decks by TCG type
        var decks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(user.getId());
        var typeCount = decks.stream()
                .filter(d -> d.getTcgType() != null)
                .collect(Collectors.groupingBy(Deck::getTcgType, Collectors.counting()));

        if (typeCount.isEmpty()) {
            return user.getFavoriteGame(); // fallback to user's preference
        }

        // Return the most common TCG type
        return typeCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(user.getFavoriteGame());
    }

    @Cacheable(value = "leaderboard", key = "'overall_' + #limit")
    public List<UserStats> getLeaderboard(int limit) {
        return userStatsRepository.findTopPlayers(limit);
    }

    @Cacheable(value = "leaderboard", key = "'active_' + #limit")
    public List<UserStats> getActivePlayersLeaderboard(int limit) {
        return userStatsRepository.findActivePlayersByWinRate(limit);
    }

    @Cacheable(value = "leaderboard", key = "'collection_' + #limit")
    public List<UserStats> getTopCollectors(int limit) {
        return userStatsRepository.findTopCollectors(limit);
    }

    @Cacheable(value = "leaderboard", key = "'tournament_' + #limit")
    public List<UserStats> getTopTournamentPlayers(int limit) {
        return userStatsRepository.findTopTournamentPlayers(limit);
    }

    @CacheEvict(value = "leaderboard", allEntries = true)
    public void invalidateAllLeaderboards() {
        // Force cache invalidation for all leaderboards
    }

    public Optional<UserStats> getUserStats(Long userId) {
        return userStatsRepository.findByUserId(userId);
    }

    @CacheEvict(value = "userStats", key = "#user.id")
    public void refreshUserStats(User user) {
        getOrCreateUserStats(user);
    }

    @CacheEvict(value = "userStats", key = "#user.id")
    public void invalidateUserStats(User user) {
        // Force cache invalidation
    }
}