package com.tcg.arena.repository;

import com.tcg.arena.model.Tournament;
import com.tcg.arena.model.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TournamentRepository extends JpaRepository<Tournament, Long> {
    @Query("SELECT t FROM Tournament t WHERE t.startDate > :now OR (t.endDate >= :fiveHoursAgo) ORDER BY t.startDate")
    List<Tournament> findUpcomingTournaments(@Param("now") LocalDateTime now,
            @Param("fiveHoursAgo") LocalDateTime fiveHoursAgo);

    @Query("SELECT t FROM Tournament t WHERE t.endDate < :fiveHoursAgo ORDER BY t.endDate DESC")
    List<Tournament> findPastTournaments(@Param("fiveHoursAgo") LocalDateTime fiveHoursAgo);

    List<Tournament> findAllByOrderByStartDateDesc();

    List<Tournament> findAllByOrderByStartDateAsc();

    List<Tournament> findByStatus(TournamentStatus status);

    List<Tournament> findByOrganizerId(Long organizerId);

    List<Tournament> findByStatusAndOrganizerId(TournamentStatus status, Long organizerId);

    /**
     * Find all tournaments created by a user (for customers who request
     * tournaments)
     */
    List<Tournament> findByCreatedByUserIdOrderByStartDateDesc(Long createdByUserId);

    /**
     * Count upcoming tournaments for a merchant (optimized for dashboard)
     */
    @Query("""
            SELECT COUNT(t) FROM Tournament t
            WHERE t.organizerId = :organizerId
            AND t.startDate > :now
            """)
    long countUpcomingTournamentsByOrganizer(@Param("organizerId") Long organizerId, @Param("now") LocalDateTime now);

    /**
     * Find today's tournaments for a merchant
     */
    @Query("""
            SELECT t FROM Tournament t
            WHERE t.organizerId = :organizerId
            AND DATE(t.startDate) = DATE(:today)
            ORDER BY t.startDate ASC
            """)
    List<Tournament> findTodaysTournamentsByOrganizer(@Param("organizerId") Long organizerId,
            @Param("today") LocalDateTime today);

    /**
     * Find tournaments starting within the next N hours
     */
    @Query("""
            SELECT t FROM Tournament t
            WHERE t.organizerId = :organizerId
            AND t.startDate BETWEEN :now AND :until
            ORDER BY t.startDate ASC
            """)
    List<Tournament> findUpcomingTournamentsByOrganizerWithinHours(
            @Param("organizerId") Long organizerId,
            @Param("now") LocalDateTime now,
            @Param("until") LocalDateTime until);
}