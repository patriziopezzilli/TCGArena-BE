package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaCardCondition;
import com.tcg.arena.model.ArenaCardVariant;
import com.tcg.arena.model.ArenaPrinting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaCardVariantRepository extends JpaRepository<ArenaCardVariant, String> {

    // Find all variants for a card
    List<ArenaCardVariant> findByCardId(String cardId);

    // Find variant by card and condition/printing
    Optional<ArenaCardVariant> findByCardIdAndConditionAndPrinting(
            String cardId,
            ArenaCardCondition condition,
            ArenaPrinting printing);

    // Find all Near Mint variants for a card
    List<ArenaCardVariant> findByCardIdAndCondition(String cardId, ArenaCardCondition condition);

    // Find all foil variants for a card
    List<ArenaCardVariant> findByCardIdAndPrinting(String cardId, ArenaPrinting printing);

    // Find variants with price within range
    @Query("SELECT v FROM ArenaCardVariant v WHERE v.price >= :minPrice AND v.price <= :maxPrice")
    List<ArenaCardVariant> findByPriceRange(@Param("minPrice") Double minPrice, @Param("maxPrice") Double maxPrice);

    // Count variants by card
    long countByCardId(String cardId);

    // Find most recently updated variants
    @Query("SELECT v FROM ArenaCardVariant v ORDER BY v.lastUpdated DESC")
    List<ArenaCardVariant> findRecentlyUpdated();
}
