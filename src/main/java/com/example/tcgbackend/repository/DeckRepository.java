package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.Deck;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeckRepository extends JpaRepository<Deck, Long> {
    List<Deck> findByOwnerIdOrderByDateCreatedDesc(Long ownerId);
    List<Deck> findByIsPublicTrueOrderByDateCreatedDesc();
    List<Deck> findByTcgTypeOrderByDateCreatedDesc(TCGType tcgType);
    List<Deck> findByOwnerIdAndTcgTypeOrderByDateCreatedDesc(Long ownerId, TCGType tcgType);
}