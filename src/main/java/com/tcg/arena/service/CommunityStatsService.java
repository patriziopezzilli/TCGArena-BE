package com.tcg.arena.service;

import com.tcg.arena.dto.CommunityStatsDTO;
import com.tcg.arena.model.CommunityEvent.EventStatus;
import com.tcg.arena.repository.CommunityEventParticipantRepository;
import com.tcg.arena.repository.CommunityEventRepository;
import com.tcg.arena.repository.ChatConversationRepository;
import com.tcg.arena.repository.TradeListEntryRepository;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.model.CommunityEventParticipant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class CommunityStatsService {

    @Autowired
    private ChatConversationRepository chatConversationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TradeListEntryRepository tradeListEntryRepository;

    @Autowired
    private CommunityEventRepository eventRepository;

    @Autowired
    private CommunityEventParticipantRepository participantRepository;

    /**
     * Get community statistics for a user
     */
    public CommunityStatsDTO getStatsForUser(Long userId) {
        CommunityStatsDTO stats = new CommunityStatsDTO();

        // Unread messages count
        stats.setUnreadMessages(getUnreadMessagesCount(userId));

        // New users in last 24 hours
        stats.setNewUsersToday(getNewUsersCount());

        // Active trade listings count
        stats.setActiveTradeListings(getActiveTradeListingsCount());

        // Total users
        stats.setTotalUsers((int) userRepository.count());

        // Upcoming events count
        stats.setUpcomingEvents((int) eventRepository.countUpcoming(LocalDateTime.now(), EventStatus.ACTIVE));

        // My events count (created + joined)
        stats.setMyEventsCount(getMyEventsCount(userId));

        return stats;
    }

    /**
     * Get unread messages count for a user
     */
    private int getUnreadMessagesCount(Long userId) {
        try {
            // Count conversations with unread messages
            return chatConversationRepository.countUnreadByUserId(userId);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get count of new users registered in last 24 hours
     */
    private int getNewUsersCount() {
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            return (int) userRepository.countByDateJoinedAfter(since);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get count of active trade listings
     */
    private int getActiveTradeListingsCount() {
        try {
            return (int) tradeListEntryRepository.count();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Get count of events user has created or joined
     */
    private int getMyEventsCount(Long userId) {
        try {
            int created = eventRepository.findByCreatorIdOrderByEventDateDesc(userId).size();
            int joined = (int) participantRepository.findByUserIdAndStatus(userId,
                    CommunityEventParticipant.ParticipantStatus.JOINED).size();
            return Math.max(created, joined); // Return max to avoid counting same event twice
        } catch (Exception e) {
            return 0;
        }
    }
}
