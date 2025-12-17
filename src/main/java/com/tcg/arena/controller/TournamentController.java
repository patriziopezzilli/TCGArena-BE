package com.tcg.arena.controller;

import com.tcg.arena.model.Tournament;
import com.tcg.arena.model.User;
import com.tcg.arena.model.TournamentParticipant;
import com.tcg.arena.service.TournamentService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.tcg.arena.dto.ManualRegistrationRequest;

@RestController
@RequestMapping("/api/tournaments")
@Tag(name = "Tournaments", description = "API for managing tournaments in the TCG Arena system")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all tournaments", description = "Retrieves a list of all tournaments. PENDING_APPROVAL tournaments are visible only to the creator and shop owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tournaments")
    })
    public List<Tournament> getAllTournaments() {
        Optional<User> currentUser = userService.getCurrentUser();
        List<Tournament> allTournaments = tournamentService.getAllTournaments();
        
        // Filter out PENDING_APPROVAL and REJECTED tournaments unless user is creator or organizer
        if (currentUser.isEmpty()) {
            // Anonymous user - show only approved tournaments
            return allTournaments.stream()
                    .filter(t -> t.getStatus() != com.tcg.arena.model.TournamentStatus.PENDING_APPROVAL 
                            && t.getStatus() != com.tcg.arena.model.TournamentStatus.REJECTED)
                    .toList();
        }
        
        Long userId = currentUser.get().getId();
        return allTournaments.stream()
                .filter(t -> {
                    // Show all approved tournaments
                    if (t.getStatus() != com.tcg.arena.model.TournamentStatus.PENDING_APPROVAL 
                            && t.getStatus() != com.tcg.arena.model.TournamentStatus.REJECTED) {
                        return true;
                    }
                    // Show pending/rejected only to creator or organizer
                    return userId.equals(t.getCreatedByUserId()) || userId.equals(t.getOrganizerId());
                })
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament by ID", description = "Retrieves a specific tournament by its unique ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament found and returned"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<Tournament> getTournamentById(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long id) {
        return tournamentService.getTournamentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming tournaments", description = "Retrieves a list of upcoming tournaments. PENDING_APPROVAL tournaments are visible only to the creator and shop owner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of upcoming tournaments")
    })
    public List<Tournament> getUpcomingTournaments() {
        Optional<User> currentUser = userService.getCurrentUser();
        List<Tournament> upcomingTournaments = tournamentService.getUpcomingTournaments();
        
        // Filter based on user permissions (same logic as getAllTournaments)
        if (currentUser.isEmpty()) {
            return upcomingTournaments.stream()
                    .filter(t -> t.getStatus() != com.tcg.arena.model.TournamentStatus.PENDING_APPROVAL 
                            && t.getStatus() != com.tcg.arena.model.TournamentStatus.REJECTED)
                    .toList();
        }
        
        Long userId = currentUser.get().getId();
        return upcomingTournaments.stream()
                .filter(t -> {
                    if (t.getStatus() != com.tcg.arena.model.TournamentStatus.PENDING_APPROVAL 
                            && t.getStatus() != com.tcg.arena.model.TournamentStatus.REJECTED) {
                        return true;
                    }
                    return userId.equals(t.getCreatedByUserId()) || userId.equals(t.getOrganizerId());
                })
                .toList();
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby tournaments", description = "Retrieves tournaments within a specified radius from given coordinates. If no tournaments are found within the radius, returns all upcoming tournaments sorted by distance from the specified location.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list of nearby or all tournaments sorted by distance")
    })
    public List<Tournament> getNearbyTournaments(
            @Parameter(description = "Latitude of the location") @RequestParam double latitude,
            @Parameter(description = "Longitude of the location") @RequestParam double longitude,
            @Parameter(description = "Search radius in kilometers") @RequestParam(defaultValue = "50") double radiusKm) {
        return tournamentService.getNearbyTournaments(latitude, longitude, radiusKm);
    }

    @GetMapping("/past")
    @Operation(summary = "Get past tournaments", description = "Retrieves all tournaments that have ended more than 5 hours ago")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Past tournaments retrieved successfully")
    })
    public List<Tournament> getPastTournaments() {
        return tournamentService.getPastTournaments();
    }

    @PostMapping
    @Operation(summary = "Create a new tournament", description = "Creates a new tournament in the system. Only merchants can create tournaments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid tournament data provided"),
            @ApiResponse(responseCode = "403", description = "Only merchants can create tournaments")
    })
    public ResponseEntity<Tournament> createTournament(
            @Parameter(description = "Tournament object to be created") @RequestBody Tournament tournament) {
        // Check if current user is a merchant
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty() || !Boolean.TRUE.equals(currentUser.get().getIsMerchant())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Set the organizerId from the current user
        tournament.setOrganizerId(currentUser.get().getId());

        // Set default status if not provided
        if (tournament.getStatus() == null) {
            tournament.setStatus(com.tcg.arena.model.TournamentStatus.UPCOMING);
        }

        Tournament savedTournament = tournamentService.saveTournament(tournament);
        return ResponseEntity.ok(savedTournament);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tournament", description = "Updates the details of an existing tournament")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament updated successfully"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<Tournament> updateTournament(
            @Parameter(description = "Unique identifier of the tournament to update") @PathVariable Long id,
            @Parameter(description = "Updated tournament object") @RequestBody Tournament tournament) {
        return tournamentService.updateTournament(id, tournament)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tournament", description = "Deletes a tournament from the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Tournament deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<Void> deleteTournament(
            @Parameter(description = "Unique identifier of the tournament to delete") @PathVariable Long id) {
        if (tournamentService.deleteTournament(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{tournamentId}/register")
    @Operation(summary = "Register for a tournament", description = "Registers the current user for a tournament. If the tournament is full, adds to waiting list.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully registered for tournament"),
            @ApiResponse(responseCode = "400", description = "User already registered or tournament not found"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Cannot register for ranked tournaments")
    })
    public ResponseEntity<?> registerForTournament(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Check if tournament is ranked - registration not allowed
        Optional<Tournament> tournamentOpt = tournamentService.getTournamentById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }

        Tournament tournament = tournamentOpt.get();
        if (Boolean.TRUE.equals(tournament.getIsRanked())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Questo è un torneo ufficiale. L'iscrizione deve avvenire tramite l'app dedicata.");
        }

        try {
            TournamentParticipant participant = tournamentService.registerForTournament(tournamentId,
                    currentUser.get().getId());
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{tournamentId}/register")
    @Operation(summary = "Unregister from a tournament", description = "Unregisters the current user from a tournament")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Successfully unregistered from tournament"),
            @ApiResponse(responseCode = "404", description = "User not registered for this tournament"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<Void> unregisterFromTournament(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (tournamentService.unregisterFromTournament(tournamentId, currentUser.get().getId())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/{id}/participants/manual")
    @Operation(summary = "Register manual participant", description = "Registers a manual participant (guest) for a tournament. Only merchants can do this.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully registered manual participant"),
            @ApiResponse(responseCode = "400", description = "Invalid data or tournament not found")
    })
    public ResponseEntity<?> registerManualParticipant(@PathVariable Long id,
            @RequestBody ManualRegistrationRequest request) {
        try {
            TournamentParticipant participant = tournamentService.registerManualParticipant(id, request);
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{id}/participants")
    @Operation(summary = "Add existing participant", description = "Adds an existing user to a tournament by email or username. Only merchants can do this.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added participant"),
            @ApiResponse(responseCode = "400", description = "Invalid data, user not found, or tournament not found")
    })
    public ResponseEntity<?> addExistingParticipant(@PathVariable Long id,
            @RequestBody Map<String, String> request) {
        try {
            String userIdentifier = request.get("userIdentifier");
            if (userIdentifier == null || userIdentifier.trim().isEmpty()) {
                return ResponseEntity.badRequest().body("userIdentifier is required");
            }

            TournamentParticipant participant = tournamentService.addExistingParticipant(id, userIdentifier.trim());
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/{tournamentId}/participants")
    @Operation(summary = "Get tournament participants", description = "Retrieves all participants for a specific tournament")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved participants")
    })
    public List<TournamentParticipant> getTournamentParticipants(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getTournamentParticipants(tournamentId);
    }

    @GetMapping("/{tournamentId}/participants/registered")
    @Operation(summary = "Get registered participants", description = "Retrieves only registered participants (not waiting list) for a specific tournament")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved registered participants")
    })
    public List<TournamentParticipant> getRegisteredParticipants(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getRegisteredParticipants(tournamentId);
    }

    @GetMapping("/{tournamentId}/participants/waiting")
    @Operation(summary = "Get waiting list", description = "Retrieves participants on the waiting list for a specific tournament")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved waiting list")
    })
    public List<TournamentParticipant> getWaitingList(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getWaitingList(tournamentId);
    }

    @PostMapping("/checkin")
    @Operation(summary = "Check-in participant", description = "Allows a participant to check-in using their QR code. Check-in is only available 1 hour before tournament start.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked in participant"),
            @ApiResponse(responseCode = "400", description = "Invalid check-in code or check-in not available"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<TournamentParticipant> checkInParticipant(
            @Parameter(description = "Check-in QR code") @RequestParam String code) {
        try {
            TournamentParticipant participant = tournamentService.checkInParticipant(code);
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{tournamentId}/self-checkin")
    @Operation(summary = "Self check-in", description = "Allows a registered participant to check themselves in for a tournament. Check-in is available 1 hour before until 30 minutes after tournament start.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully checked in"),
            @ApiResponse(responseCode = "400", description = "Check-in not available or user not registered"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<?> selfCheckIn(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        System.out.println("[CHECKIN] selfCheckIn called for tournamentId: " + tournamentId);

        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            System.out.println("[CHECKIN] User not authenticated");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        System.out.println(
                "[CHECKIN] User: " + currentUser.get().getUsername() + " (ID: " + currentUser.get().getId() + ")");

        try {
            TournamentParticipant participant = tournamentService.selfCheckIn(tournamentId, currentUser.get().getId());
            System.out.println("[CHECKIN] Check-in successful for participant: " + participant.getId());
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            System.out.println("[CHECKIN] Check-in failed: " + e.getMessage());
            // Return a proper JSON error response instead of just a string
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tournamentId}/participants/detailed")
    @Operation(summary = "Get tournament participants with user details", description = "Retrieves all participants for a specific tournament with full user information (name, email, etc.)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved participants with user details")
    })
    public List<TournamentParticipantDTO> getTournamentParticipantsWithDetails(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        List<TournamentParticipant> participants = tournamentService.getParticipantsWithUserDetails(tournamentId);
        return participants.stream()
                .map(TournamentParticipantDTO::new)
                .toList();
    }

    @PostMapping("/{tournamentId}/start")
    @Operation(summary = "Start tournament", description = "Starts a tournament, changing its status to IN_PROGRESS. This freezes all registrations, check-ins, and cancellations. Notifications are sent to all participants.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament started successfully"),
            @ApiResponse(responseCode = "400", description = "Tournament cannot be started"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Only the organizer can start the tournament")
    })
    public ResponseEntity<?> startTournament(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verify user is the organizer
        Optional<Tournament> tournamentOpt = tournamentService.getTournamentById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }

        Tournament tournament = tournamentOpt.get();
        if (!tournament.getOrganizerId().equals(currentUser.get().getId())
                && !Boolean.TRUE.equals(currentUser.get().getIsAdmin())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the organizer can start the tournament");
        }

        try {
            Tournament startedTournament = tournamentService.startTournament(tournamentId);
            return ResponseEntity.ok(startedTournament);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{tournamentId}/participants/{participantId}")
    @Operation(summary = "Remove participant", description = "Removes a participant from a tournament. Only the organizer can remove participants. A notification is sent to the removed participant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Participant removed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Only the organizer can remove participants")
    })
    public ResponseEntity<?> removeParticipant(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId,
            @Parameter(description = "Unique identifier of the participant") @PathVariable Long participantId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verify user is the organizer
        Optional<Tournament> tournamentOpt = tournamentService.getTournamentById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }

        Tournament tournament = tournamentOpt.get();
        if (!tournament.getOrganizerId().equals(currentUser.get().getId())
                && !Boolean.TRUE.equals(currentUser.get().getIsAdmin())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the organizer can remove participants");
        }

        try {
            tournamentService.removeParticipant(tournamentId, participantId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/{tournamentId}/complete")
    @Operation(summary = "Complete tournament", description = "Completes a tournament and sets placements for winners. Awards points to placed participants.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Only the organizer can complete the tournament")
    })
    public ResponseEntity<?> completeTournament(
            @Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId,
            @Parameter(description = "List of placements") @RequestBody CompleteTournamentRequest request) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Verify user is the organizer
        Optional<Tournament> tournamentOpt = tournamentService.getTournamentById(tournamentId);
        if (tournamentOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Tournament not found");
        }

        Tournament tournament = tournamentOpt.get();
        if (!tournament.getOrganizerId().equals(currentUser.get().getId())
                && !Boolean.TRUE.equals(currentUser.get().getIsAdmin())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only the organizer can complete the tournament");
        }

        try {
            Tournament completedTournament = tournamentService.completeTournament(tournamentId,
                    request.getPlacements());
            return ResponseEntity.ok(completedTournament);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Request DTO for completing a tournament
     */
    public static class CompleteTournamentRequest {
        private List<TournamentService.PlacementDTO> placements;

        public List<TournamentService.PlacementDTO> getPlacements() {
            return placements;
        }

        public void setPlacements(List<TournamentService.PlacementDTO> placements) {
            this.placements = placements;
        }
    }

    // ========== TOURNAMENT UPDATES (LIVE MESSAGES & PHOTOS) ==========

    @PostMapping("/{tournamentId}/updates")
    @Operation(summary = "Add tournament update", description = "Add a new live update to a tournament (only organizer)")
    public ResponseEntity<?> addTournamentUpdate(
            @PathVariable Long tournamentId,
            @RequestBody TournamentUpdateRequest request) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Devi essere autenticato");
        }

        try {
            com.tcg.arena.model.TournamentUpdate update = tournamentService.addTournamentUpdate(
                    tournamentId,
                    currentUser.get().getId(),
                    request.getMessage(),
                    request.getImageBase64());
            return ResponseEntity.ok(update);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tournamentId}/updates")
    @Operation(summary = "Get tournament updates", description = "Get all live updates for a tournament (public - client should verify access)")
    public ResponseEntity<?> getTournamentUpdates(@PathVariable Long tournamentId) {
        try {
            // Public endpoint - returns all updates for the tournament
            // Client apps should verify if user is participant before showing
            List<com.tcg.arena.model.TournamentUpdate> updates = tournamentService
                    .getTournamentUpdatesPublic(tournamentId);
            return ResponseEntity.ok(updates);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{tournamentId}/updates/{updateId}")
    @Operation(summary = "Delete tournament update", description = "Delete a live update (only organizer)")
    public ResponseEntity<?> deleteTournamentUpdate(
            @PathVariable Long tournamentId,
            @PathVariable Long updateId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Devi essere autenticato");
        }

        try {
            tournamentService.deleteTournamentUpdate(tournamentId, updateId, currentUser.get().getId());
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{tournamentId}/updates/count")
    @Operation(summary = "Get tournament update count", description = "Get the number of updates for a tournament")
    public ResponseEntity<?> getTournamentUpdateCount(@PathVariable Long tournamentId) {
        int count = tournamentService.getTournamentUpdateCount(tournamentId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    // ===== TOURNAMENT APPROVAL WORKFLOW ENDPOINTS =====

    @PostMapping("/request")
    @Operation(summary = "Request a tournament", description = "Allows a customer to request a tournament at a shop. The request needs shop owner approval.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament request created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request data"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<?> requestTournament(@RequestBody TournamentRequestDTO requestDTO) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Devi essere autenticato"));
        }

        try {
            Tournament tournament = tournamentService.createTournamentRequest(requestDTO, currentUser.get().getId());
            return ResponseEntity.ok(tournament);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/approve")
    @Operation(summary = "Approve tournament request", description = "Approves a pending tournament request. Only the shop owner can approve.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament approved successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to approve this tournament"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<?> approveTournament(@PathVariable Long id) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Devi essere autenticato"));
        }

        try {
            Tournament tournament = tournamentService.approveTournament(id, currentUser.get().getId());
            return ResponseEntity.ok(tournament);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/reject")
    @Operation(summary = "Reject tournament request", description = "Rejects a pending tournament request. Only the shop owner can reject.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tournament rejected successfully"),
            @ApiResponse(responseCode = "403", description = "Not authorized to reject this tournament"),
            @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<?> rejectTournament(
            @PathVariable Long id,
            @RequestBody(required = false) TournamentRejectionDTO rejectionDTO) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Devi essere autenticato"));
        }

        try {
            String reason = rejectionDTO != null ? rejectionDTO.getReason() : null;
            Tournament tournament = tournamentService.rejectTournament(id, currentUser.get().getId(), reason);
            return ResponseEntity.ok(tournament);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/pending-requests")
    @Operation(summary = "Get pending tournament requests", description = "Gets all pending tournament requests for shops owned by the current merchant")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved pending requests"),
            @ApiResponse(responseCode = "403", description = "Only merchants can access this endpoint")
    })
    public ResponseEntity<?> getPendingTournamentRequests() {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Devi essere autenticato"));
        }

        if (!Boolean.TRUE.equals(currentUser.get().getIsMerchant())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Solo i merchant possono accedere a questa risorsa"));
        }

        List<Tournament> pendingRequests = tournamentService.getPendingTournamentRequestsForMerchant(currentUser.get().getId());
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * Request DTO for creating a tournament request
     */
    public static class TournamentRequestDTO {
        private String title;
        private String description;
        private String tcgType;
        private String type;
        private String startDate;
        private String endDate;
        private Integer maxParticipants;
        private Double entryFee;
        private String prizePool;
        private Long shopId; // The shop where the tournament is requested

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTcgType() { return tcgType; }
        public void setTcgType(String tcgType) { this.tcgType = tcgType; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        
        public Integer getMaxParticipants() { return maxParticipants; }
        public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
        
        public Double getEntryFee() { return entryFee; }
        public void setEntryFee(Double entryFee) { this.entryFee = entryFee; }
        
        public String getPrizePool() { return prizePool; }
        public void setPrizePool(String prizePool) { this.prizePool = prizePool; }
        
        public Long getShopId() { return shopId; }
        public void setShopId(Long shopId) { this.shopId = shopId; }
    }

    /**
     * Request DTO for rejecting a tournament
     */
    public static class TournamentRejectionDTO {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * Request DTO for adding a tournament update
     */
    public static class TournamentUpdateRequest {
        private String message;
        private String imageBase64;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getImageBase64() {
            return imageBase64;
        }

        public void setImageBase64(String imageBase64) {
            this.imageBase64 = imageBase64;
        }
    }
}