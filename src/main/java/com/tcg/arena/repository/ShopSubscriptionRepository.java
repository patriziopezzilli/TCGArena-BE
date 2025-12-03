package com.tcg.arena.repository;

import com.tcg.arena.model.ShopSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopSubscriptionRepository extends JpaRepository<ShopSubscription, Long> {

    Optional<ShopSubscription> findByUserIdAndShopIdAndIsActiveTrue(Long userId, Long shopId);

    List<ShopSubscription> findByUserIdAndIsActiveTrue(Long userId);

    List<ShopSubscription> findByShopIdAndIsActiveTrue(Long shopId);

    @Query("SELECT COUNT(s) FROM ShopSubscription s WHERE s.shopId = :shopId AND s.isActive = true")
    Long countActiveSubscriptionsByShopId(@Param("shopId") Long shopId);

    boolean existsByUserIdAndShopIdAndIsActiveTrue(Long userId, Long shopId);
}