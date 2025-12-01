package com.example.tcgbackend.service;

import com.example.tcgbackend.dto.ReservationDTO.*;
import com.example.tcgbackend.model.InventoryCard;
import com.example.tcgbackend.model.Reservation;
import com.example.tcgbackend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private final ReservationRepository reservationRepository;
    private final InventoryCardService inventoryCardService;
    
    private static final int RESERVATION_DURATION_MINUTES = 30;
    
    /**
     * Create a new reservation
     */
    @Transactional
    public ReservationResponse createReservation(
        String userId,
        CreateReservationRequest request
    ) {
        log.info("Creating reservation for user: {} card: {}", userId, request.getCardId());
        
        // Get inventory card and validate availability
        InventoryCard inventoryCard = inventoryCardService.getInventoryCard(request.getCardId());
        
        if (inventoryCard.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient quantity available");
        }
        
        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setCardId(request.getCardId());
        reservation.setUserId(userId);
        reservation.setMerchantId(inventoryCard.getShopId());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation.setQrCode(UUID.randomUUID().toString());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(RESERVATION_DURATION_MINUTES));
        
        // Decrease inventory quantity
        inventoryCardService.updateQuantity(request.getCardId(), -request.getQuantity());
        
        Reservation saved = reservationRepository.save(reservation);
        
        return new ReservationResponse(
            saved,
            "Reservation created successfully. Please complete pickup within 30 minutes."
        );
    }
    
    /**
     * Get user's reservations
     */
    @Transactional(readOnly = true)
    public ReservationListResponse getUserReservations(
        String userId,
        int page,
        int size
    ) {
        log.info("Getting reservations for user: {}", userId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reservation> reservationPage = reservationRepository.findByUserId(userId, pageable);
        
        return new ReservationListResponse(
            reservationPage.getContent(),
            (int) reservationPage.getTotalElements(),
            page,
            size
        );
    }
    
    /**
     * Get merchant's reservations
     */
    @Transactional(readOnly = true)
    public ReservationListResponse getMerchantReservations(
        String merchantId,
        Reservation.ReservationStatus status,
        int page,
        int size
    ) {
        log.info("Getting reservations for merchant: {} with status: {}", merchantId, status);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reservation> reservationPage;
        
        if (status == null) {
            reservationPage = reservationRepository.findByMerchantId(merchantId, pageable);
        } else {
            reservationPage = reservationRepository.findByMerchantIdAndStatus(merchantId, status, pageable);
        }
        
        return new ReservationListResponse(
            reservationPage.getContent(),
            (int) reservationPage.getTotalElements(),
            page,
            size
        );
    }
    
    /**
     * Validate reservation with QR code
     */
    @Transactional
    public ReservationResponse validateReservation(
        String merchantId,
        ValidateReservationRequest request
    ) {
        log.info("Validating reservation with QR code: {}", request.getQrCode());
        
        Reservation reservation = reservationRepository.findByQrCode(request.getQrCode())
            .orElseThrow(() -> new RuntimeException("Invalid QR code"));
        
        // Verify merchant
        if (!reservation.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized: This reservation belongs to a different shop");
        }
        
        // Verify can be validated
        if (!reservation.canBeValidated()) {
            throw new RuntimeException("Reservation cannot be validated (status: " + reservation.getStatus() + ")");
        }
        
        // Update status
        reservation.setStatus(Reservation.ReservationStatus.VALIDATED);
        reservation.setValidatedAt(LocalDateTime.now());
        
        Reservation saved = reservationRepository.save(reservation);
        
        return new ReservationResponse(
            saved,
            "Reservation validated successfully"
        );
    }
    
    /**
     * Complete pickup
     */
    @Transactional
    public ReservationResponse completePickup(
        String merchantId,
        String reservationId
    ) {
        log.info("Completing pickup for reservation: {}", reservationId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        // Verify merchant
        if (!reservation.getMerchantId().equals(merchantId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Verify can be picked up
        if (!reservation.canBePickedUp()) {
            throw new RuntimeException("Reservation cannot be picked up (status: " + reservation.getStatus() + ")");
        }
        
        // Update status
        reservation.setStatus(Reservation.ReservationStatus.PICKED_UP);
        reservation.setPickedUpAt(LocalDateTime.now());
        
        Reservation saved = reservationRepository.save(reservation);
        
        return new ReservationResponse(
            saved,
            "Pickup completed successfully"
        );
    }
    
    /**
     * Cancel reservation
     */
    @Transactional
    public ReservationResponse cancelReservation(
        String userId,
        String reservationId
    ) {
        log.info("Cancelling reservation: {} by user: {}", reservationId, userId);
        
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new RuntimeException("Reservation not found"));
        
        // Verify user
        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        // Verify can be cancelled
        if (!reservation.canBeCancelled()) {
            throw new RuntimeException("Reservation cannot be cancelled (status: " + reservation.getStatus() + ")");
        }
        
        // Restore inventory quantity
        InventoryCard card = inventoryCardService.getInventoryCard(reservation.getCardId());
        inventoryCardService.updateQuantity(reservation.getCardId(), 1);
        
        // Update status
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);
        
        Reservation saved = reservationRepository.save(reservation);
        
        return new ReservationResponse(
            saved,
            "Reservation cancelled successfully"
        );
    }
    
    /**
     * Get reservation statistics
     */
    @Transactional(readOnly = true)
    public ReservationStatsResponse getReservationStats(String merchantId) {
        log.info("Getting reservation stats for merchant: {}", merchantId);
        
        long pendingCount = reservationRepository.findByMerchantIdAndStatus(
            merchantId, 
            Reservation.ReservationStatus.PENDING,
            Pageable.unpaged()
        ).getTotalElements();
        
        long validatedCount = reservationRepository.findByMerchantIdAndStatus(
            merchantId,
            Reservation.ReservationStatus.VALIDATED,
            Pageable.unpaged()
        ).getTotalElements();
        
        long pickedUpCount = reservationRepository.findByMerchantIdAndStatus(
            merchantId,
            Reservation.ReservationStatus.PICKED_UP,
            Pageable.unpaged()
        ).getTotalElements();
        
        LocalDateTime thresholdTime = LocalDateTime.now().plusMinutes(5);
        long expiringSoonCount = reservationRepository.findExpiringSoon(merchantId, thresholdTime).size();
        
        return new ReservationStatsResponse(
            pendingCount,
            validatedCount,
            pickedUpCount,
            expiringSoonCount
        );
    }
    
    /**
     * Scheduled task to expire reservations
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void expireReservations() {
        log.debug("Running scheduled task to expire reservations");
        
        List<Reservation> expiredReservations = reservationRepository.findExpiredReservations();
        
        for (Reservation reservation : expiredReservations) {
            log.info("Expiring reservation: {}", reservation.getId());
            
            // Restore inventory quantity
            try {
                inventoryCardService.updateQuantity(reservation.getCardId(), 1);
            } catch (Exception e) {
                log.error("Error restoring inventory for expired reservation: {}", reservation.getId(), e);
            }
            
            // Update status
            reservation.setStatus(Reservation.ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
        }
        
        if (!expiredReservations.isEmpty()) {
            log.info("Expired {} reservations", expiredReservations.size());
        }
    }
}
