package com.tcg.arena.service;

import com.tcg.arena.model.*;
import com.tcg.arena.repository.TournamentParticipantRepository;
import com.tcg.arena.repository.TournamentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.dto.ManualRegistrationRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Arrays;

@Service
public class TournamentService {

    // Fuso orario italiano per il confronto delle date dei tornei
    private static final java.time.ZoneId ITALY_ZONE = java.time.ZoneId.of("Europe/Rome");
    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private TournamentParticipantRepository participantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RewardService rewardService;
    
    @Autowired
    private NotificationService notificationService;

    public List<Tournament> getAllTournaments() {
        List<Tournament> tournaments = tournamentRepository.findAllByOrderByStartDateAsc();
        populateParticipantCounts(tournaments);
        return tournaments;
    }

    private void populateParticipantCounts(List<Tournament> tournaments) {
        for (Tournament tournament : tournaments) {
            long participantCount = participantRepository.countByTournamentIdAndStatusIn(
                    tournament.getId(),
                    Arrays.asList(ParticipantStatus.REGISTERED, ParticipantStatus.CHECKED_IN));
            tournament.setCurrentParticipants((int) participantCount);
        }
    }

    public Optional<Tournament> getTournamentById(Long id) {
        Optional<Tournament> tournamentOpt = tournamentRepository.findById(id);
        if (tournamentOpt.isPresent()) {
            Tournament tournament = tournamentOpt.get();
            long participantCount = participantRepository.countByTournamentIdAndStatusIn(
                    tournament.getId(),
                    Arrays.asList(ParticipantStatus.REGISTERED, ParticipantStatus.CHECKED_IN));
            tournament.setCurrentParticipants((int) participantCount);
        }
        return tournamentOpt;
    }

