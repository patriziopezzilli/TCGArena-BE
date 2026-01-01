package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityThread;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityThreadRepository extends JpaRepository<CommunityThread, Long> {

    Page<CommunityThread> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<CommunityThread> findByTcgTypeOrderByCreatedAtDesc(String tcgType, Pageable pageable);
}
