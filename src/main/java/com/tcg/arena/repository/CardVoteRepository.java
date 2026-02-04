package com.tcg.arena.repository;

import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.CardVote;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CardVoteRepository extends JpaRepository<CardVote, Long> {

    Optional<CardVote> findByCardTemplateAndUser(CardTemplate cardTemplate, User user);

    boolean existsByCardTemplateAndUser(CardTemplate cardTemplate, User user);

    @Query("SELECT COUNT(v) FROM CardVote v WHERE v.cardTemplate.id = :cardTemplateId AND v.voteType = 'LIKE'")
    long countLikesByCardTemplateId(@Param("cardTemplateId") Long cardTemplateId);

    @Query("SELECT COUNT(v) FROM CardVote v WHERE v.cardTemplate.id = :cardTemplateId AND v.voteType = 'DISLIKE'")
    long countDislikesByCardTemplateId(@Param("cardTemplateId") Long cardTemplateId);

    @Query("SELECT v.cardTemplate.id FROM CardVote v WHERE v.user = :user")
    Page<Long> findVotedCardTemplateIdsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT DISTINCT v.user FROM CardVote v")
    List<User> findUsersWhoHaveVoted();

    @Query("SELECT ct FROM CardTemplate ct WHERE ct.tcgType = :tcgType " +
           "AND ct.id NOT IN (SELECT v.cardTemplate.id FROM CardVote v WHERE v.user = :user) " +
           "ORDER BY FUNCTION('RANDOM')")
    Page<CardTemplate> findRandomUnvotedCardTemplates(@Param("tcgType") TCGType tcgType,
                                                       @Param("user") User user,
                                                       Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM CardVote v WHERE v.cardTemplate.setCode = :setCode")
    int deleteByCardTemplateSetCode(@Param("setCode") String setCode);
}
