package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByOwnerId(Long ownerId);
    List<Deck> findByIsPublicTrue();
    List<Deck> findByTcgType(TCGType tcgType);
    List<Deck> findByOwnerIdAndTcgType(Long ownerId, TCGType tcgType);
}