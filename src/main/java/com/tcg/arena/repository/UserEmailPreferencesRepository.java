package com.tcg.arena.repository;

import com.tcg.arena.model.User;
import com.tcg.arena.model.UserEmailPreferences;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserEmailPreferencesRepository extends JpaRepository<UserEmailPreferences, Long> {
    Optional<UserEmailPreferences> findByUserId(Long userId);
    Optional<UserEmailPreferences> findByUser(User user);
}
