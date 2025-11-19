package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpansionRepository extends JpaRepository<Expansion, Long> {
    List<Expansion> findByTcgType(TCGType tcgType);
    Expansion findByTitle(String title);
}