package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.ProDeck;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProDeckRepository extends JpaRepository<ProDeck, Long> {

    List<ProDeck> findByTcgType(TCGType tcgType);

    List<ProDeck> findByAuthor(String author);

    List<ProDeck> findByTournament(String tournament);

    List<ProDeck> findTop10ByOrderByCreatedAtDesc();
}