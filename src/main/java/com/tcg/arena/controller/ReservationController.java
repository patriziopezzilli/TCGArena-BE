package com.tcg.arena.controller;

import com.tcg.arena.dto.ReservationDTO.*;
import com.tcg.arena.model.Reservation;
import com.tcg.arena.service.ReservationService;
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
     * Get reservations by card ID (for availability checking)
     * GET /api/reservations?cardId={cardId}&merchantId={merchantId}
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationListResponse> getReservationsByCardId(
        @RequestParam String cardId,
        @RequestParam(required = false) String merchantId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        log.info("GET /api/reservations - cardId: {}, merchantId: {}", cardId, merchantId);
        
        ReservationListResponse response = reservationService.getReservationsByCardId(
            cardId, 
            merchantId, 
            page, 
            size
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new reservation
     * POST /api/reservations
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> createReservation(
        @Valid @RequestBody CreateReservationRequest request,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("POST /api/reservations - user: {}, cardId: {}", username, request.getCardId());

        ReservationResponse response = reservationService.createReservation(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get user's reservations (alias for /user)
     * GET /api/reservations/my
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationListResponse> getMyReservations(
        @RequestParam(required = false) Long shopId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("GET /api/reservations/my - user: {}, shopId: {}", username, shopId);

        ReservationListResponse response = reservationService.getUserReservations(username, shopId, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get user's reservations
     * GET /api/reservations/user
     */
    @GetMapping("/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationListResponse> getUserReservations(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("GET /api/reservations/user - user: {}", username);

        ReservationListResponse response = reservationService.getUserReservations(username, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Get merchant's reservations
     * GET /api/reservations/merchant?shopId={shopId}&status={status}
     */
    @GetMapping("/merchant")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationListResponse> getMerchantReservations(
        @RequestParam Long shopId,
        @RequestParam(defaultValue = "PENDING") Reservation.ReservationStatus status,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("GET /api/reservations/merchant - merchant: {}, shopId: {}, status: {}", username, shopId, status);

        // Verify merchant owns this shop
        // TODO: Add shop ownership verification
        
        ReservationListResponse response = reservationService.getMerchantReservations(shopId, status, page, size);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a reservation (merchant confirms pickup)
     * POST /api/reservations/validate?shopId={shopId}
     */
    @PostMapping("/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> validateReservation(
        @RequestParam Long shopId,
        @Valid @RequestBody ValidateReservationRequest request,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("POST /api/reservations/validate - merchant: {}, shopId: {}, qrCode: {}", username, shopId, request.getQrCode());

        // Verify merchant owns this shop
        // TODO: Add shop ownership verification
        
        ReservationResponse response = reservationService.validateReservation(shopId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Validate a reservation by ID (for manual confirmation)
     * PUT /api/reservations/{id}/validate
     */
    @PutMapping("/{id}/validate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> validateReservationById(
        @PathVariable String id,
        @RequestParam Long shopId,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("PUT /api/reservations/{}/validate - merchant: {}, shopId: {}", id, username, shopId);

        ReservationResponse response = reservationService.validateReservationById(id, shopId);
        return ResponseEntity.ok(response);
    }

    /**
     * Complete pickup of a reservation
     * PUT /api/reservations/{id}/complete?shopId={shopId}
     */
    @PutMapping("/{id}/complete")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> completePickup(
        @PathVariable String id,
        @RequestParam Long shopId,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("PUT /api/reservations/{}/complete - merchant: {}, shopId: {}", id, username, shopId);

        ReservationResponse response = reservationService.completePickup(shopId, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel a reservation
     * PUT /api/reservations/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReservationResponse> cancelReservation(
        @PathVariable String id,
        Authentication authentication
    ) {
        String username = authentication.getName();
        log.info("PUT /api/reservations/{}/cancel - user: {}", id, username);

        ReservationResponse response = reservationService.cancelReservation(username, id);
        return ResponseEntity.ok(response);
    }

}
