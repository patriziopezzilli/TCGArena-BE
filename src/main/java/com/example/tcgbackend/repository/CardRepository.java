package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.Card;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CardRepository extends JpaRepository<Card, Long> {
    List<Card> findByOwnerId(Long ownerId);

    @Query("SELECT c FROM Card c JOIN c.cardTemplate ct WHERE ct.tcgType = :tcgType")
    List<Card> findByTcgType(@Param("tcgType") TCGType tcgType);

    @Query("SELECT c FROM Card c JOIN c.cardTemplate ct WHERE ct.setCode = :setCode")
    List<Card> findBySetCode(@Param("setCode") String setCode);

    @Query("SELECT c FROM Card c JOIN c.cardTemplate ct WHERE c.ownerId = :ownerId AND ct.tcgType = :tcgType")
    List<Card> findByOwnerIdAndTcgType(@Param("ownerId") Long ownerId, @Param("tcgType") TCGType tcgType);

    @Query("SELECT c FROM Card c WHERE c.ownerId = :ownerId AND c.deckId IS NULL")
    List<Card> findUnassignedCardsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Card c JOIN c.cardTemplate ct WHERE ct.name = :name AND ct.setCode = :setCode AND ct.cardNumber = :cardNumber")
    List<Card> findByNameAndSetCodeAndCardNumber(@Param("name") String name, @Param("setCode") String setCode, @Param("cardNumber") String cardNumber);
}