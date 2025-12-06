package com.tcg.arena.repository;

import com.tcg.arena.model.TournamentParticipant;
import com.tcg.arena.model.ParticipantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface TournamentParticipantRepository extends JpaRepository<TournamentParticipant, Long> {
    List<TournamentParticipant> findByTournamentId(Long tournamentId);

    List<TournamentParticipant> findByUserId(Long userId);

    Optional<TournamentParticipant> findByTournamentIdAndUserId(Long tournamentId, Long userId);

    List<TournamentParticipant> findByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);

    List<TournamentParticipant> findByTournamentIdAndStatusOrderByRegistrationDateAsc(Long tournamentId,
            ParticipantStatus status);

    long countByTournamentIdAndStatus(Long tournamentId, ParticipantStatus status);

    long countByTournamentIdAndStatusIn(Long tournamentId, List<ParticipantStatus> statuses);

    List<TournamentParticipant> findByTournamentIdAndStatusIn(Long tournamentId, List<ParticipantStatus> statuses);

    long countByTournamentId(Long tournamentId);

    Optional<TournamentParticipant> findByCheckInCode(String checkInCode);

    @Query("SELECT tp FROM TournamentParticipant tp JOIN FETCH tp.user u WHERE tp.tournamentId = :tournamentId")
    List<TournamentParticipant> findByTournamentIdWithUserDetails(@Param("tournamentId") Long tournamentId);
}