package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.CommunityEventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EventParticipantRepository extends JpaRepository<CommunityEventParticipant, Long> {
    
    List<CommunityEventParticipant> findByEvent(CommunityEvent event);
    
    List<CommunityEventParticipant> findByEventId(Long eventId);
}
