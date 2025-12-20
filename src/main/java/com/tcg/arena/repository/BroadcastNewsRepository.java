package com.tcg.arena.repository;

import com.tcg.arena.model.BroadcastNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BroadcastNewsRepository extends JpaRepository<BroadcastNews, Long> {

    /**
     * Get all active broadcast news (started and not expired)
     */
    @Query("SELECT n FROM BroadcastNews n WHERE n.startDate <= :now AND (n.expiryDate IS NULL OR n.expiryDate > :now) ORDER BY n.isPinned DESC, n.startDate DESC")
    List<BroadcastNews> findActiveNews(LocalDateTime now);

    /**
     * Get all broadcast news ordered by creation date
     */
    List<BroadcastNews> findAllByOrderByCreatedAtDesc();

    /**
     * Get future broadcast news
     */
    @Query("SELECT n FROM BroadcastNews n WHERE n.startDate > :now ORDER BY n.startDate ASC")
    List<BroadcastNews> findFutureNews(LocalDateTime now);

    /**
     * Get expired broadcast news
     */
    @Query("SELECT n FROM BroadcastNews n WHERE n.expiryDate IS NOT NULL AND n.expiryDate <= :now ORDER BY n.expiryDate DESC")
    List<BroadcastNews> findExpiredNews(LocalDateTime now);
}
