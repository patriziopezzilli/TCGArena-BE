package com.tcg.arena.service;

import com.tcg.arena.dto.MerchantDashboardStatsDTO;
import com.tcg.arena.dto.MerchantNotificationsDTO;
import com.tcg.arena.dto.MerchantNotificationsDTO.NotificationItem;
import com.tcg.arena.dto.MerchantNotificationsDTO.NotificationType;
import com.tcg.arena.model.CustomerRequest;
import com.tcg.arena.model.Reservation;
import com.tcg.arena.model.Tournament;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
            ShopSubscriptionRepository shopSubscriptionRepository) {
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

        long upcomingTournaments = tournamentRepository.countUpcomingTournamentsByOrganizer(shopId,
                LocalDateTime.now());
        log.debug("Upcoming tournaments: {} ({}ms)", upcomingTournaments, System.currentTimeMillis() - startTime);

        long pendingRequests = customerRequestRepository.countByShopIdAndStatus(
                shopId, CustomerRequest.RequestStatus.PENDING);
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
                subscriberCount);
    }

    /**
     * Get actionable notifications for a merchant
     * Includes: today's tournaments, pending requests, active reservations
     */
    @Transactional(readOnly = true)
    public MerchantNotificationsDTO getMerchantNotifications(Long shopId) {
        log.info("Getting notifications for shop: {}", shopId);
        List<NotificationItem> notifications = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        // 1. Today's tournaments (urgent)
        List<Tournament> todaysTournaments = tournamentRepository.findTodaysTournamentsByOrganizer(shopId, now);
        for (Tournament t : todaysTournaments) {
            String time = t.getStartDate().format(timeFormatter);
            notifications.add(new NotificationItem(
                    "tournament_" + t.getId(),
                    NotificationType.TOURNAMENT_TODAY,
                    "Torneo oggi alle " + time,
                    t.getTitle(),
                    "/merchant/tournaments",
                    t.getStartDate(),
                    true));
        }

        // 2. Tournaments starting within 24 hours
        List<Tournament> upcomingTournaments = tournamentRepository.findUpcomingTournamentsByOrganizerWithinHours(
                shopId, now, now.plusHours(24));
        for (Tournament t : upcomingTournaments) {
            // Skip if already in today's list
            if (todaysTournaments.stream().anyMatch(tt -> tt.getId().equals(t.getId()))) {
                continue;
            }
            notifications.add(new NotificationItem(
                    "tournament_upcoming_" + t.getId(),
                    NotificationType.TOURNAMENT_UPCOMING,
                    "Torneo domani",
                    t.getTitle(),
                    "/merchant/tournaments",
                    t.getStartDate(),
                    false));
        }

        // 3. Unread customer requests (uses existing repository method)
        List<CustomerRequest> unreadRequests = customerRequestRepository.findUnreadRequests(shopId);
        for (CustomerRequest r : unreadRequests) {
            notifications.add(new NotificationItem(
                    "request_" + r.getId(),
                    NotificationType.PENDING_REQUEST,
                    "Nuova richiesta",
                    r.getType() != null ? r.getType().getDisplayName() : "Richiesta cliente",
                    "/merchant/requests",
                    r.getCreatedAt(),
                    true));
        }

        // 4. Active reservations (uses existing repository method - merchantId =
        // shopId)
        List<Reservation> activeReservations = reservationRepository.findActiveReservations(shopId);
        if (!activeReservations.isEmpty()) {
            notifications.add(new NotificationItem(
                    "reservations_active",
                    NotificationType.ACTIVE_RESERVATION,
                    activeReservations.size() + " prenotazioni attive",
                    "Da validare",
                    "/merchant/reservations",
                    now,
                    true));
        }

        log.info("Found {} notifications for shop {}", notifications.size(), shopId);
        return new MerchantNotificationsDTO(notifications);
    }
}