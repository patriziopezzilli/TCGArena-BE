package com.tcg.arena.repository;

import com.tcg.arena.model.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByUserIdOrderByTimestampDesc(Long userId);

    // Admin methods
    List<RewardTransaction> findAllByOrderByTimestampDesc();

    List<RewardTransaction> findByRewardIdIsNotNullAndStatusIn(
            List<RewardTransaction.RewardFulfillmentStatus> statuses);
}