package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaCardRepository extends JpaRepository<ArenaCard, String> {

    // Find by set
    List<ArenaCard> findBySetId(String setId);

    Page<ArenaCard> findBySetId(String setId, Pageable pageable);

    // Find by game
    @Query("SELECT c FROM ArenaCard c WHERE c.game.id = :gameId")
    Page<ArenaCard> findByGameId(@Param("gameId") String gameId, Pageable pageable);

    // Find by TCGPlayer ID
    Optional<ArenaCard> findByTcgplayerId(String tcgplayerId);

    // Find by Scryfall ID (MTG)
    Optional<ArenaCard> findByScryfallId(String scryfallId);

    // Find by MTGJSON ID (MTG)
    Optional<ArenaCard> findByMtgjsonId(String mtgjsonId);

    // Search by name
    @Query("SELECT c FROM ArenaCard c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ArenaCard> searchByName(@Param("query") String query, Pageable pageable);

    // Search by name within a game
    @Query("SELECT c FROM ArenaCard c WHERE c.game.id = :gameId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<ArenaCard> searchByNameAndGame(@Param("query") String query, @Param("gameId") String gameId,
            Pageable pageable);

    // Count by set
    long countBySetId(String setId);

    // Count by game
    @Query("SELECT COUNT(c) FROM ArenaCard c WHERE c.game.id = :gameId")
    long countByGameId(@Param("gameId") String gameId);
}
