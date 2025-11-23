package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.UserStats;
import com.example.tcgbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserStatsRepository extends JpaRepository<UserStats, Long> {
    Optional<UserStats> findByUser(User user);

    Optional<UserStats> findByUserId(Long userId);

    @Query("SELECT us FROM UserStats us ORDER BY us.totalWins DESC, us.winRate DESC")
    List<UserStats> findTopPlayers();

    @Query("SELECT us FROM UserStats us WHERE us.totalTournaments > 0 ORDER BY us.winRate DESC, us.totalWins DESC")
    List<UserStats> findActivePlayersByWinRate();

    @Query("SELECT us FROM UserStats us ORDER BY us.totalWins DESC, us.winRate DESC")
    List<UserStats> findTopPlayers(@Param("limit") int limit);

    @Query("SELECT us FROM UserStats us WHERE us.totalTournaments > 0 ORDER BY us.winRate DESC, us.totalWins DESC")
    List<UserStats> findActivePlayersByWinRate(@Param("limit") int limit);
}