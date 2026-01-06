package com.tcg.arena.repository;

import com.tcg.arena.model.ShopReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShopRewardRepository extends JpaRepository<ShopReward, Long> {

       List<ShopReward> findByShop_IdAndIsActiveTrue(Long shopId);

       List<ShopReward> findByShop_Id(Long shopId);

       @Query("SELECT sr FROM ShopReward sr WHERE sr.shop.id = :shopId AND sr.isActive = true " +
                     "AND (sr.expiresAt IS NULL OR sr.expiresAt > CURRENT_TIMESTAMP) " +
                     "AND (sr.stockQuantity IS NULL OR sr.claimedCount < sr.stockQuantity)")
       List<ShopReward> findAvailableByShopId(@Param("shopId") Long shopId);

       @Query("SELECT sr FROM ShopReward sr WHERE sr.isActive = true " +
                     "AND (sr.expiresAt IS NULL OR sr.expiresAt > CURRENT_TIMESTAMP) " +
                     "AND (sr.stockQuantity IS NULL OR sr.claimedCount < sr.stockQuantity)")
       List<ShopReward> findAllAvailable();

       @Query("SELECT COUNT(sr) FROM ShopReward sr WHERE sr.shop.id = :shopId")
       long countByShopId(@Param("shopId") Long shopId);

       @Query("SELECT DISTINCT sr.shop.id FROM ShopReward sr WHERE sr.isActive = true")
       List<Long> findShopIdsWithActiveRewards();
}
