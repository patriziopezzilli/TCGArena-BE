package com.tcg.arena.repository;

import com.tcg.arena.model.ArenaApiKey;
import com.tcg.arena.model.ArenaApiPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArenaApiKeyRepository extends JpaRepository<ArenaApiKey, Long> {

    Optional<ArenaApiKey> findByApiKey(String apiKey);

    Optional<ArenaApiKey> findByApiKeyAndActiveTrue(String apiKey);

    List<ArenaApiKey> findByActiveTrue();

    List<ArenaApiKey> findByPlan(ArenaApiPlan plan);

    List<ArenaApiKey> findByEmail(String email);

    boolean existsByApiKey(String apiKey);

    long countByActiveTrue();

    long countByPlan(ArenaApiPlan plan);
}
