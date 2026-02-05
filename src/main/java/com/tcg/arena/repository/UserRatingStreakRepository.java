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
}
