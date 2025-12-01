package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.TCGSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TCGSetRepository extends JpaRepository<TCGSet, Long> {
    Optional<TCGSet> findBySetCode(String setCode);
    List<TCGSet> findAllByOrderByReleaseDateDesc();
}