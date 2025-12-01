package com.tcg.arena.repository;

import com.tcg.arena.model.UserCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCardRepository extends JpaRepository<UserCard, Long> {
    List<UserCard> findByOwnerId(Long ownerId);
    List<UserCard> findByCardTemplateId(Long cardTemplateId);
}