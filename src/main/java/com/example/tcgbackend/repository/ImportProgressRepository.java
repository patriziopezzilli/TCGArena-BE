package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.ImportProgress;
import com.example.tcgbackend.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportProgressRepository extends JpaRepository<ImportProgress, Long> {
    Optional<ImportProgress> findByTcgType(TCGType tcgType);
}