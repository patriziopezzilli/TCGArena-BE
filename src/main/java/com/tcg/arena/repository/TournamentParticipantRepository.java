package com.tcg.arena.repository;

import com.tcg.arena.model.TournamentParticipant;
import com.tcg.arena.model.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);
    List<TournamentParticipant> findByUserId(Long userId);
    Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);
    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
    List<TournamentParticipant> findByTournamentIdAndStatusOrderByRegistrationDateAsc(Long tournamentId, ParticipantStatus status);
    long countByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);
}