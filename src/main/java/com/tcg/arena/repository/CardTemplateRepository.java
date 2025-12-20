package com.tcg.arena.repository;

import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, Long> {

        // Filter condition to exclude N/A card numbers
        String EXCLUDE_NA_CONDITION = "c.cardNumber IS NOT NULL AND c.cardNumber <> 'N/A' AND c.cardNumber <> ''";

        @Query("SELECT c FROM CardTemplate c WHERE c.tcgType = :tcgType AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByTcgType(@Param("tcgType") String tcgType);

        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT c FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findByExpansionId(@Param("expansionId") Long expansionId, Pageable pageable);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        long countBySetCode(@Param("setCode") String setCode);

        @Query("SELECT COUNT(c) FROM CardTemplate c WHERE c.expansion.id = :expansionId AND " + EXCLUDE_NA_CONDITION)
        long countByExpansionId(@Param("expansionId") Long expansionId);

        @Query("SELECT c FROM CardTemplate c WHERE c.rarity = :rarity AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByRarity(@Param("rarity") String rarity);

        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findBySetCode(@Param("setCode") String setCode);

        @Query("SELECT c FROM CardTemplate c WHERE c.setCode = :setCode AND " + EXCLUDE_NA_CONDITION)
        Page<CardTemplate> findBySetCode(@Param("setCode") String setCode, Pageable pageable);

        @Query("SELECT c FROM CardTemplate c WHERE c.name = :name AND c.setCode = :setCode AND c.cardNumber = :cardNumber AND "
                        + EXCLUDE_NA_CONDITION)
        List<CardTemplate> findByNameAndSetCodeAndCardNumber(@Param("name") String name,
                        @Param("setCode") String setCode, @Param("cardNumber") String cardNumber);

        @Query("SELECT c FROM CardTemplate c WHERE " +
                        "c.name LIKE CONCAT('%', :name, '%') AND " +
                        "(c.cardNumber = :cardNumber OR c.cardNumber LIKE CONCAT(:cardNumber, '/%')) AND " +
                        EXCLUDE_NA_CONDITION)
        List<CardTemplate> searchByNameAndCardNumber(@Param("name") String name,
                        @Param("cardNumber") String cardNumber);

        @Query("SELECT c FROM CardTemplate c WHERE " +
                        "(c.name LIKE CONCAT('%', :query, '%') OR " +
                        "c.setCode LIKE CONCAT('%', :query, '%')) AND " +
                        EXCLUDE_NA_CONDITION)
        List<CardTemplate> searchByNameOrSetCode(@Param("query") String query);

        @Query(value = "SELECT * FROM card_templates ct WHERE " +
                        "(:tcgType IS NULL OR ct.tcg_type = :tcgType) AND " +
                        "(:expansionId IS NULL OR ct.expansion_id = :expansionId) AND " +
                        "(:setCode IS NULL OR ct.set_code = :setCode) AND " +
                        "(:rarity IS NULL OR ct.rarity = :rarity) AND " +
                        "(:searchQuery IS NULL OR ct.name LIKE CONCAT('%', :searchQuery, '%')) AND " +
                        "ct.card_number IS NOT NULL AND ct.card_number <> 'N/A' AND ct.card_number <> '' " +
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
}