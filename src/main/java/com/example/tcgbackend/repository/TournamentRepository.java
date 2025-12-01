package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.Tournament;
import com.example.tcgbackend.model.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    @Query("SELECT t FROM Tournament t WHERE t.startDate > :now ORDER BY t.startDate")
    List<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now);

    List<Tournament> findAllByOrderByStartDateDesc();
    List<Tournament> findByStatus(TournamentStatus status);
    List<Tournament> findByOrganizerId(Long organizerId);
}