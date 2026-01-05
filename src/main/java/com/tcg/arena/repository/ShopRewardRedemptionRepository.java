package com.tcg.arena.repository;

import com.tcg.arena.model.RedemptionStatus;
import com.tcg.arena.model.ShopRewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRewardRedemptionRepository extends JpaRepository<ShopRewardRedemption, Long> {
    
    List<ShopRewardRedemption> findByUserId(Long userId);
    
    List<ShopRewardRedemption> findByUserIdOrderByRedeemedAtDesc(Long userId);
    
    @Query("SELECT srr FROM ShopRewardRedemption srr WHERE srr.shopReward.shop.id = :shopId ORDER BY srr.redeemedAt DESC")
    List<ShopRewardRedemption> findByShopId(@Param("shopId") Long shopId);
    
    @Query("SELECT srr FROM ShopRewardRedemption srr WHERE srr.shopReward.shop.id = :shopId AND srr.status = :status ORDER BY srr.redeemedAt DESC")
    List<ShopRewardRedemption> findByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") RedemptionStatus status);
    
    Optional<ShopRewardRedemption> findByRedemptionCode(String redemptionCode);
    
    @Query("SELECT COUNT(srr) FROM ShopRewardRedemption srr WHERE srr.shopReward.shop.id = :shopId AND srr.status = :status")
    long countByShopIdAndStatus(@Param("shopId") Long shopId, @Param("status") RedemptionStatus status);
    
    @Query("SELECT COUNT(srr) FROM ShopRewardRedemption srr WHERE srr.shopReward.id = :rewardId AND srr.user.id = :userId")
    long countByRewardIdAndUserId(@Param("rewardId") Long rewardId, @Param("userId") Long userId);
}
