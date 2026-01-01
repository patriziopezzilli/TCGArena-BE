package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityPull;
import com.tcg.arena.model.PullLike;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PullLikeRepository extends JpaRepository<PullLike, Long> {
    Optional<PullLike> findByPullAndUser(CommunityPull pull, User user);

    boolean existsByPullAndUser(CommunityPull pull, User user);

    long countByPull(CommunityPull pull);
}
