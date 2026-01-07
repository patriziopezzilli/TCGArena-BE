package com.tcg.arena.service;

import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.CommunityEventParticipant;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.CommunityEventRepository;
import com.tcg.arena.repository.EventParticipantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventReminderService {

    private static final Logger logger = LoggerFactory.getLogger(EventReminderService.class);
    
    private final CommunityEventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final EmailService emailService;

    public EventReminderService(CommunityEventRepository eventRepository,
                                EventParticipantRepository participantRepository,
                                EmailService emailService) {
        this.eventRepository = eventRepository;
        this.participantRepository = participantRepository;
        this.emailService = emailService;
    }

    /**
     * Send event reminders 24 hours before event
     * Runs every hour to check for events happening in 24 hours
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    public void sendEventReminders() {
        logger.info("Checking for events needing reminders...");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in24Hours = now.plusHours(24);
        LocalDateTime in25Hours = now.plusHours(25);
        
        // Find events happening between 24-25 hours from now
        List<CommunityEvent> upcomingEvents = eventRepository.findByEventDateBetween(in24Hours, in25Hours);
        
        logger.info("Found {} events needing reminders", upcomingEvents.size());
        
        for (CommunityEvent event : upcomingEvents) {
            sendRemindersForEvent(event);
        }
    }

    /**
     * Send reminders to all participants of an event
     */
    private void sendRemindersForEvent(CommunityEvent event) {
        List<CommunityEventParticipant> participants = participantRepository.findByEvent(event);
        
        logger.info("Sending reminders to {} participants for event: {}", participants.size(), event.getTitle());
        
        for (CommunityEventParticipant participant : participants) {
            User user = participant.getUser();
            
            try {
                emailService.sendEventReminder(user, event);
                logger.info("Event reminder sent to user: {}", user.getUsername());
            } catch (Exception e) {
                logger.error("Failed to send event reminder to user: {}", user.getUsername(), e);
            }
        }
    }
}
