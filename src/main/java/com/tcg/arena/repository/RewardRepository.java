package com.tcg.arena.repository;

import com.tcg.arena.model.Reward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardRepository extends JpaRepository<Reward, Long> {
    List<Reward> findByIsActiveTrue();

    @Query("SELECT r FROM Reward r LEFT JOIN FETCH r.partner WHERE r.isActive = true")
    List<Reward> findByIsActiveTrueWithPartner();

    @Query("SELECT r FROM Reward r LEFT JOIN FETCH r.partner WHERE r.partner.id = :partnerId")
    List<Reward> findByPartnerId(Long partnerId);
}