package com.tcg.arena.service;

import com.tcg.arena.dto.ReservationDTO.*;
import com.tcg.arena.model.User;
import com.tcg.arena.model.InventoryCard;
import com.tcg.arena.model.Reservation;
import com.tcg.arena.model.Shop;
import com.tcg.arena.repository.ShopRepository;
import com.tcg.arena.repository.ReservationRepository;
import com.tcg.arena.service.InventoryCardService;
import com.tcg.arena.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationService.class);

    private final ReservationRepository reservationRepository;
    private final InventoryCardService inventoryCardService;
    private final UserService userService;
    private final ShopRepository shopRepository;
    private final RewardService rewardService;
    private final NotificationService notificationService;

    public ReservationService(ReservationRepository reservationRepository,
            InventoryCardService inventoryCardService,
            UserService userService,
            ShopRepository shopRepository,
            RewardService rewardService,
            NotificationService notificationService) {
        this.reservationRepository = reservationRepository;
        this.inventoryCardService = inventoryCardService;
        this.userService = userService;
        this.shopRepository = shopRepository;
        this.rewardService = rewardService;
        this.notificationService = notificationService;
    }

    @Transactional
    public ReservationResponse createReservation(
            String username,
            CreateReservationRequest request) {
        log.info("Creating reservation for user: {} card: {}", username, request.getCardId());

        // Get user by username
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Get inventory card and validate availability
        InventoryCard inventoryCard = inventoryCardService.getInventoryCard(request.getCardId());

        if (inventoryCard.getQuantity() < request.getQuantity()) {
            throw new RuntimeException("Insufficient quantity available");
        }

        // Get shop to retrieve reservation duration setting
        Shop shop = shopRepository.findById(inventoryCard.getShopId())
                .orElseThrow(() -> new RuntimeException("Shop not found"));

        int reservationDurationMinutes = shop.getReservationDurationMinutes() != null
                ? shop.getReservationDurationMinutes()
                : 30; // Default to 30 minutes if not set

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setCardId(request.getCardId());
        reservation.setUserId(user.getId());
        reservation.setMerchantId(inventoryCard.getShopId());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation.setQrCode(UUID.randomUUID().toString());
        reservation.setExpiresAt(LocalDateTime.now().plusMinutes(reservationDurationMinutes));

        // Decrease inventory quantity
        inventoryCardService.updateQuantity(request.getCardId(), -request.getQuantity());

        Reservation saved = reservationRepository.save(reservation);

        // Award points for reservation (+10 points)
        rewardService.earnPoints(user.getId(), 10, "Prenotazione presso " + shop.getName());

        return new ReservationResponse(
                saved,
                String.format("Prenotazione creata con successo. Completa il ritiro entro %d minuti.",
                        reservationDurationMinutes));
    }

    /**
     * Get user's reservations
     */
    @Transactional(readOnly = true)
    public ReservationListResponse getUserReservations(
            String username,
            int page,
            int size) {
        return getUserReservations(username, null, page, size);
    }

    public ReservationListResponse getUserReservations(
            String username,
            Long shopId,
            int page,
            int size) {
        log.info("Getting reservations for user: {} with shop filter: {}", username, shopId);

        // Get user by username
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reservation> reservationPage;

        if (shopId != null) {
            // Filter by both user and shop
            reservationPage = reservationRepository.findByUserIdAndMerchantId(user.getId(), shopId, pageable);
        } else {
            // Get all user's reservations
            reservationPage = reservationRepository.findByUserId(user.getId(), pageable);
        }

        // Convert to DTOs
        List<ReservationSummaryDTO> reservationDTOs = reservationPage.getContent().stream()
                .map(ReservationSummaryDTO::new)
                .toList();

        return new ReservationListResponse(
                reservationDTOs,
                (int) reservationPage.getTotalElements(),
                page,
                size);
    }

    /**
     * Get reservations by card ID (for availability checking)
     */
    @Transactional(readOnly = true)
    public ReservationListResponse getReservationsByCardId(
            String cardId,
            String merchantId,
            int page,
            int size) {
        log.info("Getting reservations for card: {}, merchant: {}", cardId, merchantId);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reservation> reservationPage;

        if (merchantId != null && !merchantId.isEmpty()) {
            // Filter by both cardId and merchantId
            reservationPage = reservationRepository.findByCardIdAndMerchantId(cardId, Long.parseLong(merchantId),
                    pageable);
        } else {
            // Filter only by cardId
            reservationPage = reservationRepository.findByCardId(cardId, pageable);
        }

        // Convert to DTOs
        List<ReservationSummaryDTO> reservationDTOs = reservationPage.getContent().stream()
                .map(ReservationSummaryDTO::new)
                .toList();

        return new ReservationListResponse(
                reservationDTOs,
                (int) reservationPage.getTotalElements(),
                page,
                size);
    }

    /**
     * Get merchant's reservations
     */
    @Transactional(readOnly = true)
    public ReservationListResponse getMerchantReservations(
            Long shopId,
            Reservation.ReservationStatus status,
            int page,
            int size) {
        log.info("Getting reservations for shop: {} with status: {}", shopId, status);

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Reservation> reservationPage;

        if (status == null) {
            reservationPage = reservationRepository.findByMerchantId(shopId, pageable);
        } else {
            reservationPage = reservationRepository.findByMerchantIdAndStatus(shopId, status, pageable);
        }

        // Convert to DTOs
        List<ReservationSummaryDTO> reservationDTOs = reservationPage.getContent().stream()
                .map(ReservationSummaryDTO::new)
                .toList();

        return new ReservationListResponse(
                reservationDTOs,
                (int) reservationPage.getTotalElements(),
                page,
                size);
    }

    /**
     * Validate reservation with QR code
     */
    @Transactional
    public ReservationResponse validateReservation(
            Long shopId,
            ValidateReservationRequest request) {
        log.info("Validating reservation with QR code: {}", request.getQrCode());

        Reservation reservation = reservationRepository.findByQrCode(request.getQrCode())
                .orElseThrow(() -> new RuntimeException("Invalid QR code"));

        // Verify merchant
        if (!reservation.getMerchantId().equals(shopId)) {
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

        // Send notification to user
        try {
            InventoryCard card = inventoryCardService.getInventoryCard(reservation.getCardId());
            Shop shop = shopRepository.findById(shopId).orElse(null);
            String cardName = (card != null && card.getCardTemplate() != null) ? card.getCardTemplate().getName()
                    : "carta";
            String shopName = shop != null ? shop.getName() : "negozio";
            notificationService.sendReservationValidatedNotification(reservation.getUserId(), cardName, shopName);
        } catch (Exception e) {
            log.warn("Failed to send validation notification: {}", e.getMessage());
        }

        return new ReservationResponse(
                saved,
                "Prenotazione validata con successo");
    }

    /**
     * Validate reservation by ID (for manual confirmation)
     */
    @Transactional
    public ReservationResponse validateReservationById(
            String reservationId,
            Long shopId) {
        log.info("Validating reservation by ID: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Verify merchant
        if (!reservation.getMerchantId().equals(shopId)) {
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

        // Send notification to user
        try {
            InventoryCard card = inventoryCardService.getInventoryCard(reservation.getCardId());
            Shop shop = shopRepository.findById(shopId).orElse(null);
            String cardName = (card != null && card.getCardTemplate() != null) ? card.getCardTemplate().getName()
                    : "carta";
            String shopName = shop != null ? shop.getName() : "negozio";
            notificationService.sendReservationValidatedNotification(reservation.getUserId(), cardName, shopName);
        } catch (Exception e) {
            log.warn("Failed to send validation notification: {}", e.getMessage());
        }

        return new ReservationResponse(
                saved,
                "Prenotazione validata con successo");
    }

    /**
     * Complete pickup
     */
    @Transactional
    public ReservationResponse completePickup(
            Long shopId,
            String reservationId) {
        log.info("Completing pickup for reservation: {}", reservationId);

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Verify merchant
        if (!reservation.getMerchantId().equals(shopId)) {
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
                "Ritiro completato con successo");
    }

    /**
     * Cancel reservation
     */
    @Transactional
    public ReservationResponse cancelReservation(
            String username,
            String reservationId) {
        log.info("Cancelling reservation: {} by user: {}", reservationId, username);

        // Get user by username
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        // Verify user
        if (!reservation.getUserId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        // Verify can be cancelled
        if (!reservation.canBeCancelled()) {
            throw new RuntimeException("Reservation cannot be cancelled (status: " + reservation.getStatus() + ")");
        }

        // Restore inventory quantity
        inventoryCardService.updateQuantity(reservation.getCardId(), 1);

        // Update status
        reservation.setStatus(Reservation.ReservationStatus.CANCELLED);

        Reservation saved = reservationRepository.save(reservation);

        return new ReservationResponse(
                saved,
                "Prenotazione annullata con successo");
    }

    /**
     * Get reservation statistics
     */
    @Transactional(readOnly = true)
    public ReservationStatsResponse getReservationStats(String merchantId) {
        log.info("Getting reservation stats for merchant: {}", merchantId);

        long pendingCount = reservationRepository.findByMerchantIdAndStatus(
                Long.valueOf(merchantId),
                Reservation.ReservationStatus.PENDING,
                Pageable.unpaged()).getTotalElements();

        long validatedCount = reservationRepository.findByMerchantIdAndStatus(
                Long.valueOf(merchantId),
                Reservation.ReservationStatus.VALIDATED,
                Pageable.unpaged()).getTotalElements();

        long pickedUpCount = reservationRepository.findByMerchantIdAndStatus(
                Long.valueOf(merchantId),
                Reservation.ReservationStatus.PICKED_UP,
                Pageable.unpaged()).getTotalElements();

        LocalDateTime thresholdTime = LocalDateTime.now().plusMinutes(5);
        long expiringSoonCount = reservationRepository.findExpiringSoon(Long.valueOf(merchantId), thresholdTime).size();

        return new ReservationStatsResponse(
                pendingCount,
                validatedCount,
                pickedUpCount,
                expiringSoonCount);
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
