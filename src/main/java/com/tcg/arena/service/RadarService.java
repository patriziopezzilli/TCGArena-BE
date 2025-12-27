package com.tcg.arena.service;

import com.tcg.arena.dto.LocationUpdateRequest;
import com.tcg.arena.dto.*;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.TradeListEntryRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RadarService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradeListEntryRepository tradeListEntryRepository;

    @Autowired
    private DeckRepository deckRepository;

    // Update user location
    public void updateUserLocation(Long userId, LocationUpdateRequest request) {
        userRepository.findById(userId).ifPresent(user -> {
            UserLocation location = user.getLocation();
            if (location == null) {
                location = new UserLocation();
            }
            location.setLatitude(request.getLatitude());
            location.setLongitude(request.getLongitude());
            location.setCity(request.getCity());
            location.setCountry(request.getCountry());
            user.setLocation(location);
            // Optionally update "last active" timestamp in User entity if available
            userRepository.save(user);
        });
    }

    // Get nearby users
    public List<RadarUserDto> getNearbyUsers(Long currentUserId, double latitude, double longitude, double radiusKm) {
        // Fetch users who have a location set
        // In a real app we would use a bounding box query here
        List<User> users = userRepository.findAll();

        List<RadarUserDto> nearbyUsers = users.stream()
                .filter(user -> !user.getId().equals(currentUserId)) // Exclude self
                .filter(user -> user.getLocation() != null && user.getLocation().getLatitude() != null
                        && user.getLocation().getLongitude() != null)
                .filter(user -> {
                    double dist = calculateDistance(latitude, longitude, user.getLocation().getLatitude(),
                            user.getLocation().getLongitude());
                    return dist <= radiusKm;
                })
                .map(this::convertToRadarDto)
                .collect(Collectors.toCollection(ArrayList::new)); // Use ArrayList to allow modification

        // MOCK DATA: Inject fake users for testing/demo purposes if list is empty or
        // minimal
        // This ensures the user sees specific "Ghost" users to Verify the feature
        if (nearbyUsers.isEmpty()) {
            nearbyUsers.add(createMockUser(99991L, latitude + 0.002, longitude + 0.002, "Ghost Trainer A"));
            nearbyUsers.add(createMockUser(99992L, latitude - 0.003, longitude + 0.001, "Ghost Trainer B"));
            nearbyUsers.add(createMockUser(99993L, latitude + 0.001, longitude - 0.003, "Ghost Trainer C"));
        }

        return nearbyUsers;
    }

    private RadarUserDto createMockUser(Long id, double lat, double lon, String displayName) {
        RadarUserDto dto = new RadarUserDto();
        dto.setId(id);
        dto.setUsername("Ghost");
        dto.setDisplayName(displayName);
        dto.setLatitude(lat);
        dto.setLongitude(lon);
        dto.setOnline(true);
        dto.setFavoriteTCG(com.tcg.arena.model.TCGType.POKEMON);
        return dto;
    }

    // Send Ping
    public void sendPing(Long fromUserId, Long toUserId) {
        userRepository.findById(fromUserId).ifPresent(fromUser -> {
            // Logic to send push notification via NotificationService
            // notificationService.sendPing(toUserId, fromUser.getDisplayName());
            System.out.println("PING from " + fromUserId + " to " + toUserId);
        });
    }

    private RadarUserDto convertToRadarDto(User user) {
        RadarUserDto dto = new RadarUserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setProfileImageUrl(user.getProfileImageUrl());

        if (user.getLocation() != null) {
            dto.setLatitude(user.getLocation().getLatitude());
            dto.setLongitude(user.getLocation().getLongitude());
        }

        if (user.getFavoriteTCGTypes() != null && !user.getFavoriteTCGTypes().isEmpty()) {
            dto.setFavoriteTCG(user.getFavoriteTCGTypes().get(0));
        } else {
            dto.setFavoriteTCG(TCGType.MAGIC);
        }
        dto.setOnline(true);

        // Fetch trade lists
        dto.setWantList(tradeListEntryRepository.findByUserAndType(user, TradeListType.WANT)
                .stream().map(this::toRadarTradeEntry).collect(Collectors.toList()));
        dto.setHaveList(tradeListEntryRepository.findByUserAndType(user, TradeListType.HAVE)
                .stream().map(this::toRadarTradeEntry).collect(Collectors.toList()));

        // Fetch cards from LISTA type decks
        dto.setCards(deckRepository.findByOwnerIdOrderByDateCreatedDesc(user.getId())
                .stream()
                .filter(deck -> deck.getDeckType() == DeckType.LISTA)
                .flatMap(deck -> deck.getCards().stream())
                .map(this::toRadarUserCard)
                .collect(Collectors.toList()));

        return dto;
    }

    private RadarTradeEntry toRadarTradeEntry(TradeListEntry entry) {
        RadarTradeEntry dto = new RadarTradeEntry();
        dto.setId(entry.getId());
        dto.setCardTemplateId(entry.getCardTemplate().getId());
        dto.setCardName(entry.getCardTemplate().getName());
        dto.setImageUrl(entry.getCardTemplate().getImageUrl());
        dto.setTcgType(entry.getCardTemplate().getTcgType());
        dto.setRarity(entry.getCardTemplate().getRarity() != null ? entry.getCardTemplate().getRarity().name() : null);
        return dto;
    }

    private RadarUserCard toRadarUserCard(DeckCard card) {
        RadarUserCard dto = new RadarUserCard();
        dto.setCardId(card.getCardId());
        dto.setCardName(card.getCardName());
        dto.setImageUrl(card.getCardImageUrl());
        dto.setQuantity(card.getQuantity());
        dto.setCondition(card.getCondition().name());
        dto.setRarity(card.getRarity());
        dto.setSetName(card.getSetName());
        if (card.getCardTemplate() != null) {
            dto.setTcgType(card.getCardTemplate().getTcgType());
        }
        return dto;
    }

    // Haversine formula
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
