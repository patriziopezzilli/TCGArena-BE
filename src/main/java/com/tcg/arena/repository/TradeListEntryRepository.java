package com.tcg.arena.repository;

import com.tcg.arena.model.TradeListEntry;
import com.tcg.arena.model.TradeListType;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeListEntryRepository extends JpaRepository<TradeListEntry, Long> {
    List<TradeListEntry> findByUserAndType(User user, TradeListType type);
    Optional<TradeListEntry> findByUserAndCardTemplateIdAndType(User user, Long cardTemplateId, TradeListType type);
    List<TradeListEntry> findByUser(User user);
}
