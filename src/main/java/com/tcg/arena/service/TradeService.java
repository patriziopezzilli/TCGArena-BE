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

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private RewardService rewardService;

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

        Map<User, List<TradeListEntry>> matchesMap = new HashMap<>();
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
                    matchesMap.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(entry);
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
                    matchesMap.computeIfAbsent(otherUser, k -> new ArrayList<>()).add(entry);
                    
                    String currentType = matchTypeMap.get(otherUser);
                    if (currentType != null && currentType.equals("THEY_HAVE_WHAT_I_WANT")) {
                        matchTypeMap.put(otherUser, "BOTH");
                    } else {
                        matchTypeMap.put(otherUser, "I_HAVE_WHAT_THEY_WANT");
                    }
                }
            }
        }

        // 3. Include existing matches with history (even if no current card match)
        List<TradeMatch> existingMatches = tradeMatchRepository.findAllMatchesForUser(currentUser);
        for (TradeMatch match : existingMatches) {
            User otherUser = match.getUser1().equals(currentUser) ? match.getUser2() : match.getUser1();
            
            // If already found by radar, skip (it's already in matchesMap)
            if (matchesMap.containsKey(otherUser)) {
                continue;
            }

            // If not found by radar, check if it's worth keeping (has messages or is COMPLETED/CANCELLED)
            boolean hasMessages = tradeMessageRepository.existsByMatchId(match.getId());
            boolean isRelevant = hasMessages || match.getStatus() == TradeStatus.COMPLETED || match.getStatus() == TradeStatus.CANCELLED;

            if (isRelevant) {
                matchesMap.put(otherUser, Collections.emptyList()); // No current matched cards
                matchTypeMap.put(otherUser, "HISTORY");
            }
        }

        // Convert to DTOs
        List<TradeMatchDTO> results = new ArrayList<>();
        for (Map.Entry<User, List<TradeListEntry>> entry : matchesMap.entrySet()) {
            User otherUser = entry.getKey();
            List<TradeListEntry> entries = entry.getValue();
            String type = matchTypeMap.get(otherUser);
            double distance = calculateDistance(currentUser, otherUser);

            // Ensure persistent match exists
            TradeMatch match = createOrGetMatch(currentUser, otherUser);

            // If we have actual cards to trade (not just history) and the match was closed, reactivate it
            if (!entries.isEmpty() && match.getStatus() != TradeStatus.ACTIVE) {
                match.setStatus(TradeStatus.ACTIVE);
                tradeMatchRepository.save(match);
                
                // Add a separator message to indicate new negotiation
                try {
                    sendMessage(match.getId(), currentUser.getId(), "🔄 Nuova trattativa iniziata");
                } catch (Exception e) {
                    // Ignore if message fails, not critical
                }
            }

            TradeMatchDTO dto = new TradeMatchDTO();
            dto.setId(match.getId());
            dto.setOtherUserId(otherUser.getId());
            dto.setOtherUserName(otherUser.getUsername()); // Or display name
            dto.setOtherUserAvatar("person.crop.circle"); // Placeholder
            dto.setDistance(distance);
            
            List<com.tcg.arena.dto.TradeListEntryDTO> cardDtos = entries.stream()
                .map(e -> {
                    com.tcg.arena.dto.TradeListEntryDTO d = new com.tcg.arena.dto.TradeListEntryDTO();
                    d.setId(e.getId());
                    d.setCardTemplateId(e.getCardTemplate().getId());
                    d.setCardName(e.getCardTemplate().getName());
                    d.setImageUrl(getFullImageUrl(e.getCardTemplate()));
                    d.setType(e.getType());
                    d.setTcgType(e.getCardTemplate().getTcgType().name());
                    d.setRarity(e.getCardTemplate().getRarity().name());
                    return d;
                })
                .collect(Collectors.toList());

            dto.setMatchedCards(cardDtos);
            dto.setType(type);
            dto.setStatus(match.getStatus().name());
            
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

    public List<com.tcg.arena.dto.TradeListEntryDTO> getUserList(Long userId, TradeListType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tradeListEntryRepository.findByUserAndType(user, type).stream()
                .map(entry -> {
                    com.tcg.arena.dto.TradeListEntryDTO dto = new com.tcg.arena.dto.TradeListEntryDTO();
                    dto.setId(entry.getId());
                    dto.setCardTemplateId(entry.getCardTemplate().getId());
                    dto.setCardName(entry.getCardTemplate().getName());
                    dto.setImageUrl(getFullImageUrl(entry.getCardTemplate()));
                    dto.setType(entry.getType());
                    dto.setTcgType(entry.getCardTemplate().getTcgType().name());
                    dto.setRarity(entry.getCardTemplate().getRarity().name());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    private String getFullImageUrl(CardTemplate template) {
        // 1. Priority: TCGPlayer ID (Direct Link)
        if (template.getTcgplayerId() != null && !template.getTcgplayerId().isEmpty()) {
            return "https://tcgplayer-cdn.tcgplayer.com/product/" + template.getTcgplayerId() + "_in_1000x1000.jpg";
        }
        
        // 2. Fallback: Image URL field
        String baseUrl = template.getImageUrl();
        if (baseUrl == null) return null;
        
        // Logic from iOS Card.swift
        if (baseUrl.toLowerCase().contains("tcgplayer")) {
            return baseUrl;
        }
        if (baseUrl.contains("/high.webp")) {
            return baseUrl;
        }
        return baseUrl + "/high.webp";
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

        // Send Push Notification to the other user
        User recipient = match.getUser1().getId().equals(senderId) ? match.getUser2() : match.getUser1();
        String title = "Nuovo messaggio da " + sender.getUsername();
        notificationService.sendPushNotification(recipient.getId(), title, content);
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

        // 1. Identify matched cards to remove and summarize
        User u1 = match.getUser1();
        User u2 = match.getUser2();
        
        List<TradeListEntry> u1Entries = tradeListEntryRepository.findByUser(u1);
        List<TradeListEntry> u2Entries = tradeListEntryRepository.findByUser(u2);
        
        Set<Long> u1Want = u1Entries.stream().filter(e -> e.getType() == TradeListType.WANT).map(e -> e.getCardTemplate().getId()).collect(Collectors.toSet());
        Set<Long> u1Have = u1Entries.stream().filter(e -> e.getType() == TradeListType.HAVE).map(e -> e.getCardTemplate().getId()).collect(Collectors.toSet());
        
        Set<Long> u2Want = u2Entries.stream().filter(e -> e.getType() == TradeListType.WANT).map(e -> e.getCardTemplate().getId()).collect(Collectors.toSet());
        Set<Long> u2Have = u2Entries.stream().filter(e -> e.getType() == TradeListType.HAVE).map(e -> e.getCardTemplate().getId()).collect(Collectors.toSet());

        List<TradeListEntry> toRemove = new ArrayList<>();
        StringBuilder summary = new StringBuilder("🤝 Scambio Concluso!\n\nCarte scambiate:\n");
        boolean hasTrades = false;

        // U1 gets what U2 has
        for (TradeListEntry e : u2Entries) {
            if (e.getType() == TradeListType.HAVE && u1Want.contains(e.getCardTemplate().getId())) {
                summary.append("- ").append(e.getCardTemplate().getName()).append(" (da ").append(u2.getUsername()).append(" a ").append(u1.getUsername()).append(")\n");
                toRemove.add(e);
                // Also remove the WANT entry from U1
                u1Entries.stream()
                    .filter(x -> x.getType() == TradeListType.WANT && x.getCardTemplate().getId().equals(e.getCardTemplate().getId()))
                    .findFirst().ifPresent(toRemove::add);
                hasTrades = true;
            }
        }

        // U2 gets what U1 has
        for (TradeListEntry e : u1Entries) {
            if (e.getType() == TradeListType.HAVE && u2Want.contains(e.getCardTemplate().getId())) {
                summary.append("- ").append(e.getCardTemplate().getName()).append(" (da ").append(u1.getUsername()).append(" a ").append(u2.getUsername()).append(")\n");
                toRemove.add(e);
                // Also remove the WANT entry from U2
                u2Entries.stream()
                    .filter(x -> x.getType() == TradeListType.WANT && x.getCardTemplate().getId().equals(e.getCardTemplate().getId()))
                    .findFirst().ifPresent(toRemove::add);
                hasTrades = true;
            }
        }

        if (hasTrades) {
            tradeListEntryRepository.deleteAll(toRemove);
            sendMessage(matchId, userId, summary.toString());
        }
        
        match.setStatus(TradeStatus.COMPLETED);
        tradeMatchRepository.save(match);

        // Award Loyalty Points
        rewardService.earnPoints(match.getUser1().getId(), 50, "Scambio completato con " + match.getUser2().getUsername());
        rewardService.earnPoints(match.getUser2().getId(), 50, "Scambio completato con " + match.getUser1().getUsername());
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
