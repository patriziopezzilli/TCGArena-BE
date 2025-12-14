package com.tcg.arena.repository;

import com.tcg.arena.model.TournamentUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TournamentUpdateRepository extends JpaRepository<TournamentUpdate, Long> {

    List<TournamentUpdate> findByTournamentIdOrderByCreatedAtDesc(Long tournamentId);

    void deleteByTournamentId(Long tournamentId);

    int countByTournamentId(Long tournamentId);
}
