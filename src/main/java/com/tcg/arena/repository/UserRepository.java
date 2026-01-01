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
}