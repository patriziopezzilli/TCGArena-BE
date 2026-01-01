package com.tcg.arena.repository;

import com.tcg.arena.model.CommunityThread;
import com.tcg.arena.model.ThreadResponse;
import com.tcg.arena.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThreadResponseRepository extends JpaRepository<ThreadResponse, Long> {

    List<ThreadResponse> findByThreadOrderByCreatedAtAsc(CommunityThread thread);

    boolean existsByThreadAndResponder(CommunityThread thread, User responder);

    int countByThread(CommunityThread thread);
}
