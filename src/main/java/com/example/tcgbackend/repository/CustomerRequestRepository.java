package com.example.tcgbackend.repository;

import com.example.tcgbackend.model.CustomerRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRequestRepository extends JpaRepository<CustomerRequest, String> {
    
    /**
     * Find requests by shop
     */
    Page<CustomerRequest> findByShopId(String shopId, Pageable pageable);
    
    /**
     * Find requests by user
     */
    Page<CustomerRequest> findByUserId(String userId, Pageable pageable);
    
    /**
     * Find requests by shop and status
     */
    Page<CustomerRequest> findByShopIdAndStatus(
        String shopId, 
        CustomerRequest.RequestStatus status, 
        Pageable pageable
    );
    
    /**
     * Find requests by shop and type
     */
    Page<CustomerRequest> findByShopIdAndType(
        String shopId, 
        CustomerRequest.RequestType type, 
        Pageable pageable
    );
    
    /**
     * Complex search with multiple filters
     */
    @Query("""
        SELECT r FROM CustomerRequest r
        WHERE (:shopId IS NULL OR r.shopId = :shopId)
        AND (:userId IS NULL OR r.userId = :userId)
        AND (:status IS NULL OR r.status = :status)
        AND (:type IS NULL OR r.type = :type)
        ORDER BY r.createdAt DESC
        """)
    Page<CustomerRequest> searchRequests(
        @Param("shopId") String shopId,
        @Param("userId") String userId,
        @Param("status") CustomerRequest.RequestStatus status,
        @Param("type") CustomerRequest.RequestType type,
        Pageable pageable
    );
    
    /**
     * Find unread requests for shop
     */
    @Query("""
        SELECT r FROM CustomerRequest r
        WHERE r.shopId = :shopId
        AND r.hasUnreadMessages = true
        ORDER BY r.updatedAt DESC
        """)
    List<CustomerRequest> findUnreadRequests(@Param("shopId") String shopId);
    
    /**
     * Count pending requests by shop
     */
    long countByShopIdAndStatus(String shopId, CustomerRequest.RequestStatus status);
    
    /**
     * Count requests with unread messages
     */
    long countByShopIdAndHasUnreadMessages(String shopId, Boolean hasUnreadMessages);
}
