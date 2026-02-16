package com.tcg.arena.repository;

import com.tcg.arena.model.Deck;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByOwnerIdOrderByDateCreatedDesc(Long ownerId);

    List<Deck> findByIsPublicTrueOrderByDateCreatedDesc();

    List<Deck> findByTcgTypeOrderByDateCreatedDesc(TCGType tcgType);

    List<Deck> findByOwnerIdAndTcgTypeOrderByDateCreatedDesc(Long ownerId, TCGType tcgType);

    // For public profile view - exclude hidden decks
    List<Deck> findByOwnerIdAndIsHiddenFalseOrderByDateCreatedDesc(Long ownerId);

    List<Deck> findByOwnerIdAndIsPublicTrueAndIsHiddenFalseOrderByDateCreatedDesc(Long ownerId);

    // Popular decks (Top 10 most liked public & visible)
    // Popular decks (Top 10 most liked & visible, regardless of public status)
    List<Deck> findTop10ByIsHiddenFalseOrderByLikesDesc();

    List<Deck> findTop10ByTcgTypeAndIsHiddenFalseOrderByLikesDesc(TCGType tcgType);
}