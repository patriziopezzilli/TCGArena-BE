package com.tcg.arena.repository;

import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, Long> {

        // Filter condition to exclude N/A card numbers and non-card products (Boxes,
        // Decks, Codes)
        String EXCLUDE_NA_CONDITION = "c.cardNumber IS NOT NULL AND c.cardNumber <> 'N/A' AND c.cardNumber <> '' " +
                        "AND LOWER(c.name) NOT LIKE '%code%' " +
                        "AND LOWER(c.name) NOT LIKE '%blister%' " +
                        "AND LOWER(c.name) NOT LIKE '%box%' " +
                        "AND LOWER(c.name) NOT LIKE '%deck%' " +
                        "AND LOWER(c.name) NOT LIKE '%decks%'";

        @Query("SELECT c FROM CardTemplate c WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByTcgType(@Param("tcgType") String tcgType);

        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findByExpansionId(@Param("expansionId") Long expansionId, Pageable pageable);

        // Methods that include cards with cardNumber = 'N/A' for expansion details
        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findAllByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findAllByExpansionId(@Param("expansionId") Long expansionId, Pageable pageable);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        long countBySetCode(@Param("setCode") String setCode);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        long countAllBySetCode(@Param("setCode") String setCode);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        long countByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        long countAllByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT c FROM CardTemplate c WHERE c.rarity = :rarity AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByRarity(@Param("rarity") String rarity);

        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findBySetCode(@Param("setCode") String setCode);

        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findBySetCode(@Param("setCode") String setCode, Pageable pageable);

        // Methods that include cards with cardNumber = 'N/A' for set details
        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findAllBySetCode(@Param("setCode") String setCode);

        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findAllBySetCode(@Param("setCode") String setCode, Pageable pageable);

        @Query("SELECT c FROM CardTemplate c WHERE c.name = :name AND c.setCode = :setCode AND c.cardNumber = :cardNumber AND "
                        + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByNameAndSetCodeAndCardNumber(@Param("name") String name,
                        @Param("setCode") String setCode, @Param("cardNumber") String cardNumber);

        /**
         * Check for existing card by composite key INCLUDING N/A card numbers.
         * Used by import process to check for duplicates before insert.
         */
        @Query("SELECT c FROM CardTemplate c WHERE c.name = :name AND c.setCode = :setCode AND c.cardNumber = :cardNumber")
        List<CardTemplate> findByNameAndSetCodeAndCardNumberIncludingNA(@Param("name") String name,
                        @Param("setCode") String setCode, @Param("cardNumber") String cardNumber);

        /**
         * Get all card composite keys (name, setCode, cardNumber) for a specific
         * setCode.
         * Used for delta import to identify missing cards.
         */
        @Query("SELECT CONCAT(c.name, '|||', c.setCode, '|||', COALESCE(c.cardNumber, '')) FROM CardTemplate c WHERE c.setCode = :setCode")
        java.util.Set<String> findAllCardKeysBySetCode(@Param("setCode") String setCode);

        @Query("SELECT c.name, c.setCode, c.cardNumber FROM CardTemplate c WHERE c.tcgType = 'MAGIC'")
        List<Object[]> findAllCardKeys();

        @Query("SELECT c FROM CardTemplate c WHERE " +
                        "c.name LIKE CONCAT('%', :name, '%') AND " +
                        "(c.cardNumber = :cardNumber OR c.cardNumber LIKE CONCAT(:cardNumber, '/%')) AND " +
                        EXCLUDE_NA_CONDITION)
        List<CardTemplate> searchByNameAndCardNumber(@Param("name") String name,
                        @Param("cardNumber") String cardNumber);

        @Query("SELECT c FROM CardTemplate c WHERE " +
                        "(c.name LIKE CONCAT('%', :query, '%') OR " +
                        "c.setCode LIKE CONCAT('%', :query, '%') OR " +
                        "c.cardNumber LIKE CONCAT('%', :query, '%')) AND " +
                        EXCLUDE_NA_CONDITION)
        List<CardTemplate> searchByNameOrSetCode(@Param("query") String query);

        @Query(value = "SELECT * FROM card_templates ct WHERE " +
                        "(:tcgType IS NULL OR ct.tcg_type = :tcgType) AND " +
                        "(:expansionId IS NULL OR ct.expansion_id = :expansionId) AND " +
                        "(:setCode IS NULL OR ct.set_code = :setCode) AND " +
                        "(:rarity IS NULL OR ct.rarity = :rarity) AND " +
                        "(:searchQuery IS NULL OR LOWER(ct.name) LIKE LOWER('%' || :searchQuery || '%') OR LOWER(ct.card_number) LIKE LOWER('%' || :searchQuery || '%')) AND "
                        +
                        "ct.card_number IS NOT NULL AND ct.card_number <> 'N/A' AND ct.card_number <> '' " +
                        "AND LOWER(ct.name) NOT LIKE '%code%' " +
                        "AND LOWER(ct.name) NOT LIKE '%blister%' " +
                        "AND LOWER(ct.name) NOT LIKE '%box%' " +
                        "AND LOWER(ct.name) NOT LIKE '%deck%' " +
                        "AND LOWER(ct.name) NOT LIKE '%decks%' " +
                        "ORDER BY ct.id", nativeQuery = true)
        Page<CardTemplate> findWithFilters(
                        @Param("tcgType") String tcgType,
                        @Param("expansionId") Long expansionId,
                        @Param("setCode") String setCode,
                        @Param("rarity") String rarity,
                        @Param("searchQuery") String searchQuery,
                        Pageable pageable);

        @Modifying
        @Query("DELETE FROM CardTemplate c WHERE c.tcgType = :tcgType")
        void deleteByTcgType(@Param("tcgType") TCGType tcgType);

        /**
         * Find card templates for bulk import - matches by name (LIKE), setCode
         * (exact), and tcgType (exact)
         */
        @Query("SELECT c FROM CardTemplate c WHERE " +
                        "LOWER(c.name) LIKE LOWER(CONCAT('%', :cardName, '%')) AND " +
                        "c.setCode = :setCode AND " +
                        "c.tcgType = :tcgType AND " +
                        EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByNameLikeAndSetCodeAndTcgType(
                        @Param("cardName") String cardName,
                        @Param("setCode") String setCode,
                        @Param("tcgType") String tcgType);

        /**
         * OPTIMIZED: Get all card counts grouped by setCode in a single query
         * Returns a list of Object[] where [0] = setCode (String), [1] = count (Long)
         */
        @Query("SELECT c.setCode, COUNT(c) FROM CardTemplate c " +
                        "WHERE " + EXCLUDE_NA_CONDITION + " " +
                        "GROUP BY c.setCode")
        List<Object[]> countAllGroupedBySetCode();

        /**
         * OPTIMIZED: Get all card counts grouped by expansionId in a single query
         * Returns a list of Object[] where [0] = expansionId (Long), [1] = count (Long)
         */
        @Query("SELECT c.expansion.id, COUNT(c) FROM CardTemplate c " +
                        "WHERE c.expansion IS NOT NULL AND " + EXCLUDE_NA_CONDITION + " " +
                        "GROUP BY c.expansion.id")
        List<Object[]> countAllGroupedByExpansionId();

        /**
         * OPTIMIZED: Get particular TCG card counts grouped by setCode
         * Returns a list of Object[] where [0] = setCode (String), [1] = count (Long)
         */
        @Query("SELECT c.setCode, COUNT(c) FROM CardTemplate c " +
                        "WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION + " " +
                        "GROUP BY c.setCode")
        List<Object[]> countByTcgTypeGroupedBySetCode(@Param("tcgType") TCGType tcgType);

        /**
         * Standard JPA query for case-insensitive name search
         */
        List<CardTemplate> findByNameContainingIgnoreCase(String name);

        /**
         * Smart Scan: Find cards where name fits tokens or card number matches tokens
         */
        @Query(value = "SELECT * FROM card_templates ct WHERE " +
                        "(ct.name IN :tokens) OR " +
                        "(LOWER(ct.name) LIKE LOWER('%' || :longToken || '%')) " +
                        "AND ct.card_number IS NOT NULL AND ct.card_number <> 'N/A' AND ct.card_number <> '' " +
                        "LIMIT 20", nativeQuery = true)
        List<CardTemplate> findBySmartScanTokens(@Param("tokens") List<String> tokens,
                        @Param("longToken") String longToken);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION)
        long countByTcgType(@Param("tcgType") TCGType tcgType);

        /**
         * Get random card templates from the last N years
         * Uses native SQL for RANDOM() function and date filtering
         */
        @Query(value = "SELECT * FROM card_templates ct WHERE " +
                        "ct.date_created >= NOW() - MAKE_INTERVAL(YEARS => :years) AND " +
                        "ct.card_number IS NOT NULL AND ct.card_number <> 'N/A' AND ct.card_number <> '' " +
                        "AND LOWER(ct.name) NOT LIKE '%code%' " +
                        "AND LOWER(ct.name) NOT LIKE '%blister%' " +
                        "AND LOWER(ct.name) NOT LIKE '%box%' " +
                        "AND LOWER(ct.name) NOT LIKE '%deck%' " +
                        "AND LOWER(ct.name) NOT LIKE '%decks%' " +
                        "ORDER BY RANDOM() " +
                        "LIMIT :limit", nativeQuery = true)
        List<CardTemplate> findRandomRecentCards(@Param("years") int years, @Param("limit") int limit);

        // Card Rating Arena - Rankings by likes/dislikes
        @Query("SELECT c FROM CardTemplate c WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION
                        + " ORDER BY c.likesCount DESC")
        Page<CardTemplate> findByTcgTypeOrderByLikesCountDesc(@Param("tcgType") TCGType tcgType, Pageable pageable);

        @Query("SELECT c FROM CardTemplate c WHERE " + EXCLUDE_NA_CONDITION + " ORDER BY c.likesCount DESC")
        Page<CardTemplate> findAllByOrderByLikesCountDesc(Pageable pageable);

        @Query("SELECT c FROM CardTemplate c WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION
                        + " ORDER BY c.dislikesCount DESC")
        Page<CardTemplate> findByTcgTypeOrderByDislikesCountDesc(@Param("tcgType") TCGType tcgType, Pageable pageable);

        @Query("SELECT c FROM CardTemplate c WHERE " + EXCLUDE_NA_CONDITION + " ORDER BY c.dislikesCount DESC")
        Page<CardTemplate> findAllByOrderByDislikesCountDesc(Pageable pageable);

        @Modifying
        @Transactional
        @Query("DELETE FROM CardTemplate c WHERE c.setCode = :setCode")
        int deleteBySetCode(@Param("setCode") String setCode);
}