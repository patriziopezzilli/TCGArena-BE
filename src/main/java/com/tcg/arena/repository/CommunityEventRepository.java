package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.CommunityEvent.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommunityEventRepository extends JpaRepository<CommunityEvent, Long> {

    // Find upcoming events ordered by date
    @Query("SELECT e FROM CommunityEvent e WHERE e.eventDate >= :now AND e.status = :status ORDER BY e.eventDate ASC")
    List<CommunityEvent> findUpcoming(@Param("now") LocalDateTime now, @Param("status") EventStatus status);

    // Find events by creator
    List<CommunityEvent> findByCreatorIdOrderByEventDateDesc(Long creatorId);

    // Find events by TCG type
    @Query("SELECT e FROM CommunityEvent e WHERE e.eventDate >= :now AND e.status = :status AND e.tcgType = :tcgType ORDER BY e.eventDate ASC")
    List<CommunityEvent> findUpcomingByTcgType(@Param("now") LocalDateTime now, @Param("status") EventStatus status,
            @Param("tcgType") String tcgType);

    // Find nearby events using Haversine formula (approximate)
    @Query(value = """
            SELECT * FROM community_event e
            WHERE e.event_date >= :now
            AND e.status = 'ACTIVE'
            AND e.latitude IS NOT NULL
            AND e.longitude IS NOT NULL
            AND (
                6371 * acos(
                    cos(radians(:lat)) * cos(radians(e.latitude)) *
                    cos(radians(e.longitude) - radians(:lon)) +
                    sin(radians(:lat)) * sin(radians(e.latitude))
                )
            ) <= :radiusKm
            ORDER BY e.event_date ASC
            """, nativeQuery = true)
    List<CommunityEvent> findNearby(
            @Param("now") LocalDateTime now,
            @Param("lat") Double lat,
            @Param("lon") Double lon,
            @Param("radiusKm") Double radiusKm);

    // Count upcoming events
    @Query("SELECT COUNT(e) FROM CommunityEvent e WHERE e.eventDate >= :now AND e.status = :status")
    long countUpcoming(@Param("now") LocalDateTime now, @Param("status") EventStatus status);

    // Find events where user is participant
    @Query("SELECT e FROM CommunityEvent e JOIN e.participants p WHERE p.user.id = :userId AND p.status = 'JOINED' ORDER BY e.eventDate ASC")
    List<CommunityEvent> findByParticipantUserId(@Param("userId") Long userId);
}
