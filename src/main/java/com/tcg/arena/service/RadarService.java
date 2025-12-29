package com.tcg.arena.service;

import com.tcg.arena.dto.LocationUpdateRequest;
import com.tcg.arena.dto.*;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.TradeListEntryRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
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
                .collect(Collectors.toList());

        return nearbyUsers;
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
        List<TradeListEntry> wantEntries = tradeListEntryRepository.findByUserAndType(user, TradeListType.WANT);
        List<TradeListEntry> haveEntries = tradeListEntryRepository.findByUserAndType(user, TradeListType.HAVE);
        System.out.println("🔍 RadarService: User " + user.getId() + " - WANT entries: " + wantEntries.size()
                + ", HAVE entries: " + haveEntries.size());

        dto.setWantList(wantEntries.stream().map(this::toRadarTradeEntry).collect(Collectors.toList()));
        dto.setHaveList(haveEntries.stream().map(this::toRadarTradeEntry).collect(Collectors.toList()));

        // Fetch cards from ALL decks (no public filter per user request)
        List<Deck> allDecks = deckRepository.findByOwnerIdOrderByDateCreatedDesc(user.getId());

        System.out.println("🔍 RadarService: User " + user.getId() + " - Total decks: " + allDecks.size());

        Map<Long, RadarUserCard> cardMap = new HashMap<>();
        int totalCards = 0;
        for (Deck deck : allDecks) {
            totalCards += deck.getCards().size();
            for (DeckCard card : deck.getCards()) {
                RadarUserCard existing = cardMap.get(card.getCardId());
                if (existing != null) {
                    existing.setQuantity(existing.getQuantity() + card.getQuantity());
                } else {
                    cardMap.put(card.getCardId(), toRadarUserCard(card));
                }
            }
        }
        System.out.println("🔍 RadarService: User " + user.getId() + " - Total cards from decks: " + totalCards
                + ", Unique cards: " + cardMap.size());

        dto.setCards(new ArrayList<>(cardMap.values()));

        // Trade Rating Statistics
        dto.setTradeRating(user.getTradeRating());
        dto.setTradeRatingCount(user.getTradeRatingCount());

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
