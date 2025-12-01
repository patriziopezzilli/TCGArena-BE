package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.ProDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProDeckRepository extends JpaRepository<ProDeck, Long> {

    List<ProDeck> findTop10ByOrderByCreatedAtDesc();
}