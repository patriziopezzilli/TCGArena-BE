package com.tcg.arena.repository;

import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    List<User> findAllByOrderByPointsDesc();

    List<User> findAllByOrderByDateJoinedDesc();

    // Count users registered after a given date
    long countByDateJoinedAfter(java.time.LocalDateTime since);

    // Find users registered between dates (for Welcome Message)
    List<User> findByDateJoinedBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Find users inactive since a date (e.g. 7 days ago)
    // We want users whose lastLogin is BEFORE 'cutoff' AND (optionally) AFTER
    // 'olderCutoff' to avoid spamming very old users endlessly
    // For now, let's just find users whose lastLogin is roughly X days ago.
    // Actually, "Inactivity Alert" usually runs once. We can query users whose
    // lastLogin is BEFORE X days ago.
    // To avoid spam, we should probably check if we already notified them? Or just
    // check if lastLogin is within a specific window (e.g., 7-8 days ago).
    List<User> findByLastLoginBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);

    // Also need query to find users with specific favorite TCG
    List<User> findByFavoriteTCGTypesStringContaining(String tcgType);
}