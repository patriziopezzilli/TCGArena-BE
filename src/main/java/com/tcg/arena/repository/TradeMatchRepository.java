package com.tcg.arena.repository;

import com.tcg.arena.model.TradeMatch;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeMatchRepository extends JpaRepository<TradeMatch, Long> {
    
    @Query("SELECT tm FROM TradeMatch tm WHERE (tm.user1 = :user OR tm.user2 = :user) AND tm.status = 'ACTIVE'")
    List<TradeMatch> findActiveMatchesForUser(@Param("user") User user);

    @Query("SELECT tm FROM TradeMatch tm WHERE (tm.user1 = :u1 AND tm.user2 = :u2) OR (tm.user1 = :u2 AND tm.user2 = :u1)")
    TradeMatch findByUsers(@Param("u1") User u1, @Param("u2") User u2);
}
