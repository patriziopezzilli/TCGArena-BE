package com.tcg.arena.repository;

import com.tcg.arena.model.UserAppreciation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAppreciationRepository extends JpaRepository<UserAppreciation, Long> {
    Optional<UserAppreciation> findByTargetUserIdAndLikerUserId(Long targetUserId, Long likerUserId);

    long countByTargetUserId(Long targetUserId);

    boolean existsByTargetUserIdAndLikerUserId(Long targetUserId, Long likerUserId);
}
