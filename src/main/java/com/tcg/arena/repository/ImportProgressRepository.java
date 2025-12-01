package com.tcg.arena.repository;

import com.tcg.arena.model.ImportProgress;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportProgressRepository extends JpaRepository<ImportProgress, Long> {
    Optional<ImportProgress> findByTcgType(TCGType tcgType);
}