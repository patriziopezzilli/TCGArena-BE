package com.tcg.arena.repository;

import com.tcg.arena.model.PollVote;
import com.tcg.arena.model.PollOption;
import com.tcg.arena.model.User;
import com.tcg.arena.model.CommunityThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PollVoteRepository extends JpaRepository<PollVote, Long> {

    boolean existsByUserAndPollOptionThread(User user, CommunityThread thread);

    Optional<PollVote> findByUserAndPollOptionThread(User user, CommunityThread thread);

    List<PollVote> findByPollOption(PollOption pollOption);

    int countByPollOption(PollOption pollOption);

    boolean existsByUserAndPollOption(User user, PollOption pollOption);
}