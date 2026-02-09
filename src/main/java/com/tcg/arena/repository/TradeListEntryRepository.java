package com.tcg.arena.repository;

import com.tcg.arena.model.TradeListEntry;
import com.tcg.arena.model.TradeListType;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeListEntryRepository extends JpaRepository<TradeListEntry, Long> {
    List<TradeListEntry> findByUserAndType(User user, TradeListType type);

    Optional<TradeListEntry> findByUserAndCardTemplateIdAndType(User user, Long cardTemplateId, TradeListType type);

    List<TradeListEntry> findByUser(User user);

    @Modifying
    @Transactional
    @Query("DELETE FROM TradeListEntry tle WHERE tle.cardTemplate.expansion.id = :expansionId")
    int deleteByCardTemplateExpansionId(@Param("expansionId") Long expansionId);

    @Modifying
    @Transactional
    @Query("DELETE FROM TradeListEntry tle WHERE tle.cardTemplate.setCode = :setCode")
    int deleteByCardTemplateSetCode(@Param("setCode") String setCode);

    // Find all entries for a specific card template and type (e.g. all WANT entries
    // for Charizard)
    // With JOIN FETCH to get the user efficiently
    @Query("SELECT tle FROM TradeListEntry tle JOIN FETCH tle.user WHERE tle.cardTemplate.id = :cardTemplateId AND tle.type = :type")
    List<TradeListEntry> findByCardTemplateIdAndType(@Param("cardTemplateId") Long cardTemplateId,
            @Param("type") TradeListType type);
}
