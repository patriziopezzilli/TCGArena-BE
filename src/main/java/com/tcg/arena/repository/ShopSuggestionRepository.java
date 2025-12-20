package com.tcg.arena.repository;

import com.tcg.arena.model.ShopSuggestion;
import com.tcg.arena.model.ShopSuggestion.SuggestionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopSuggestionRepository extends JpaRepository<ShopSuggestion, Long> {
    
    List<ShopSuggestion> findAllByOrderByCreatedAtDesc();
    
    List<ShopSuggestion> findByStatusOrderByCreatedAtDesc(SuggestionStatus status);
    
    List<ShopSuggestion> findByUserIdOrderByCreatedAtDesc(Long userId);
}
