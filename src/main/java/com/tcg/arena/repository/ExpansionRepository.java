package com.tcg.arena.repository;

import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpansionRepository extends JpaRepository<Expansion, Long> {
    List<Expansion> findByTcgType(TCGType tcgType);

    Expansion findByTitle(String title);

    @Query("SELECT e FROM Expansion e LEFT JOIN FETCH e.sets WHERE e.id = :id")
    Optional<Expansion> findByIdWithSets(@Param("id") Long id);

    @Query("SELECT DISTINCT e FROM Expansion e LEFT JOIN FETCH e.sets")
    List<Expansion> findAllWithSets();

    @Query("SELECT e FROM Expansion e LEFT JOIN e.sets s GROUP BY e ORDER BY MAX(s.releaseDate) DESC")
    List<Expansion> findAllByOrderByReleaseDateDesc();
}