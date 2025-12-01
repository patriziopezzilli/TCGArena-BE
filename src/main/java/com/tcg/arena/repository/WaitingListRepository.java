package com.tcg.arena.repository;

import com.tcg.arena.model.WaitingListEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaitingListRepository extends JpaRepository<WaitingListEntry, Long> {
    
    Optional<WaitingListEntry> findByEmail(String email);
    
    boolean existsByEmail(String email);
}
