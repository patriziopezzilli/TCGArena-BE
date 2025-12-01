package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.InventoryCard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryCardRepository extends JpaRepository<InventoryCard, String> {
    
    /**
     * Find all inventory cards for a specific shop
     */
    Page<InventoryCard> findByShopId(String shopId, Pageable pageable);
    
    /**
     * Find inventory cards by shop and condition
     */
    Page<InventoryCard> findByShopIdAndCondition(
        String shopId, 
        InventoryCard.CardCondition condition, 
        Pageable pageable
    );
    
    /**
     * Complex search with filters
     */
    @Query("""
        SELECT i FROM InventoryCard i
        JOIN i.cardTemplate c
        WHERE i.shopId = :shopId
        AND (:tcgType IS NULL OR c.tcgType = :tcgType)
        AND (:condition IS NULL OR i.condition = :condition)
        AND (:minPrice IS NULL OR i.price >= :minPrice)
        AND (:maxPrice IS NULL OR i.price <= :maxPrice)
        AND (:searchQuery IS NULL OR 
             LOWER(c.name) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR
             LOWER(c.setName) LIKE LOWER(CONCAT('%', :searchQuery, '%')))
        AND i.quantity > 0
        """)
    Page<InventoryCard> searchInventory(
        @Param("shopId") String shopId,
        @Param("tcgType") String tcgType,
        @Param("condition") InventoryCard.CardCondition condition,
        @Param("minPrice") Double minPrice,
        @Param("maxPrice") Double maxPrice,
        @Param("searchQuery") String searchQuery,
        Pageable pageable
    );
    
    /**
     * Find by card template ID and shop
     */
    List<InventoryCard> findByCardTemplateIdAndShopId(String cardTemplateId, String shopId);
    
    /**
     * Count inventory items by shop
     */
    long countByShopId(String shopId);
    
    /**
     * Find low stock items (quantity <= threshold)
     */
    @Query("""
        SELECT i FROM InventoryCard i
        WHERE i.shopId = :shopId
        AND i.quantity > 0
        AND i.quantity <= :threshold
        ORDER BY i.quantity ASC
        """)
    List<InventoryCard> findLowStockItems(@Param("shopId") String shopId, @Param("threshold") int threshold);
}
