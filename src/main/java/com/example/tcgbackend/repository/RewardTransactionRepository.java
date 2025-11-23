package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.RewardTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardTransactionRepository extends JpaRepository<RewardTransaction, Long> {
    List<RewardTransaction> findByUserIdOrderByTimestampDesc(Long userId);
}