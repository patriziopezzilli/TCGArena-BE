package com.tcg.arena.repository;

import com.tcg.arena.model.TradeMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeMessageRepository extends JpaRepository<TradeMessage, Long> {
    List<TradeMessage> findByMatchIdOrderBySentAtAsc(Long matchId);
    boolean existsByMatchId(Long matchId);
}
