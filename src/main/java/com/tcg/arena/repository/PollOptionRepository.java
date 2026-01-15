package com.tcg.arena.repository;

import com.tcg.arena.model.PollOption;
import com.tcg.arena.model.CommunityThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollOptionRepository extends JpaRepository<PollOption, Long> {

    List<PollOption> findByThreadOrderByCreatedAtAsc(CommunityThread thread);
}