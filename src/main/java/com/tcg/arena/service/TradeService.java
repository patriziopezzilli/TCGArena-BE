package com.tcg.arena.service;

import com.tcg.arena.dto.TradeMatchDTO;
import com.tcg.arena.model.*;
import com.tcg.arena.repository.CardTemplateRepository;
import com.tcg.arena.repository.TradeListEntryRepository;
import com.tcg.arena.repository.TradeMatchRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class TradeService {

    @Autowired
    private TradeListEntryRepository tradeListEntryRepository;

    @Autowired
    private TradeMatchRepository tradeMatchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CardTemplateRepository cardTemplateRepository;

    @Transactional
    public void addCardToList(Long userId, Long cardTemplateId, TradeListType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if already exists
        Optional<TradeListEntry> existing = tradeListEntryRepository.findByUserAndCardTemplateIdAndType(user, cardTemplateId, type);
        if (existing.isPresent()) {
            return;
        }

        CardTemplate cardTemplate = cardTemplateRepository.findById(cardTemplateId)
                .orElseThrow(() -> new RuntimeException("Card not found"));

        TradeListEntry entry = new TradeListEntry();
        entry.setUser(user);
        entry.setCardTemplate(cardTemplate);
        entry.setType(type);
        tradeListEntryRepository.save(entry);
    }

    @Transactional
    public void removeCardFromList(Long userId, Long cardTemplateId, TradeListType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Optional<TradeListEntry> existing = tradeListEntryRepository.findByUserAndCardTemplateIdAndType(user, cardTemplateId, type);
        existing.ifPresent(tradeListEntryRepository::delete);
    }

    public List<TradeMatchDTO> findMatches(Long userId, double radiusKm) {
        User currentUser = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (currentUser.getLocation() == null || currentUser.getLocation().getLatitude() == null) {
            return Collections.emptyList();
        }

        List<TradeListEntry> myEntries = tradeListEntryRepository.findByUser(currentUser);
        Set<Long> myWantIds = myEntries.stream()
                .filter(e -> e.getType() == TradeListType.WANT)
                .map(e -> e.getCardTemplate().getId())
                .collect(Collectors.toSet());
        
        Set<Long> myHaveIds = myEntries.stream()
                .filter(e -> e.getType() == TradeListType.HAVE)
                .map(e -> e.getCardTemplate().getId())
                .collect(Collectors.toSet());

        Map<User, List<String>> matchesMap = new HashMap<>();
        Map<User, String> matchTypeMap = new HashMap<>();

        // 1. Find users who HAVE what I WANT
        if (!myWantIds.isEmpty()) {
            // Ideally use a custom query here: SELECT e FROM TradeListEntry e WHERE e.type = HAVE AND e.cardTemplate.id IN :ids
            // For now, let's fetch all entries of type HAVE (might be large, optimization needed later)
            // Or better:
            List<TradeListEntry> potentialMatches = tradeListEntryRepository.findAll().stream()
                    .filter(e -> e.getType() == TradeListType.HAVE)
                    .filter(e -> myWantIds.contains(e.getCardTemplate().getId()))
                    .filter(e -> !e.getUser().getId().equals(userId))
                    .collect(Collectors.toList());

            for (TradeListEntry entry : potentialMatches) {
                User otherUser = entry.getUser();
                if (isWithinRadius(currentUser, otherUser, radiusKm)) {
                    matchesMap.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(entry.getCardTemplate().getName());
                    matchTypeMap.put(otherUser, "THEY_HAVE_WHAT_I_WANT");
                }
            }
        }

        // 2. Find users who WANT what I HAVE
        if (!myHaveIds.isEmpty()) {
            List<TradeListEntry> potentialMatches = tradeListEntryRepository.findAll().stream()
                    .filter(e -> e.getType() == TradeListType.WANT)
                    .filter(e -> myHaveIds.contains(e.getCardTemplate().getId()))
                    .filter(e -> !e.getUser().getId().equals(userId))
                    .collect(Collectors.toList());

            for (TradeListEntry entry : potentialMatches) {
                User otherUser = entry.getUser();
                if (isWithinRadius(currentUser, otherUser, radiusKm)) {
                    matchesMap.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(entry.getCardTemplate().getName());
                    
                    String currentType = matchTypeMap.get(otherUser);
                    if (currentType != null && currentType.equals("THEY_HAVE_WHAT_I_WANT")) {
                        matchTypeMap.put(otherUser, "BOTH");
                    } else {
                        matchTypeMap.put(otherUser, "I_HAVE_WHAT_THEY_WANT");
                    }
                }
            }
        }

        // Convert to DTOs
        List<TradeMatchDTO> results = new ArrayList<>();
        for (Map.Entry<User, List<String>> entry : matchesMap.entrySet()) {
            User otherUser = entry.getKey();
            List<String> cards = entry.getValue();
            String type = matchTypeMap.get(otherUser);
            double distance = calculateDistance(currentUser, otherUser);

            // Ensure persistent match exists
            TradeMatch match = createOrGetMatch(currentUser, otherUser);

            TradeMatchDTO dto = new TradeMatchDTO();
            dto.setId(match.getId());
            dto.setOtherUserId(otherUser.getId());
            dto.setOtherUserName(otherUser.getUsername()); // Or display name
            dto.setOtherUserAvatar("person.crop.circle"); // Placeholder
            dto.setDistance(distance);
            dto.setMatchedCards(cards.stream().distinct().collect(Collectors.toList()));
            dto.setType(type);
            
            results.add(dto);
        }
        
        return results;
    }

    private TradeMatch createOrGetMatch(User u1, User u2) {
        TradeMatch match = tradeMatchRepository.findByUsers(u1, u2);
        if (match == null) {
            match = new TradeMatch();
            match.setUser1(u1);
            match.setUser2(u2);
            match.setDistance(calculateDistance(u1, u2));
            match.setStatus(TradeStatus.ACTIVE);
            tradeMatchRepository.save(match);
        }
        return match;
    }

    public List<TradeListEntry> getUserList(Long userId, TradeListType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tradeListEntryRepository.findByUserAndType(user, type);
    }

    private boolean isWithinRadius(User u1, User u2, double radiusKm) {
        if (u2.getLocation() == null || u2.getLocation().getLatitude() == null) return false;
        double distance = calculateDistance(u1, u2);
        return distance <= radiusKm * 1000; // radiusKm to meters
    }

    private double calculateDistance(User u1, User u2) {
        double lat1 = u1.getLocation().getLatitude();
        double lon1 = u1.getLocation().getLongitude();
        double lat2 = u2.getLocation().getLatitude();
        double lon2 = u2.getLocation().getLongitude();

        // Haversine formula
        double R = 6371e3; // metres
        double phi1 = lat1 * Math.PI / 180;
        double phi2 = lat2 * Math.PI / 180;
        double deltaPhi = (lat2 - lat1) * Math.PI / 180;
        double deltaLambda = (lon2 - lon1) * Math.PI / 180;

        double a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                   Math.cos(phi1) * Math.cos(phi2) *
                   Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    @Autowired
    private com.tcg.arena.repository.TradeMessageRepository tradeMessageRepository;

    @Transactional
    public void sendMessage(Long matchId, Long senderId, String content) {
        TradeMatch match = tradeMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verify user is part of the match
        if (!match.getUser1().getId().equals(senderId) && !match.getUser2().getId().equals(senderId)) {
            throw new RuntimeException("User not part of this trade match");
        }

        TradeMessage message = new TradeMessage();
        message.setMatch(match);
        message.setSender(sender);
        message.setContent(content);
        tradeMessageRepository.save(message);
    }

    public List<com.tcg.arena.dto.TradeMessageDTO> getMessages(Long matchId, Long currentUserId) {
        TradeMatch match = tradeMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        // Verify user is part of the match
        if (!match.getUser1().getId().equals(currentUserId) && !match.getUser2().getId().equals(currentUserId)) {
            throw new RuntimeException("User not part of this trade match");
        }

        return tradeMessageRepository.findByMatchIdOrderBySentAtAsc(matchId).stream()
                .map(msg -> {
                    com.tcg.arena.dto.TradeMessageDTO dto = new com.tcg.arena.dto.TradeMessageDTO();
                    dto.setId(msg.getId());
                    dto.setContent(msg.getContent());
                    dto.setSenderId(msg.getSender().getId());
                    dto.setSenderName(msg.getSender().getUsername());
                    dto.setSentAt(msg.getSentAt());
                    dto.setCurrentUser(msg.getSender().getId().equals(currentUserId));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void completeTrade(Long matchId, Long userId) {
        TradeMatch match = tradeMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (!match.getUser1().getId().equals(userId) && !match.getUser2().getId().equals(userId)) {
            throw new RuntimeException("User not part of this trade match");
        }
        
        match.setStatus(TradeStatus.COMPLETED);
        tradeMatchRepository.save(match);
    }

    @Transactional
    public void cancelTrade(Long matchId, Long userId) {
        TradeMatch match = tradeMatchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        
        if (!match.getUser1().getId().equals(userId) && !match.getUser2().getId().equals(userId)) {
            throw new RuntimeException("User not part of this trade match");
        }
        
        match.setStatus(TradeStatus.CANCELLED);
        tradeMatchRepository.save(match);
    }
}
