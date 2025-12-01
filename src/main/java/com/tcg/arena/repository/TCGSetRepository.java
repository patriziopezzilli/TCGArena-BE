package com.tcg.arena.repository;

import com.tcg.arena.model.TCGSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TCGSetRepository extends JpaRepository<TCGSet, Long> {
    Optional<TCGSet> findBySetCode(String setCode);
    List<TCGSet> findAllByOrderByReleaseDateDesc();
}