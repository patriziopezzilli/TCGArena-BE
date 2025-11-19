package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.Tournament;
import com.example.tcgbackend.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tournaments")
@Tag(name = "Tournaments", description = "API for managing tournaments in the TCG Arena system")
public class TournamentController {

    @Autowired
    private TournamentService tournamentService;

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
    @Operation(summary = "Create a new tournament", description = "Creates a new tournament in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Tournament created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid tournament data provided")
    })
    public Tournament createTournament(@Parameter(description = "Tournament object to be created") @RequestBody Tournament tournament) {
        return tournamentService.saveTournament(tournament);
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
}