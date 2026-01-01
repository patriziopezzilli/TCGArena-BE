package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityEventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityEventParticipantRepository extends JpaRepository<CommunityEventParticipant, Long> {

    // Find all participants for an event
    List<CommunityEventParticipant> findByEventId(Long eventId);

    // Count participants for an event
    long countByEventIdAndStatus(Long eventId, CommunityEventParticipant.ParticipantStatus status);

    // Check if user is already a participant
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    // Find participant by event and user
    Optional<CommunityEventParticipant> findByEventIdAndUserId(Long eventId, Long userId);

    // Find all events a user has joined
    List<CommunityEventParticipant> findByUserIdAndStatus(Long userId,
            CommunityEventParticipant.ParticipantStatus status);
}
