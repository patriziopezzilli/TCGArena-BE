package com.tcg.arena.service;

import com.tcg.arena.dto.CommunityEventDTO;
import com.tcg.arena.dto.CreateCommunityEventRequest;
import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.CommunityEvent.EventStatus;
import com.tcg.arena.model.CommunityEvent.LocationType;
import com.tcg.arena.model.CommunityEventParticipant;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.CommunityEventParticipantRepository;
import com.tcg.arena.repository.CommunityEventRepository;
import com.tcg.arena.repository.ShopRepository;
import com.tcg.arena.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommunityEventService {

    @Autowired
    private CommunityEventRepository eventRepository;

    @Autowired
    private CommunityEventParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private com.tcg.arena.repository.UserEmailPreferencesRepository emailPreferencesRepository;

    /**
     * Create a new community event
     */
    @Transactional
    public CommunityEventDTO createEvent(Long userId, CreateCommunityEventRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommunityEvent event = new CommunityEvent();
        event.setCreator(creator);
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setEventDate(LocalDateTime.parse(request.getEventDate(), DateTimeFormatter.ISO_DATE_TIME));
        event.setMaxParticipants(request.getMaxParticipants() != null ? request.getMaxParticipants() : 10);
        event.setTcgType(request.getTcgType());

        // Handle location
        LocationType locationType = LocationType.valueOf(request.getLocationType());
        event.setLocationType(locationType);

        if (locationType == LocationType.SHOP && request.getShopId() != null) {
            Shop shop = shopRepository.findById(request.getShopId())
                    .orElseThrow(() -> new RuntimeException("Shop not found"));
            event.setShop(shop);
            event.setLatitude(shop.getLatitude());
            event.setLongitude(shop.getLongitude());
        } else {
            event.setCustomLocation(request.getCustomLocation());
            event.setLatitude(request.getLatitude());
            event.setLongitude(request.getLongitude());
        }

        event = eventRepository.save(event);

        // Creator automatically joins their own event
        CommunityEventParticipant creatorParticipant = new CommunityEventParticipant(event, creator);
        participantRepository.save(creatorParticipant);
        event.getParticipants().add(creatorParticipant);

        return CommunityEventDTO.fromEntity(event, userId);
    }

    /**
     * Get all upcoming events
     */
    public List<CommunityEventDTO> getUpcomingEvents(Long currentUserId, String tcgType) {
        List<CommunityEvent> events;

        if (tcgType != null && !tcgType.isEmpty()) {
            events = eventRepository.findUpcomingByTcgType(LocalDateTime.now(), EventStatus.ACTIVE, tcgType);
        } else {
            events = eventRepository.findUpcoming(LocalDateTime.now(), EventStatus.ACTIVE);
        }

        return events.stream()
                .map(e -> CommunityEventDTO.fromEntity(e, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get nearby events
     */
    public List<CommunityEventDTO> getNearbyEvents(Long currentUserId, Double lat, Double lon, Double radiusKm) {
        List<CommunityEvent> events = eventRepository.findNearby(LocalDateTime.now(), lat, lon, radiusKm);

        return events.stream()
                .map(e -> CommunityEventDTO.fromEntity(e, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Get event by ID
     */
    public CommunityEventDTO getEvent(Long eventId, Long currentUserId) {
        CommunityEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        return CommunityEventDTO.fromEntity(event, currentUserId);
    }

    /**
     * Join an event
     */
    @Transactional
    public CommunityEventDTO joinEvent(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommunityEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        // Check if event is full
        if (event.isFull()) {
            throw new RuntimeException("Event is full");
        }

        // Check if already joined
        if (participantRepository.existsByEventIdAndUserId(eventId, userId)) {
            // Update status if previously cancelled
            CommunityEventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                    .orElseThrow();
            participant.setStatus(CommunityEventParticipant.ParticipantStatus.JOINED);
            participant.setJoinedAt(LocalDateTime.now());
            participantRepository.save(participant);
        } else {
            CommunityEventParticipant participant = new CommunityEventParticipant(event, user);
            participantRepository.save(participant);
            event.getParticipants().add(participant);
        }

        // Notify event creator
        if (!event.getCreator().getId().equals(userId)) {
            notificationService.sendPushNotification(
                    event.getCreator().getId(),
                    "Nuovo partecipante",
                    user.getDisplayName().toLowerCase() + " si è iscritto al tuo evento: " + event.getTitle());
        }

        return CommunityEventDTO.fromEntity(event, userId);
    }

    /**
     * Leave an event
     */
    @Transactional
    public CommunityEventDTO leaveEvent(Long userId, Long eventId) {
        CommunityEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        CommunityEventParticipant participant = participantRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new RuntimeException("Not a participant"));

        participant.setStatus(CommunityEventParticipant.ParticipantStatus.CANCELLED);
        participantRepository.save(participant);

        // Remove from list for DTO generation
        event.getParticipants().removeIf(p -> p.getUser().getId().equals(userId));

        return CommunityEventDTO.fromEntity(event, userId);
    }

    /**
     * Get events created by user
     */
    public List<CommunityEventDTO> getMyCreatedEvents(Long userId) {
        return eventRepository.findByCreatorIdOrderByEventDateDesc(userId).stream()
                .map(e -> CommunityEventDTO.fromEntity(e, userId))
                .collect(Collectors.toList());
    }

    /**
     * Get events user has joined
     */
    public List<CommunityEventDTO> getMyJoinedEvents(Long userId) {
        return eventRepository.findByParticipantUserId(userId).stream()
                .map(e -> CommunityEventDTO.fromEntity(e, userId))
                .collect(Collectors.toList());
    }

    /**
     * Cancel an event (only creator can do this)
     */
    @Transactional
    public void cancelEvent(Long userId, Long eventId) {
        CommunityEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the creator can cancel this event");
        }

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        // Format date for email
        String formattedDate = event.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        // Notify all participants via push and email
        for (CommunityEventParticipant participant : event.getParticipants()) {
            if (!participant.getUser().getId().equals(userId)) {
                // Push notification
                notificationService.sendPushNotification(
                        participant.getUser().getId(),
                        "Evento annullato",
                        "L'evento \"" + event.getTitle() + "\" è stato annullato");
                
                // Email notification
                if (shouldSendEventNotification(participant.getUser())) {
                    try {
                        emailService.sendEventCancelled(
                            participant.getUser().getEmail(),
                            participant.getUser().getUsername(),
                            event.getTitle(),
                            formattedDate,
                            event.getLocationName(),
                            "L'organizzatore ha cancellato l'evento"
                        );
                    } catch (Exception e) {
                        // Log but don't fail
                    }
                }
            }
        }
    }

    /**
     * Count upcoming events
     */
    public long countUpcoming() {
        return eventRepository.countUpcoming(LocalDateTime.now(), EventStatus.ACTIVE);
    }
    /**
     * Check if user wants to receive event notifications
     */
    private boolean shouldSendEventNotification(User user) {
        return emailPreferencesRepository.findByUser(user)
                .map(prefs -> prefs.getEventNotifications())
                .orElse(true);
    }

    /**
     * Update event and notify participants of changes
     */
    @Transactional
    public CommunityEventDTO updateEvent(Long userId, Long eventId, String newDate, String newTime, 
                                         String newLocation, String updateNote) {
        CommunityEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (!event.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Only the creator can update this event");
        }

        // Update event fields (only if provided)
        boolean hasChanges = false;
        if (newDate != null) {
            event.setEventDate(LocalDateTime.parse(newDate, java.time.format.DateTimeFormatter.ISO_DATE_TIME));
            hasChanges = true;
        }
        if (newLocation != null) {
            event.setCustomLocation(newLocation);
            hasChanges = true;
        }

        if (hasChanges) {
            eventRepository.save(event);

            // Notify all participants
            for (CommunityEventParticipant participant : event.getParticipants()) {
                if (!participant.getUser().getId().equals(userId)) {
                    // Push notification
                    notificationService.sendPushNotification(
                            participant.getUser().getId(),
                            "Evento modificato",
                            "L'evento \"" + event.getTitle() + "\" è stato aggiornato");
                    
                    // Email notification
                    if (shouldSendEventNotification(participant.getUser())) {
                        try {
                            emailService.sendEventUpdated(
                                participant.getUser().getEmail(),
                                participant.getUser().getUsername(),
                                event.getTitle(),
                                eventId,
                                newDate != null ? event.getEventDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : null,
                                newTime,
                                newLocation,
                                updateNote
                            );
                        } catch (Exception e) {
                            // Log but don't fail
                        }
                    }
                }
            }
        }

        return CommunityEventDTO.fromEntity(event, userId);
    }}
