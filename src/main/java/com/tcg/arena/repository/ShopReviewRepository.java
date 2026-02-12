package com.tcg.arena.repository;

import com.tcg.arena.model.ShopReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopReviewRepository extends JpaRepository<ShopReview, Long> {

    List<ShopReview> findByShopIdOrderByCreatedAtDesc(Long shopId);

    Optional<ShopReview> findByShopIdAndUserId(Long shopId, Long userId);

    @Query("SELECT AVG(r.rating) FROM ShopReview r WHERE r.shopId = ?1")
    Double getAverageRating(Long shopId);

    @Query("SELECT COUNT(r) FROM ShopReview r WHERE r.shopId = ?1")
    Long countByShopId(Long shopId);
}
