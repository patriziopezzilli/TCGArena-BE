package com.tcg.arena.repository;

import com.tcg.arena.model.TCGSet;
import com.tcg.arena.model.TCGType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TCGSetRepository extends JpaRepository<TCGSet, Long> {
    Optional<TCGSet> findBySetCode(String setCode);
    List<TCGSet> findAllByOrderByReleaseDateDesc();

    @Query("SELECT s.setCode FROM TCGSet s WHERE s.expansion.tcgType = :tcgType")
    Set<String> findAllSetCodesByTcgType(@Param("tcgType") TCGType tcgType);
}