    public List<Tournament> getUpcomingTournaments() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveHoursAgo = now.minusHours(5);
        System.out.println("📅 getUpcomingTournaments query with:");
        System.out.println("   now: " + now);
        System.out.println("   fiveHoursAgo: " + fiveHoursAgo);
        List<Tournament> tournaments = tournamentRepository.findUpcomingTournaments(now, fiveHoursAgo);
        System.out.println("   Found " + tournaments.size() + " tournaments from query");
        tournaments.forEach(t -> {
            System.out.println("   - " + t.getTitle() + " | Status: " + t.getStatus() + " | Start: " + t.getStartDate() + " | End: " + t.getEndDate());
        });
        if (tournaments.isEmpty()) {
            return getAllTournaments();
        }
        populateParticipantCounts(tournaments);
        return tournaments;
    }

    public List<Tournament> getPastTournaments() {
        LocalDateTime fiveHoursAgo = LocalDateTime.now().minusHours(5);
        List<Tournament> tournaments = tournamentRepository.findPastTournaments(fiveHoursAgo);
        populateParticipantCounts(tournaments);
        return tournaments;
    }

    public List<Tournament> getNearbyTournaments(double latitude, double longitude, double radiusKm) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveHoursAgo = now.minusHours(5);
        List<Tournament> allTournaments = tournamentRepository.findUpcomingTournaments(now, fiveHoursAgo);

        // First, try to find tournaments within the specified radius
        List<Tournament> nearbyTournaments = allTournaments.stream()
                .filter(tournament -> {
                    if (tournament.getLocation() == null)
                        return false;
                    // Check if latitude and longitude are not null before using them
                    Double lat = tournament.getLocation().getLatitude();
                    Double lon = tournament.getLocation().getLongitude();
                    if (lat == null || lon == null)
                        return false;

                    double distance = calculateDistance(
                            latitude, longitude,
                            lat,
                            lon);
                    return distance <= radiusKm;
                })
                .toList();

        // If no tournaments are found within radius, return all tournaments sorted by
        // distance
        if (nearbyTournaments.isEmpty()) {
            return allTournaments.stream()
                    .sorted((t1, t2) -> {
                        // Handle null coordinates by putting them at the end
                        Double lat1 = t1.getLocation() != null ? t1.getLocation().getLatitude() : null;
                        Double lon1 = t1.getLocation() != null ? t1.getLocation().getLongitude() : null;
                        Double lat2 = t2.getLocation() != null ? t2.getLocation().getLatitude() : null;
                        Double lon2 = t2.getLocation() != null ? t2.getLocation().getLongitude() : null;

                        if (lat1 == null || lon1 == null)
                            return 1; // t1 goes after t2
                        if (lat2 == null || lon2 == null)
                            return -1; // t1 goes before t2

                        double dist1 = calculateDistance(latitude, longitude, lat1, lon1);
                        double dist2 = calculateDistance(latitude, longitude, lat2, lon2);
                        return Double.compare(dist1, dist2);
                    })
                    .toList();
        }

        return nearbyTournaments;
    }

    public Tournament saveTournament(Tournament tournament) {
        return tournamentRepository.save(tournament);
    }

    public TournamentParticipant registerForTournament(Long tournamentId, Long userId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Block registrations when tournament is locked
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new RuntimeException("Tournament has already started. Registrations are closed.");
        }
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RuntimeException("Tournament has already ended. Registrations are closed.");
        }
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new RuntimeException("Tournament has been cancelled.");
        }

        // Check if user is already registered
        Optional<TournamentParticipant> existingParticipant = participantRepository
                .findByTournamentIdAndUserId(tournamentId, userId);

        if (existingParticipant.isPresent()) {
            throw new RuntimeException("User is already registered for this tournament");
        }

        // Count current registered participants (including checked-in)
        long registeredCount = participantRepository.countByTournamentIdAndStatusIn(
                tournamentId,
                Arrays.asList(ParticipantStatus.REGISTERED, ParticipantStatus.CHECKED_IN));

        TournamentParticipant participant = new TournamentParticipant();
        participant.setTournamentId(tournamentId);
        participant.setUserId(userId);
        participant.setRegistrationDate(LocalDateTime.now());
        participant.setCheckInCode(UUID.randomUUID().toString()); // Generate unique check-in code

        // Determine status based on available slots
        if (registeredCount < tournament.getMaxParticipants()) {
            participant.setStatus(ParticipantStatus.REGISTERED);
        } else {
            participant.setStatus(ParticipantStatus.WAITING_LIST);
        }

        TournamentParticipant savedParticipant = participantRepository.save(participant);

        // Award points for registration (+15 points)
        rewardService.earnPoints(userId, 15, "Tournament registration: " + tournament.getTitle());

        return savedParticipant;
    }

    public TournamentParticipant registerManualParticipant(Long tournamentId, ManualRegistrationRequest request) {
        User user = new User();
        String baseName = (request.getFirstName() + " " + request.getLastName()).trim();
        if (baseName.isEmpty())
            baseName = "Guest";

        user.setDisplayName(baseName);

        // Generate unique username/email for guest
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        user.setUsername("guest_" + timestamp + "_" + uuid);
        user.setEmail("guest_" + timestamp + "_" + uuid + "@tcgarena.local");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setDateJoined(LocalDateTime.now());
        user.setIsPremium(false);
        user.setIsMerchant(false);
        user.setPoints(0);

        user = userRepository.save(user);

        return registerForTournament(tournamentId, user.getId());
    }

    public TournamentParticipant addExistingParticipant(Long tournamentId, String userIdentifier) {
        // Find user by email or username
        Optional<User> userOpt = userRepository.findByEmail(userIdentifier);
        if (!userOpt.isPresent()) {
            userOpt = userRepository.findByUsername(userIdentifier);
        }

        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found with email or username: " + userIdentifier);
        }

        User user = userOpt.get();
        return registerForTournament(tournamentId, user.getId());
    }

    public boolean unregisterFromTournament(Long tournamentId, Long userId) {
        Optional<TournamentParticipant> participant = participantRepository
                .findByTournamentIdAndUserId(tournamentId, userId);

        if (participant.isPresent()) {
            Long participantUserId = participant.get().getUserId();
            participantRepository.delete(participant.get());

            // If the participant was registered and there are people on waiting list,
            // promote the first person from waiting list
            if (participant.get().getStatus() == ParticipantStatus.REGISTERED) {
                promoteFromWaitingList(tournamentId);
            }

            // Deduct points for cancellation (-10 points)
            Tournament tournament = tournamentRepository.findById(tournamentId).orElse(null);
            String tournamentName = tournament != null ? tournament.getTitle() : "Tournament";
            rewardService.earnPoints(participantUserId, -10, "Tournament cancellation: " + tournamentName);

            return true;
        }
        return false;
    }

    private void promoteFromWaitingList(Long tournamentId) {
        List<TournamentParticipant> waitingList = participantRepository
                .findByTournamentIdAndStatusOrderByRegistrationDateAsc(
                        tournamentId, ParticipantStatus.WAITING_LIST);

        if (!waitingList.isEmpty()) {
            TournamentParticipant promoted = waitingList.get(0);
            promoted.setStatus(ParticipantStatus.REGISTERED);
            participantRepository.save(promoted);
            
            // Send push notification to promoted user
            try {
                Optional<Tournament> tournament = tournamentRepository.findById(tournamentId);
                if (tournament.isPresent()) {
                    String tournamentTitle = tournament.get().getTitle();
                    String notificationTitle = "Sei stato iscritto al torneo!";
                    String notificationMessage = String.format(
                        "Un posto si è liberato per il torneo \"%s\". Sei stato promosso dalla waiting list e ora sei ufficialmente iscritto!",
                        tournamentTitle
                    );
                    
                    notificationService.sendPushNotification(
                        promoted.getUserId(),
                        notificationTitle,
                        notificationMessage
                    );
                }
            } catch (Exception e) {
                // Log error but don't fail the promotion
                System.err.println("Failed to send promotion notification: " + e.getMessage());
            }
        }
    }

    public List<TournamentParticipant> getTournamentParticipants(Long tournamentId) {
        return participantRepository.findByTournamentId(tournamentId);
    }

    public List<TournamentParticipant> getRegisteredParticipants(Long tournamentId) {
        return participantRepository.findByTournamentIdAndStatus(
                tournamentId, ParticipantStatus.REGISTERED);
    }

    public List<TournamentParticipant> getWaitingList(Long tournamentId) {
        return participantRepository.findByTournamentIdAndStatus(
                tournamentId, ParticipantStatus.WAITING_LIST);
    }

    public Optional<Tournament> updateTournament(Long id, Tournament tournamentDetails) {
        return tournamentRepository.findById(id).map(tournament -> {
            tournament.setTitle(tournamentDetails.getTitle());
            tournament.setDescription(tournamentDetails.getDescription());
            tournament.setTcgType(tournamentDetails.getTcgType());
            tournament.setType(tournamentDetails.getType());
            tournament.setStatus(tournamentDetails.getStatus());
            tournament.setStartDate(tournamentDetails.getStartDate());
            tournament.setEndDate(tournamentDetails.getEndDate());
            tournament.setMaxParticipants(tournamentDetails.getMaxParticipants());
            tournament.setEntryFee(tournamentDetails.getEntryFee());
            tournament.setPrizePool(tournamentDetails.getPrizePool());
            tournament.setLocation(tournamentDetails.getLocation());
            return tournamentRepository.save(tournament);
        });
    }

    public boolean deleteTournament(Long id) {
        if (tournamentRepository.existsById(id)) {
            tournamentRepository.deleteById(id);
            return true;
        }
        return false;
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

    // Check-in methods
    public TournamentParticipant checkInParticipant(String checkInCode) {
        TournamentParticipant participant = participantRepository.findByCheckInCode(checkInCode)
                .orElseThrow(() -> new RuntimeException("Codice check-in non valido"));

        // Check if tournament allows check-in (1 hour before start)
        Tournament tournament = tournamentRepository.findById(participant.getTournamentId())
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Usa ZonedDateTime con fuso orario italiano per il confronto corretto
        java.time.ZonedDateTime nowItaly = java.time.ZonedDateTime.now(ITALY_ZONE);
        LocalDateTime tournamentStart = tournament.getStartDate();
        java.time.ZonedDateTime tournamentStartZoned = tournamentStart.atZone(ITALY_ZONE);
        java.time.ZonedDateTime checkInStartTime = tournamentStartZoned.minusHours(1);
        java.time.ZonedDateTime checkInEndTime = tournamentStartZoned.plusMinutes(30);

        if (nowItaly.isBefore(checkInStartTime)) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            throw new RuntimeException(
                    "Check-in non ancora disponibile. Il check-in apre alle " + checkInStartTime.format(formatter));
        }

        if (nowItaly.isAfter(checkInEndTime)) {
            throw new RuntimeException("Check-in chiuso. Il periodo di check-in è terminato.");
        }

        // Check if participant is registered
        if (participant.getStatus() != ParticipantStatus.REGISTERED) {
            throw new RuntimeException("Solo i partecipanti registrati possono fare il check-in");
        }

        // Check if already checked in
        if (participant.getCheckedInAt() != null) {
            throw new RuntimeException("Check-in già effettuato");
        }

        // Perform check-in - use Italian time for consistency
        participant.setCheckedInAt(nowItaly.toLocalDateTime());
        participant.setStatus(ParticipantStatus.CHECKED_IN);

        return participantRepository.save(participant);
    }

    public TournamentParticipant selfCheckIn(Long tournamentId, Long userId) {
        TournamentParticipant participant = participantRepository.findByTournamentIdAndUserId(tournamentId, userId)
                .orElseThrow(() -> new RuntimeException("You are not registered for this tournament"));

        // Check if tournament allows check-in (1 hour before until 30 min after start)
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Block check-ins when tournament is locked
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new RuntimeException("Tournament has already started. Check-ins are closed.");
        }
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RuntimeException("Tournament has already ended.");
        }
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new RuntimeException("Tournament has been cancelled.");
        }

        // Usa ZonedDateTime con fuso orario italiano per il confronto corretto
        java.time.ZonedDateTime nowItaly = java.time.ZonedDateTime.now(ITALY_ZONE);
        LocalDateTime tournamentStart = tournament.getStartDate();

        // Interpreta la data del torneo come ora italiana e calcola la finestra di
        // check-in
        java.time.ZonedDateTime tournamentStartZoned = tournamentStart.atZone(ITALY_ZONE);
        java.time.ZonedDateTime checkInStartTime = tournamentStartZoned.minusHours(1);
        java.time.ZonedDateTime checkInEndTime = tournamentStartZoned.plusMinutes(30);

        // Debug logging
        System.out.println("[CHECKIN] ========================================");
        System.out.println("[CHECKIN] Tournament ID: " + tournamentId);
        System.out.println("[CHECKIN] Current time (Italy): " + nowItaly);
        System.out.println("[CHECKIN] Tournament start (Italy): " + tournamentStartZoned);
        System.out.println("[CHECKIN] Check-in window: " + checkInStartTime + " to " + checkInEndTime);
        System.out.println("[CHECKIN] Within window: "
                + (!nowItaly.isBefore(checkInStartTime) && !nowItaly.isAfter(checkInEndTime)));
        System.out.println("[CHECKIN] ========================================");

        if (nowItaly.isBefore(checkInStartTime)) {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
            throw new RuntimeException(
                    "Check-in non ancora disponibile. Il check-in apre alle " + checkInStartTime.format(formatter));
        }

        if (nowItaly.isAfter(checkInEndTime)) {
            throw new RuntimeException("Check-in chiuso. Il periodo di check-in è terminato.");
        }

        // Check if participant is registered
        if (participant.getStatus() != ParticipantStatus.REGISTERED) {
            throw new RuntimeException("Solo i partecipanti registrati possono fare il check-in");
        }

        // Check if already checked in
        if (participant.getCheckedInAt() != null) {
            throw new RuntimeException("Hai già effettuato il check-in");
        }

        // Perform check-in - use Italian time for consistency
        participant.setCheckedInAt(nowItaly.toLocalDateTime());
        participant.setStatus(ParticipantStatus.CHECKED_IN);

        TournamentParticipant savedParticipant = participantRepository.save(participant);

        // Award points for check-in (+25 points)
        rewardService.earnPoints(userId, 25, "Tournament check-in: " + tournament.getTitle());

        System.out.println("[CHECKIN] ✅ Check-in successful for participant: " + savedParticipant.getId());

        return savedParticipant;
    }

    public List<TournamentParticipant> getParticipantsWithUserDetails(Long tournamentId) {
        return participantRepository.findByTournamentIdWithUserDetails(tournamentId);
    }

    /**
     * Start a tournament - changes status to IN_PROGRESS and freezes
     * registrations/check-ins
     */
    public Tournament startTournament(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Validate tournament can be started
        if (tournament.getStatus() == TournamentStatus.IN_PROGRESS) {
            throw new RuntimeException("Tournament is already in progress");
        }
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RuntimeException("Tournament is already completed");
        }
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new RuntimeException("Tournament is cancelled");
        }

        // Get all registered participants to notify
        List<TournamentParticipant> participants = participantRepository.findByTournamentIdAndStatusIn(
                tournamentId,
                Arrays.asList(ParticipantStatus.REGISTERED, ParticipantStatus.CHECKED_IN,
                        ParticipantStatus.WAITING_LIST));

        // Change status to IN_PROGRESS
        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        Tournament savedTournament = tournamentRepository.save(tournament);

        // TODO: Send push notifications to all participants
        for (TournamentParticipant participant : participants) {
            sendTournamentStartedNotification(participant, tournament);
        }

        return savedTournament;
    }

    /**
     * Remove a participant from a tournament (merchant action)
     */
    public void removeParticipant(Long tournamentId, Long participantId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        TournamentParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (!participant.getTournamentId().equals(tournamentId)) {
            throw new RuntimeException("Participant does not belong to this tournament");
        }

        // Store participant status before deletion for promotion logic
        ParticipantStatus previousStatus = participant.getStatus();

        // Send notification before deletion
        sendParticipantRemovedNotification(participant, tournament);

        // Delete participant
        participantRepository.delete(participant);

        // If participant was registered/checked-in, promote from waiting list
        if (previousStatus == ParticipantStatus.REGISTERED || previousStatus == ParticipantStatus.CHECKED_IN) {
            promoteFromWaitingList(tournamentId);
        }
    }

    /**
     * Placeholder for sending tournament started notification
     * TODO: Implement actual push notification service
     */
    private void sendTournamentStartedNotification(TournamentParticipant participant, Tournament tournament) {
        // TODO: Implement push notification
        System.out.println("[NOTIFICATION PLACEHOLDER] Tournament started notification for user "
                + participant.getUserId() + " - Tournament: " + tournament.getTitle());

        // Get user to check for device token
        Optional<User> userOpt = userRepository.findById(participant.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String deviceToken = user.getDeviceToken();
            if (deviceToken != null && !deviceToken.isEmpty()) {
                // TODO: Send actual push notification
                // pushNotificationService.send(deviceToken, "Tournament Started!",
                // "The tournament '" + tournament.getTitle() + "' has started! Head to the
                // venue now.");
                System.out.println("[NOTIFICATION PLACEHOLDER] Would send push to device: "
                        + deviceToken.substring(0, 10) + "...");
            }
        }
    }

    /**
     * Placeholder for sending participant removed notification
     * TODO: Implement actual push notification service
     */
    private void sendParticipantRemovedNotification(TournamentParticipant participant, Tournament tournament) {
        // TODO: Implement push notification
        System.out.println("[NOTIFICATION PLACEHOLDER] Participant removed notification for user "
                + participant.getUserId() + " - Tournament: " + tournament.getTitle());

        // Get user to check for device token
        Optional<User> userOpt = userRepository.findById(participant.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String deviceToken = user.getDeviceToken();
            if (deviceToken != null && !deviceToken.isEmpty()) {
                // TODO: Send actual push notification
                // pushNotificationService.send(deviceToken, "Tournament Update",
                // "You have been removed from the tournament '" + tournament.getTitle() + "'.
                // Contact the organizer for more information.");
                System.out.println("[NOTIFICATION PLACEHOLDER] Would send push to device: "
                        + deviceToken.substring(0, 10) + "...");
            }
        }
    }

    /**
     * Complete a tournament and set placements for winners
     * 
     * @param tournamentId Tournament to complete
     * @param placements   List of PlacementDTO with participantId and placement (1,
     *                     2, 3)
     */
    public Tournament completeTournament(Long tournamentId, List<PlacementDTO> placements) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Tournament not found"));

        // Validate tournament can be completed
        if (tournament.getStatus() == TournamentStatus.COMPLETED) {
            throw new RuntimeException("Tournament is already completed");
        }
        if (tournament.getStatus() == TournamentStatus.CANCELLED) {
            throw new RuntimeException("Tournament is cancelled");
        }

        // Points per placement
        final int FIRST_PLACE_POINTS = 100;
        final int SECOND_PLACE_POINTS = 50;
        final int THIRD_PLACE_POINTS = 25;

        // Process placements
        for (PlacementDTO placement : placements) {
            TournamentParticipant participant = participantRepository.findById(placement.getParticipantId())
                    .orElseThrow(() -> new RuntimeException("Participant not found: " + placement.getParticipantId()));

            if (!participant.getTournamentId().equals(tournamentId)) {
                throw new RuntimeException("Participant does not belong to this tournament");
            }

            // Set placement
            participant.setPlacement(placement.getPlacement());
            participantRepository.save(participant);

            // Award points based on placement
            int pointsToAward = 0;
            String placementText = "";
            switch (placement.getPlacement()) {
                case 1:
                    pointsToAward = FIRST_PLACE_POINTS;
                    placementText = "1st place 🥇";
                    break;
                case 2:
                    pointsToAward = SECOND_PLACE_POINTS;
                    placementText = "2nd place 🥈";
                    break;
                case 3:
                    pointsToAward = THIRD_PLACE_POINTS;
                    placementText = "3rd place 🥉";
                    break;
            }

            if (pointsToAward > 0) {
                // Award points using rewardService to properly track the transaction
                rewardService.earnPoints(participant.getUserId(), pointsToAward,
                        placementText + " in tournament: " + tournament.getTitle());

                // Send notification to winner
                Optional<User> userOpt = userRepository.findById(participant.getUserId());
                if (userOpt.isPresent()) {
                    sendWinnerNotification(participant, tournament, placementText, pointsToAward);
                }
            }
        }

        // Change status to COMPLETED
        tournament.setStatus(TournamentStatus.COMPLETED);

        // Set winnerId if we have a 1st place
        Optional<PlacementDTO> firstPlace = placements.stream()
                .filter(p -> p.getPlacement() == 1)
                .findFirst();
        if (firstPlace.isPresent()) {
            TournamentParticipant winner = participantRepository.findById(firstPlace.get().getParticipantId())
                    .orElse(null);
            if (winner != null) {
                tournament.setWinnerId(winner.getUserId());
            }
        }

        return tournamentRepository.save(tournament);
    }

    /**
     * Placeholder for sending winner notification
     */
    private void sendWinnerNotification(TournamentParticipant participant, Tournament tournament, String placementText,
            int pointsAwarded) {
        System.out.println("[NOTIFICATION PLACEHOLDER] Winner notification for user "
                + participant.getUserId() + " - " + placementText + " in tournament: " + tournament.getTitle()
                + " (+" + pointsAwarded + " points)");

        Optional<User> userOpt = userRepository.findById(participant.getUserId());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String deviceToken = user.getDeviceToken();
            if (deviceToken != null && !deviceToken.isEmpty()) {
                System.out.println("[NOTIFICATION PLACEHOLDER] Would send push to device: "
                        + deviceToken.substring(0, Math.min(10, deviceToken.length())) + "...");
            }
        }
    }

    /**
     * DTO for placement data
     */
    public static class PlacementDTO {
        private Long participantId;
        private Integer placement;

        public PlacementDTO() {
        }

        public PlacementDTO(Long participantId, Integer placement) {
            this.participantId = participantId;
            this.placement = placement;
        }

        public Long getParticipantId() {
            return participantId;
        }

        public void setParticipantId(Long participantId) {
            this.participantId = participantId;
        }

        public Integer getPlacement() {
            return placement;
        }

        public void setPlacement(Integer placement) {
            this.placement = placement;
        }
    }

    /**
     * Auto-complete expired tournaments.
     * A tournament is considered expired if:
     * - Status is UPCOMING, REGISTRATION_OPEN, REGISTRATION_CLOSED, or IN_PROGRESS
     * - The scheduled start date + 8 hours has passed (assuming max tournament
     * duration)
     * 
     * @return Number of tournaments that were auto-completed
     */
    public int autoCompleteExpiredTournaments() {
        LocalDateTime now = LocalDateTime.now();
        // Consider a tournament expired if it started more than 8 hours ago
        LocalDateTime cutoffTime = now.minusHours(8);

        List<Tournament> allTournaments = tournamentRepository.findAll();
        int completedCount = 0;

        for (Tournament tournament : allTournaments) {
            // Only process tournaments that are not already completed or cancelled
            if (tournament.getStatus() == TournamentStatus.COMPLETED ||
                    tournament.getStatus() == TournamentStatus.CANCELLED) {
                continue;
            }

            // Check if tournament's start date is before the cutoff time
            if (tournament.getStartDate() != null && tournament.getStartDate().isBefore(cutoffTime)) {
                tournament.setStatus(TournamentStatus.COMPLETED);
                tournamentRepository.save(tournament);
                completedCount++;
                System.out.println("[AUTO-COMPLETE] Tournament '" + tournament.getTitle() +
                        "' (ID: " + tournament.getId() + ") auto-completed. Start date was: "
                        + tournament.getStartDate());
            }
        }

        return completedCount;
    }

    // ========== TOURNAMENT UPDATES (LIVE MESSAGES & PHOTOS) ==========

    @Autowired
    private com.tcg.arena.repository.TournamentUpdateRepository tournamentUpdateRepository;

    /**
     * Check if a user is a participant of the tournament
     */
    public boolean isUserParticipant(Long tournamentId, Long userId) {
        return participantRepository.findByTournamentIdAndUserId(tournamentId, userId).isPresent();
    }

    /**
     * Check if a user is the organizer of the tournament
     */
    public boolean isUserOrganizer(Long tournamentId, Long userId) {
        Optional<Tournament> tournament = tournamentRepository.findById(tournamentId);
        return tournament.isPresent() && tournament.get().getOrganizerId().equals(userId);
    }

    /**
     * Add a new update to a tournament (only organizer can do this)
     */
    public TournamentUpdate addTournamentUpdate(Long tournamentId, Long userId, String message, String imageBase64) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Verify user is the organizer
        if (!tournament.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Solo l'organizzatore può aggiungere aggiornamenti");
        }

        // Validate content
        if ((message == null || message.trim().isEmpty()) && (imageBase64 == null || imageBase64.trim().isEmpty())) {
            throw new RuntimeException("Inserisci un messaggio o un'immagine");
        }

        // Validate message length
        if (message != null && message.length() > 2000) {
            throw new RuntimeException("Il messaggio non può superare i 2000 caratteri");
        }

        // Validate image size (max ~5MB base64 = ~7MB string)
        if (imageBase64 != null && imageBase64.length() > 7000000) {
            throw new RuntimeException("L'immagine è troppo grande (max 5MB)");
        }

        TournamentUpdate update = new TournamentUpdate(tournamentId, message, imageBase64, userId);
        return tournamentUpdateRepository.save(update);
    }

    /**
     * Get all updates for a tournament (only participants or organizer can see)
     */
    public List<TournamentUpdate> getTournamentUpdates(Long tournamentId, Long userId) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Verify user is participant or organizer
        boolean isOrganizer = tournament.getOrganizerId().equals(userId);
        boolean isParticipant = isUserParticipant(tournamentId, userId);

        if (!isOrganizer && !isParticipant) {
            throw new RuntimeException("Solo i partecipanti possono vedere gli aggiornamenti");
        }

        return tournamentUpdateRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId);
    }

    /**
     * Get all updates for a tournament (PUBLIC - no authentication check)
     * Client apps should verify if user is participant before showing
     */
    public List<TournamentUpdate> getTournamentUpdatesPublic(Long tournamentId) {
        // Verify tournament exists
        tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        return tournamentUpdateRepository.findByTournamentIdOrderByCreatedAtDesc(tournamentId);
    }

    /**
     * Delete an update (only organizer can do this)
     */
    public void deleteTournamentUpdate(Long tournamentId, Long updateId, Long userId) {
        // Verify tournament exists
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Verify user is the organizer
        if (!tournament.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Solo l'organizzatore può eliminare gli aggiornamenti");
        }

        // Verify update exists and belongs to this tournament
        TournamentUpdate update = tournamentUpdateRepository.findById(updateId)
                .orElseThrow(() -> new RuntimeException("Aggiornamento non trovato"));

        if (!update.getTournamentId().equals(tournamentId)) {
            throw new RuntimeException("L'aggiornamento non appartiene a questo torneo");
        }

        tournamentUpdateRepository.deleteById(updateId);
    }

    /**
     * Get update count for a tournament
     */
    public int getTournamentUpdateCount(Long tournamentId) {
        return tournamentUpdateRepository.countByTournamentId(tournamentId);
    }

    // ===== TOURNAMENT APPROVAL WORKFLOW METHODS =====

    /**
     * Create a tournament request from a customer
     * The tournament starts in PENDING_APPROVAL status
     */
    public Tournament createTournamentRequest(com.tcg.arena.controller.TournamentController.TournamentRequestDTO requestDTO, Long userId) {
        // Validate shop exists
        Shop shop = shopRepository.findById(requestDTO.getShopId())
                .orElseThrow(() -> new RuntimeException("Negozio non trovato"));

        // Create tournament with PENDING_APPROVAL status
        Tournament tournament = new Tournament();
        tournament.setTitle(requestDTO.getTitle());
        tournament.setDescription(requestDTO.getDescription());
        tournament.setTcgType(TCGType.valueOf(requestDTO.getTcgType()));
        if (requestDTO.getType() != null) {
            tournament.setType(TournamentType.valueOf(requestDTO.getType()));
        }
        tournament.setStatus(TournamentStatus.PENDING_APPROVAL);
        LocalDateTime startDate = LocalDateTime.parse(requestDTO.getStartDate());
        tournament.setStartDate(startDate);
        if (requestDTO.getEndDate() != null && !requestDTO.getEndDate().isEmpty()) {
            tournament.setEndDate(LocalDateTime.parse(requestDTO.getEndDate()));
        } else {
            // Se non viene fornito l'orario di fine, imposta 4 ore dopo l'inizio
            tournament.setEndDate(startDate.plusHours(4));
        }
        tournament.setMaxParticipants(requestDTO.getMaxParticipants());
        tournament.setEntryFee(requestDTO.getEntryFee());
        tournament.setPrizePool(requestDTO.getPrizePool());
        
        // Set organizerId to shop owner (the one who will need to approve)
        tournament.setOrganizerId(shop.getOwnerId());
        
        // Set createdByUserId to the customer requesting the tournament
        tournament.setCreatedByUserId(userId);
        
        // Set location from shop
        TournamentLocation location = new TournamentLocation();
        location.setVenueName(shop.getName());
        location.setAddress(shop.getAddress());
        
        // Extract city from shop address (format: "Via Address, City PostalCode")
        String city = "";
        if (shop.getAddress() != null && shop.getAddress().contains(",")) {
            String[] parts = shop.getAddress().split(",");
            if (parts.length > 1) {
                // Get the part after the comma and trim it
                String cityPart = parts[1].trim();
                // Remove postal code if present (last digits)
                city = cityPart.replaceAll("\\s*\\d+\\s*$", "").trim();
            }
        }
        location.setCity(city);
        location.setCountry("Italy");
        location.setLatitude(shop.getLatitude());
        location.setLongitude(shop.getLongitude());
        tournament.setLocation(location);

        System.out.println("💾 Saving tournament request:");
        System.out.println("   Title: " + tournament.getTitle());
        System.out.println("   Status: " + tournament.getStatus());
        System.out.println("   Start Date: " + tournament.getStartDate());
        System.out.println("   End Date: " + tournament.getEndDate());
        System.out.println("   CreatedByUserId: " + tournament.getCreatedByUserId());
        System.out.println("   OrganizerId: " + tournament.getOrganizerId());
        
        Tournament saved = tournamentRepository.save(tournament);
        System.out.println("   ✅ Saved with ID: " + saved.getId());
        return saved;
    }

    /**
     * Approve a tournament request
     * Only the shop owner (organizer) can approve
     */
    public Tournament approveTournament(Long tournamentId, Long userId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Verify tournament is in PENDING_APPROVAL status
        if (tournament.getStatus() != TournamentStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Questo torneo non è in attesa di approvazione");
        }

        // Verify user is the organizer (shop owner)
        if (!tournament.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Solo il proprietario del negozio può approvare questo torneo");
        }

        // Approve the tournament
        tournament.setStatus(TournamentStatus.UPCOMING);
        tournament.setApprovedByUserId(userId);
        tournament.setApprovalDate(LocalDateTime.now());

        System.out.println("✅ Approving tournament:");
        System.out.println("   ID: " + tournament.getId());
        System.out.println("   Title: " + tournament.getTitle());
        System.out.println("   New Status: " + tournament.getStatus());
        System.out.println("   Start Date: " + tournament.getStartDate());
        System.out.println("   End Date: " + tournament.getEndDate());
        
        Tournament approved = tournamentRepository.save(tournament);
        System.out.println("   ✅ Approved and saved");
        return approved;
    }

    /**
     * Reject a tournament request
     * Only the shop owner (organizer) can reject
     */
    public Tournament rejectTournament(Long tournamentId, Long userId, String reason) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> new RuntimeException("Torneo non trovato"));

        // Verify tournament is in PENDING_APPROVAL status
        if (tournament.getStatus() != TournamentStatus.PENDING_APPROVAL) {
            throw new RuntimeException("Questo torneo non è in attesa di approvazione");
        }

        // Verify user is the organizer (shop owner)
        if (!tournament.getOrganizerId().equals(userId)) {
            throw new RuntimeException("Solo il proprietario del negozio può rifiutare questo torneo");
        }

        // Reject the tournament
        tournament.setStatus(TournamentStatus.REJECTED);
        tournament.setRejectionReason(reason);

        return tournamentRepository.save(tournament);
    }

    /**
     * Get all pending tournament requests for shops owned by a merchant
     */
    public List<Tournament> getPendingTournamentRequestsForMerchant(Long merchantUserId) {
        // Find all tournaments where:
        // - status is PENDING_APPROVAL
        // - organizerId matches the merchant's user ID
        return tournamentRepository.findByStatusAndOrganizerId(
                TournamentStatus.PENDING_APPROVAL, 
                merchantUserId
        );
    }

    @Autowired
    private com.tcg.arena.repository.ShopRepository shopRepository;
}
