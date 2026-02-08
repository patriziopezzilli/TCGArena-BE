package com.tcg.arena.repository;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserRatingStreak;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRatingStreakRepository extends JpaRepository<UserRatingStreak, Long> {
    Optional<UserRatingStreak> findByUser(User user);

    Optional<UserRatingStreak> findByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM UserRatingStreak s JOIN FETCH s.user u ORDER BY s.totalVotes DESC")
    org.springframework.data.domain.Page<UserRatingStreak> findAllByOrderByTotalVotesDesc(
            org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM UserRatingStreak s JOIN FETCH s.user u ORDER BY s.currentStreak DESC")
    org.springframework.data.domain.Page<UserRatingStreak> findAllByOrderByCurrentStreakDesc(
            org.springframework.data.domain.Pageable pageable);
}
