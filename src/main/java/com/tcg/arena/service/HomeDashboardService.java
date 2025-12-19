package com.tcg.arena.service;

import com.tcg.arena.dto.HomeDashboardDTO;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserCard;
import com.tcg.arena.model.Card;
import com.tcg.arena.model.Tournament;
import com.tcg.arena.model.CustomerRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class HomeDashboardService {

    @Autowired
    private ShopService shopService;

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserCardService userCardService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private CustomerRequestService customerRequestService;

    @Autowired
    private ShopNewsService shopNewsService;

    public HomeDashboardDTO getDashboardData(User user, Double latitude, Double longitude) {
        HomeDashboardDTO dashboard = new HomeDashboardDTO();

        // 1. Nearby Shops (within 20km)
        long nearbyShopsCount = 0;
        if (latitude != null && longitude != null) {
            List<Shop> allShops = shopService.getAllShops();
            nearbyShopsCount = allShops.stream()
                    .filter(shop -> {
                        if (shop.getLatitude() == null || shop.getLongitude() == null)
                            return false;
                        double distance = calculateDistance(latitude, longitude, shop.getLatitude(),
                                shop.getLongitude());
                        return distance <= 20.0; // 20km radius
                    })
                    .count();
        }
        dashboard.setNearbyShopsCount(nearbyShopsCount);

        // 2. Upcoming Tournaments (nearby, within 50km or generic upcoming)
        long upcomingTournamentsCount = 0;
        if (latitude != null && longitude != null) {
            List<Tournament> nearbyTournaments = tournamentService.getNearbyTournaments(latitude, longitude, 50.0); // 50km
                                                                                                                    // for
                                                                                                                    // tournaments
            upcomingTournamentsCount = nearbyTournaments.size();
        } else {
            List<Tournament> upcoming = tournamentService.getUpcomingTournaments();
            upcomingTournamentsCount = upcoming.size();
        }
        dashboard.setUpcomingTournamentsCount(upcomingTournamentsCount);

        // 3. Collection Stats
        List<UserCard> userCards = userCardService.getUserCardsByUserId(user.getId());
        dashboard.setCollectionCount(userCards.size());

        BigDecimal totalValue = BigDecimal.ZERO;
        for (UserCard card : userCards) {
            if (card.getCardTemplate() != null && card.getCardTemplate().getMarketPrice() != null) {
                totalValue = totalValue.add(BigDecimal.valueOf(card.getCardTemplate().getMarketPrice()));
            }
        }
        dashboard.setTotalCollectionValue(totalValue);

        // 4. Pending Reservations
        // Using page size 1 just to get the total count from the response
        try {
            var reservations = reservationService.getUserReservations(user.getUsername(), 0, 1);
            dashboard.setPendingReservationsCount(reservations.getTotalElements());
        } catch (Exception e) {
            dashboard.setPendingReservationsCount(0);
        }

        // 5. Active Requests
        try {
            var requests = customerRequestService.getRequests(null, String.valueOf(user.getId()), null, null, 0, 1);
            dashboard.setActiveRequestsCount(requests.getTotalElements());
        } catch (Exception e) {
            dashboard.setActiveRequestsCount(0);
        }

        // 6. News (Placeholder: Sum of active news from all shops for now, as we don't
        // have "following" logic easily accessible)
        // In a real scenario, we'd filter by shops the user follows.
        // For MVP, let's just count active news from nearby shops if location is
        // available
        long unreadNewsCount = 0;
        if (latitude != null && longitude != null) {
            List<Shop> allShops = shopService.getAllShops();
            for (Shop shop : allShops) {
                if (shop.getLatitude() != null && shop.getLongitude() != null) {
                    double distance = calculateDistance(latitude, longitude, shop.getLatitude(), shop.getLongitude());
                    if (distance <= 20.0) {
                        unreadNewsCount += shopNewsService.getActiveNews(shop.getId()).size();
                    }
                }
            }
        }
        dashboard.setUnreadNewsCount(unreadNewsCount);

        return dashboard;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;
        return distance;
    }
}
