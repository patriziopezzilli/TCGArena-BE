package com.tcg.arena.repository;

import com.tcg.arena.model.Reservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    
    /**
     * Find reservation by QR code
     */
    Optional<Reservation> findByQrCode(String qrCode);
    
    /**
     * Find all reservations for a user
     */
    Page<Reservation> findByUserId(Long userId, Pageable pageable);
    
    /**
     * Find all reservations for a merchant
     */
    Page<Reservation> findByMerchantId(Long merchantId, Pageable pageable);
    
    /**
     * Find reservations by status
     */
    Page<Reservation> findByMerchantIdAndStatus(
        Long merchantId, 
        Reservation.ReservationStatus status, 
        Pageable pageable
    );
    
    /**
     * Find active reservations (pending or validated)
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.merchantId = :merchantId
        AND r.status IN ('PENDING', 'VALIDATED')
        AND r.expiresAt > CURRENT_TIMESTAMP
        ORDER BY r.createdAt DESC
        """)
    List<Reservation> findActiveReservations(@Param("merchantId") Long merchantId);
    
    /**
     * Find expired pending reservations
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.status = 'PENDING'
        AND r.expiresAt < CURRENT_TIMESTAMP
        """)
    List<Reservation> findExpiredReservations();
    
    /**
     * Find user's active reservations
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.userId = :userId
        AND r.status IN ('PENDING', 'VALIDATED')
        AND r.expiresAt > CURRENT_TIMESTAMP
        ORDER BY r.createdAt DESC
        """)
    List<Reservation> findUserActiveReservations(@Param("userId") Long userId);
    
    /**
     * Count reservations by card
     */
    long countByCardIdAndStatusIn(String cardId, List<Reservation.ReservationStatus> statuses);
    
    /**
     * Find reservations expiring soon
     */
    @Query("""
        SELECT r FROM Reservation r
        WHERE r.merchantId = :merchantId
        AND r.status = 'PENDING'
        AND r.expiresAt > CURRENT_TIMESTAMP
        AND r.expiresAt < :thresholdTime
        ORDER BY r.expiresAt ASC
        """)
    List<Reservation> findExpiringSoon(
        @Param("merchantId") Long merchantId,
        @Param("thresholdTime") LocalDateTime thresholdTime
    );
}
