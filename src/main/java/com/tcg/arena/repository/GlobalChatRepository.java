package com.tcg.arena.repository;

import com.tcg.arena.model.GlobalChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for global chat messages.
 */
@Repository
public interface GlobalChatRepository extends JpaRepository<GlobalChatMessage, Long> {

    /**
     * Get the most recent messages, ordered by timestamp descending.
     */
    @Query("SELECT m FROM GlobalChatMessage m ORDER BY m.timestamp DESC LIMIT 50")
    List<GlobalChatMessage> findRecentMessages();

    /**
     * Get messages newer than a specific ID (for pagination/sync).
     */
    @Query("SELECT m FROM GlobalChatMessage m WHERE m.id > :afterId ORDER BY m.timestamp ASC")
    List<GlobalChatMessage> findMessagesAfterId(Long afterId);
}
