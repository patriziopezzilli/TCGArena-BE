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
    List<Card> findByTcgType(TCGType tcgType);
    List<Card> findBySetCode(String setCode);
    List<Card> findByOwnerIdAndTcgType(Long ownerId, TCGType tcgType);

    @Query("SELECT c FROM Card c WHERE c.ownerId = :ownerId AND c.deckId IS NULL")
    List<Card> findUnassignedCardsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT c FROM Card c WHERE c.name = :name AND c.setCode = :setCode AND c.cardNumber = :cardNumber")
    List<Card> findByNameAndSetCodeAndCardNumber(@Param("name") String name, @Param("setCode") String setCode, @Param("cardNumber") String cardNumber);
}