package com.tcg.arena.repository;

import com.tcg.arena.model.UserCardDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCardDeckRepository extends JpaRepository<UserCardDeck, Long> {

    List<UserCardDeck> findByUserCardId(Long userCardId);

    List<UserCardDeck> findByDeckId(Long deckId);

    Optional<UserCardDeck> findByUserCardIdAndDeckId(Long userCardId, Long deckId);

    boolean existsByUserCardIdAndDeckId(Long userCardId, Long deckId);

    void deleteByUserCardIdAndDeckId(Long userCardId, Long deckId);

    void deleteByDeckId(Long deckId);

    void deleteByUserCardId(Long userCardId);
}
