package com.tcg.arena.repository;

import com.tcg.arena.model.DeckLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeckLikeRepository extends JpaRepository<DeckLike, Long> {
    Optional<DeckLike> findByDeckIdAndUserId(Long deckId, Long userId);

    long countByDeckId(Long deckId);

    boolean existsByDeckIdAndUserId(Long deckId, Long userId);
}
