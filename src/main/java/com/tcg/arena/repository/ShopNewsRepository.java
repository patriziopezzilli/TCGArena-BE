package com.tcg.arena.repository;

import com.tcg.arena.model.ShopNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShopNewsRepository extends JpaRepository<ShopNews, Long> {

    /**
     * Find all news for a shop, ordered by pinned first, then by created date
     */
    List<ShopNews> findByShopIdOrderByIsPinnedDescCreatedAtDesc(Long shopId);

    /**
     * Find active news: startDate <= now AND (expiryDate IS NULL OR expiryDate >
     * now)
     * Ordered by pinned first, then by start date descending
     */
    @Query("SELECT n FROM ShopNews n WHERE n.shopId = :shopId " +
            "AND n.startDate <= :now " +
            "AND (n.expiryDate IS NULL OR n.expiryDate > :now) " +
            "ORDER BY n.isPinned DESC, n.startDate DESC")
    List<ShopNews> findActiveNewsByShopId(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);

    /**
     * Find future news: startDate > now
     * Ordered by start date ascending (soonest first)
     */
    @Query("SELECT n FROM ShopNews n WHERE n.shopId = :shopId " +
            "AND n.startDate > :now " +
            "ORDER BY n.startDate ASC")
    List<ShopNews> findFutureNewsByShopId(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);

    /**
     * Find expired news: expiryDate IS NOT NULL AND expiryDate <= now
     * Ordered by expiry date descending (most recently expired first)
     */
    @Query("SELECT n FROM ShopNews n WHERE n.shopId = :shopId " +
            "AND n.expiryDate IS NOT NULL " +
            "AND n.expiryDate <= :now " +
            "ORDER BY n.expiryDate DESC")
    List<ShopNews> findExpiredNewsByShopId(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);

    /**
     * Count active news for a shop
     */
    @Query("SELECT COUNT(n) FROM ShopNews n WHERE n.shopId = :shopId " +
            "AND n.startDate <= :now " +
            "AND (n.expiryDate IS NULL OR n.expiryDate > :now)")
    long countActiveNewsByShopId(@Param("shopId") Long shopId, @Param("now") LocalDateTime now);
}
