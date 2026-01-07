package com.tcg.arena.controller;

import com.tcg.arena.dto.CommunityEventDTO;
import com.tcg.arena.dto.CreateCommunityEventRequest;
import com.tcg.arena.model.CommunityEvent;
import com.tcg.arena.model.User;
import com.tcg.arena.service.CommunityEventService;
import com.tcg.arena.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/community/events")
public class CommunityEventController {

    private static final Logger logger = LoggerFactory.getLogger(CommunityEventController.class);

    @Autowired
    private CommunityEventService eventService;
    
    @Autowired
    private EmailService emailService;

    @Autowired
    private com.tcg.arena.repository.UserRepository userRepository;
    
    @Autowired
    private com.tcg.arena.repository.CommunityEventRepository eventRepository;

    /**
     * Create a new community event
     */
    @PostMapping
    public ResponseEntity<CommunityEventDTO> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody CreateCommunityEventRequest request) {
        Long userId = getUserId(userDetails);
        CommunityEventDTO event = eventService.createEvent(userId, request);
        return ResponseEntity.ok(event);
    }

    /**
     * Get upcoming events with optional filters
     */
    @GetMapping
    public ResponseEntity<List<CommunityEventDTO>> getEvents(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String tcgType,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            @RequestParam(required = false, defaultValue = "50") Double radiusKm) {
        Long userId = getUserId(userDetails);

        List<CommunityEventDTO> events;
        if (lat != null && lon != null) {
            events = eventService.getNearbyEvents(userId, lat, lon, radiusKm);
        } else {
            events = eventService.getUpcomingEvents(userId, tcgType);
        }

        return ResponseEntity.ok(events);
    }

    /**
     * Get event by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityEventDTO> getEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        CommunityEventDTO event = eventService.getEvent(id, userId);
        return ResponseEntity.ok(event);
    }

    /**
     * Join an event
     */
    @PostMapping("/{id}/join")
    public ResponseEntity<CommunityEventDTO> joinEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        CommunityEventDTO eventDTO = eventService.joinEvent(userId, id);
        
        // Send confirmation email
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getEmail() != null) {
                // Get full event details for email
                CommunityEvent event = eventRepository.findById(id).orElse(null);
                if (event != null) {
                    emailService.sendTournamentRegistration(user, event);
                    logger.info("Event registration email sent to: {}", user.getEmail());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send event registration email", e);
            // Don't fail registration if email fails
        }
        
        return ResponseEntity.ok(eventDTO);
    }

    /**
     * Leave an event
     */
    @DeleteMapping("/{id}/join")
    public ResponseEntity<CommunityEventDTO> leaveEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        CommunityEventDTO event = eventService.leaveEvent(userId, id);
        return ResponseEntity.ok(event);
    }

    /**
     * Get my created events
     */
    @GetMapping("/my/created")
    public ResponseEntity<List<CommunityEventDTO>> getMyCreatedEvents(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<CommunityEventDTO> events = eventService.getMyCreatedEvents(userId);
        return ResponseEntity.ok(events);
    }

    /**
     * Get events I've joined
     */
    @GetMapping("/my/joined")
    public ResponseEntity<List<CommunityEventDTO>> getMyJoinedEvents(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        List<CommunityEventDTO> events = eventService.getMyJoinedEvents(userId);
        return ResponseEntity.ok(events);
    }

    /**
     * Cancel an event (only creator)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = getUserId(userDetails);
        eventService.cancelEvent(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
