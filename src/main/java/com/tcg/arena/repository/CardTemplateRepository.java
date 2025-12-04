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
    List<CardTemplate> findByTcgType(String tcgType);

    List<CardTemplate> findByExpansionId(Long expansionId);

    List<CardTemplate> findByRarity(String rarity);

    List<CardTemplate> findBySetCode(String setCode);

    Page<CardTemplate> findBySetCode(String setCode, Pageable pageable);

    List<CardTemplate> findByNameAndSetCodeAndCardNumber(String name, String setCode, String cardNumber);

    @Query("SELECT c FROM CardTemplate c WHERE " +
            "c.name LIKE CONCAT('%', :query, '%') OR " +
            "c.setCode LIKE CONCAT('%', :query, '%')")
    List<CardTemplate> searchByNameOrSetCode(@Param("query") String query);

    @Query(value = "SELECT * FROM card_templates ct WHERE " +
           "(:tcgType IS NULL OR ct.tcg_type = :tcgType) AND " +
           "(:expansionId IS NULL OR ct.expansion_id = :expansionId) AND " +
           "(:setCode IS NULL OR ct.set_code = :setCode) AND " +
           "(:rarity IS NULL OR ct.rarity = :rarity) AND " +
           "(:searchQuery IS NULL OR ct.name LIKE CONCAT('%', :searchQuery, '%')) " +
           "ORDER BY ct.id",
           nativeQuery = true)
    Page<CardTemplate> findWithFilters(
        @Param("tcgType") String tcgType,
        @Param("expansionId") Long expansionId,
        @Param("setCode") String setCode,
        @Param("rarity") String rarity,
        @Param("searchQuery") String searchQuery,
        Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM CardTemplate c WHERE c.tcgType = :tcgType")
    void deleteByTcgType(@Param("tcgType") TCGType tcgType);
}