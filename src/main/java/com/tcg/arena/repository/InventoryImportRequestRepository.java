package com.tcg.arena.repository;

import com.tcg.arena.model.InventoryImportRequest;
import com.tcg.arena.model.InventoryImportRequest.ImportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryImportRequestRepository extends JpaRepository<InventoryImportRequest, Long> {

    /**
     * Find all import requests for a specific shop
     */
    Page<InventoryImportRequest> findByShopIdOrderByCreatedAtDesc(Long shopId, Pageable pageable);

    /**
     * Find all import requests for a shop with a specific status
     */
    List<InventoryImportRequest> findByShopIdAndStatusOrderByCreatedAtDesc(Long shopId, ImportStatus status);

    /**
     * Find all pending import requests (for admin processing)
     */
    List<InventoryImportRequest> findByStatusOrderByCreatedAtAsc(ImportStatus status);

    /**
     * Count pending requests for a shop
     */
    long countByShopIdAndStatus(Long shopId, ImportStatus status);

    /**
     * Find all pending requests across all shops
     */
    @Query("SELECT r FROM InventoryImportRequest r WHERE r.status = :status ORDER BY r.createdAt ASC")
    List<InventoryImportRequest> findAllPending(@Param("status") ImportStatus status);
}
