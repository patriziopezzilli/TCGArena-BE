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
import java.util.Optional;

@RestController
@RequestMapping("/api/tournaments")
@Tag(name = "Tournaments", description = "API for managing tournaments in the TCG Arena system")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get all tournaments", description = "Retrieves a list of all tournaments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of tournaments")
    })
    public List<Tournament> getAllTournaments() {
        return tournamentService.getAllTournaments();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tournament by ID", description = "Retrieves a specific tournament by its unique ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tournament found and returned"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<Tournament> getTournamentById(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long id) {
        return tournamentService.getTournamentById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get upcoming tournaments", description = "Retrieves a list of upcoming tournaments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of upcoming tournaments")
    })
    public List<Tournament> getUpcomingTournaments() {
        return tournamentService.getUpcomingTournaments();
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby tournaments", description = "Retrieves tournaments within a specified radius from given coordinates")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of nearby tournaments")
    })
    public List<Tournament> getNearbyTournaments(
            @Parameter(description = "Latitude of the location") @RequestParam double latitude,
            @Parameter(description = "Longitude of the location") @RequestParam double longitude,
            @Parameter(description = "Search radius in kilometers") @RequestParam(defaultValue = "50") double radiusKm) {
        return tournamentService.getNearbyTournaments(latitude, longitude, radiusKm);
    }

    @PostMapping
    @Operation(summary = "Create a new tournament", description = "Creates a new tournament in the system. Only merchants can create tournaments.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tournament created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid tournament data provided"),
        @ApiResponse(responseCode = "403", description = "Only merchants can create tournaments")
    })
    public ResponseEntity<Tournament> createTournament(@Parameter(description = "Tournament object to be created") @RequestBody Tournament tournament) {
        // Check if current user is a merchant
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty() || !Boolean.TRUE.equals(currentUser.get().getIsMerchant())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // Set the organizerId from the current user
        tournament.setOrganizerId(currentUser.get().getId());
        
        Tournament savedTournament = tournamentService.saveTournament(tournament);
        return ResponseEntity.ok(savedTournament);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tournament", description = "Updates the details of an existing tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tournament updated successfully"),
        @ApiResponse(responseCode = "404", description = "Tournament not found")
    })
    public ResponseEntity<Tournament> updateTournament(@Parameter(description = "Unique identifier of the tournament to update") @PathVariable Long id, @Parameter(description = "Updated tournament object") @RequestBody Tournament tournament) {
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
    public ResponseEntity<Void> deleteTournament(@Parameter(description = "Unique identifier of the tournament to delete") @PathVariable Long id) {
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
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<TournamentParticipant> registerForTournament(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            TournamentParticipant participant = tournamentService.registerForTournament(tournamentId, currentUser.get().getId());
            return ResponseEntity.ok(participant);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{tournamentId}/register")
    @Operation(summary = "Unregister from a tournament", description = "Unregisters the current user from a tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully unregistered from tournament"),
        @ApiResponse(responseCode = "404", description = "User not registered for this tournament"),
        @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<Void> unregisterFromTournament(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        Optional<User> currentUser = userService.getCurrentUser();
        if (currentUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (tournamentService.unregisterFromTournament(tournamentId, currentUser.get().getId())) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/{tournamentId}/participants")
    @Operation(summary = "Get tournament participants", description = "Retrieves all participants for a specific tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved participants")
    })
    public List<TournamentParticipant> getTournamentParticipants(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getTournamentParticipants(tournamentId);
    }

    @GetMapping("/{tournamentId}/participants/registered")
    @Operation(summary = "Get registered participants", description = "Retrieves only registered participants (not waiting list) for a specific tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved registered participants")
    })
    public List<TournamentParticipant> getRegisteredParticipants(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getRegisteredParticipants(tournamentId);
    }

    @GetMapping("/{tournamentId}/participants/waiting")
    @Operation(summary = "Get waiting list", description = "Retrieves participants on the waiting list for a specific tournament")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved waiting list")
    })
    public List<TournamentParticipant> getWaitingList(@Parameter(description = "Unique identifier of the tournament") @PathVariable Long tournamentId) {
        return tournamentService.getWaitingList(tournamentId);
    }
}