package com.tcg.arena.service;

import com.tcg.arena.dto.MerchantDashboardStatsDTO;
import com.tcg.arena.model.CustomerRequest;
import com.tcg.arena.repository.CustomerRequestRepository;
import com.tcg.arena.repository.ReservationRepository;
import com.tcg.arena.repository.ShopSubscriptionRepository;
import com.tcg.arena.repository.TournamentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MerchantDashboardService {

    private static final Logger log = LoggerFactory.getLogger(MerchantDashboardService.class);

    private final InventoryCardService inventoryCardService;
    private final ReservationRepository reservationRepository;
    private final TournamentRepository tournamentRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final ShopSubscriptionRepository shopSubscriptionRepository;

    public MerchantDashboardService(
        InventoryCardService inventoryCardService,
        ReservationRepository reservationRepository,
        TournamentRepository tournamentRepository,
        CustomerRequestRepository customerRequestRepository,
        ShopSubscriptionRepository shopSubscriptionRepository
    ) {
        this.inventoryCardService = inventoryCardService;
        this.reservationRepository = reservationRepository;
        this.tournamentRepository = tournamentRepository;
        this.customerRequestRepository = customerRequestRepository;
        this.shopSubscriptionRepository = shopSubscriptionRepository;
    }

    /**
     * Get dashboard statistics for a merchant
     */
    @Transactional(readOnly = true)
    public MerchantDashboardStatsDTO getDashboardStats(Long shopId) {
        log.info("Getting dashboard stats for shop: {}", shopId);

        // Get inventory count from existing service
        long inventoryCount = inventoryCardService.getInventoryStats(shopId).getTotalItems();

        // Get active reservations count
        long activeReservations = reservationRepository.findActiveReservations(shopId).size();

        // Get upcoming tournaments count
        long upcomingTournaments = tournamentRepository.findByOrganizerId(shopId).stream()
            .filter(t -> t.getStartDate().isAfter(LocalDateTime.now()))
            .count();

        // Get pending requests count
        long pendingRequests = customerRequestRepository.countByShopIdAndStatus(
            shopId, CustomerRequest.RequestStatus.PENDING
        );

        // Get subscriber count
        long subscriberCount = shopSubscriptionRepository.countActiveSubscriptionsByShopId(shopId);

        return new MerchantDashboardStatsDTO(
            inventoryCount,
            activeReservations,
            upcomingTournaments,
            pendingRequests,
            subscriberCount
        );
    }
}