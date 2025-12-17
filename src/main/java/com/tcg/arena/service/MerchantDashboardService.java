package com.tcg.arena.service;

import com.tcg.arena.dto.MerchantDashboardStatsDTO;
import com.tcg.arena.model.CustomerRequest;
import com.tcg.arena.repository.CustomerRequestRepository;
import com.tcg.arena.repository.InventoryCardRepository;
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

    private final InventoryCardRepository inventoryCardRepository;
    private final ReservationRepository reservationRepository;
    private final TournamentRepository tournamentRepository;
    private final CustomerRequestRepository customerRequestRepository;
    private final ShopSubscriptionRepository shopSubscriptionRepository;

    public MerchantDashboardService(
        InventoryCardRepository inventoryCardRepository,
        ReservationRepository reservationRepository,
        TournamentRepository tournamentRepository,
        CustomerRequestRepository customerRequestRepository,
        ShopSubscriptionRepository shopSubscriptionRepository
    ) {
        this.inventoryCardRepository = inventoryCardRepository;
        this.reservationRepository = reservationRepository;
        this.tournamentRepository = tournamentRepository;
        this.customerRequestRepository = customerRequestRepository;
        this.shopSubscriptionRepository = shopSubscriptionRepository;
    }

    /**
     * Get dashboard statistics for a merchant
     * Optimized with COUNT queries instead of loading entities
     */
    @Transactional(readOnly = true)
    public MerchantDashboardStatsDTO getDashboardStats(Long shopId) {
        log.info("Getting dashboard stats for shop: {}", shopId);
        long startTime = System.currentTimeMillis();

        // Use optimized COUNT queries instead of loading entities
        long inventoryCount = inventoryCardRepository.countByShopId(shopId);
        log.debug("Inventory count: {} ({}ms)", inventoryCount, System.currentTimeMillis() - startTime);

        long activeReservations = reservationRepository.countActiveReservations(shopId);
        log.debug("Active reservations: {} ({}ms)", activeReservations, System.currentTimeMillis() - startTime);

        long upcomingTournaments = tournamentRepository.countUpcomingTournamentsByOrganizer(shopId, LocalDateTime.now());
        log.debug("Upcoming tournaments: {} ({}ms)", upcomingTournaments, System.currentTimeMillis() - startTime);

        long pendingRequests = customerRequestRepository.countByShopIdAndStatus(
            shopId, CustomerRequest.RequestStatus.PENDING
        );
        log.debug("Pending requests: {} ({}ms)", pendingRequests, System.currentTimeMillis() - startTime);

        long subscriberCount = shopSubscriptionRepository.countActiveSubscriptionsByShopId(shopId);
        log.debug("Subscriber count: {} ({}ms)", subscriberCount, System.currentTimeMillis() - startTime);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("Dashboard stats retrieved in {}ms", totalTime);

        return new MerchantDashboardStatsDTO(
            inventoryCount,
            activeReservations,
            upcomingTournaments,
            pendingRequests,
            subscriberCount
        );
    }
}