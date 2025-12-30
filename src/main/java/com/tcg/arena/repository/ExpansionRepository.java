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

    /**
     * Find expansions that have at least one set released in the given year(s)
     */
    @Query("SELECT DISTINCT e FROM Expansion e LEFT JOIN FETCH e.sets s WHERE YEAR(s.releaseDate) IN :years")
    List<Expansion> findAllWithSetsByYears(@Param("years") List<Integer> years);

    /**
     * Find expansions that have at least one set released in the given year
     */
    @Query("SELECT DISTINCT e FROM Expansion e LEFT JOIN FETCH e.sets s WHERE YEAR(s.releaseDate) = :year")
    List<Expansion> findAllWithSetsByYear(@Param("year") int year);

    /**
     * Search expansions by title (case-insensitive), ordered by most recent first
     */
    @Query("SELECT DISTINCT e FROM Expansion e LEFT JOIN FETCH e.sets s WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%')) ORDER BY e.id DESC")
    List<Expansion> searchByTitle(@Param("query") String query);
}