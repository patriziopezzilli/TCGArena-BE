package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaPriceStatistics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArenaPriceStatisticsRepository extends JpaRepository<ArenaPriceStatistics, Long> {

    Optional<ArenaPriceStatistics> findByVariantId(String variantId);

    void deleteByVariantId(String variantId);
}
