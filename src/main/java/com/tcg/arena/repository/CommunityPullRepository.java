package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityPull;
import com.tcg.arena.model.TCGType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityPullRepository extends JpaRepository<CommunityPull, Long> {
    Page<CommunityPull> findByOrderByCreatedAtDesc(Pageable pageable);

    Page<CommunityPull> findByTcgTypeOrderByCreatedAtDesc(TCGType tcgType, Pageable pageable);
}
