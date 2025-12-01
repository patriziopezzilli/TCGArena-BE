package com.example.tcgbackend.controller;

import com.example.tcgbackend.dto.ReservationDTO.*;
import com.example.tcgbackend.model.Reservation;
import com.example.tcgbackend.service.ReservationService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {
    
    private static final Logger log = LoggerFactory.getLogger(ReservationController.class);
    
    private final ReservationService reservationService;
    
    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }
    
    /**
     * Create a new reservation (Player)
     * POST /api/reservations
     */
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> createReservation(
        Authentication authentication,
        @Valid @RequestBody CreateReservationRequest request
    ) {
        String userId = authentication.getName();
        log.info("POST /api/reservations - user: {}, card: {}", userId, request.getCardId());
        
        ReservationResponse response = reservationService.createReservation(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    /**
     * Get user's reservations
     * GET /api/reservations/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationListResponse> getMyReservations(
        Authentication authentication,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        String userId = authentication.getName();
        log.info("GET /api/reservations/my - user: {}", userId);
        
        ReservationListResponse response = reservationService.getUserReservations(userId, page, size);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get merchant's reservations
     * GET /api/reservations/merchant?shopId={shopId}&status={status}
     */
    @GetMapping("/merchant")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<ReservationListResponse> getMerchantReservations(
        @RequestParam String shopId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        log.info("GET /api/reservations/merchant - shopId: {}, status: {}", shopId, status);
        
        Reservation.ReservationStatus reservationStatus = status != null ? 
            Reservation.ReservationStatus.valueOf(status) : null;
        
        ReservationListResponse response = reservationService.getMerchantReservations(
            shopId, 
            reservationStatus, 
            page, 
            size
        );
        return ResponseEntity.ok(response);
    }
    
    /**
     * Validate reservation with QR code (Merchant)
     * POST /api/reservations/validate
     */
    @PostMapping("/validate")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<ReservationResponse> validateReservation(
        @RequestParam String shopId,
        @Valid @RequestBody ValidateReservationRequest request
    ) {
        log.info("POST /api/reservations/validate - shopId: {}, qrCode: {}", 
                 shopId, request.getQrCode());
        
        ReservationResponse response = reservationService.validateReservation(shopId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Complete pickup (Merchant)
     * POST /api/reservations/{id}/pickup
     */
    @PostMapping("/{id}/pickup")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<ReservationResponse> completePickup(
        @PathVariable String id,
        @RequestParam String shopId
    ) {
        log.info("POST /api/reservations/{}/pickup - shopId: {}", id, shopId);
        
        ReservationResponse response = reservationService.completePickup(shopId, id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Cancel reservation (Player)
     * POST /api/reservations/{id}/cancel
     */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ReservationResponse> cancelReservation(
        Authentication authentication,
        @PathVariable String id
    ) {
        String userId = authentication.getName();
        log.info("POST /api/reservations/{}/cancel - user: {}", id, userId);
        
        ReservationResponse response = reservationService.cancelReservation(userId, id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get reservation statistics (Merchant)
     * GET /api/reservations/stats?shopId={shopId}
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('MERCHANT')")
    public ResponseEntity<ReservationStatsResponse> getReservationStats(
        @RequestParam String shopId
    ) {
        log.info("GET /api/reservations/stats - shopId: {}", shopId);
        
        ReservationStatsResponse stats = reservationService.getReservationStats(shopId);
        return ResponseEntity.ok(stats);
    }
}
