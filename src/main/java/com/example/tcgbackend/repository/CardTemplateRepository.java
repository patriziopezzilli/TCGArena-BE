package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.TCGType;
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
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(c.setCode) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CardTemplate> searchByNameOrSetCode(@Param("query") String query);

    @Modifying
    @Query("DELETE FROM CardTemplate c WHERE c.tcgType = :tcgType")
    void deleteByTcgType(@Param("tcgType") TCGType tcgType);
}