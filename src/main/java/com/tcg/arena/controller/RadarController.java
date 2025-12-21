package com.tcg.arena.controller;

import com.tcg.arena.dto.LocationUpdateRequest;
import com.tcg.arena.dto.RadarUserDto;
import com.tcg.arena.service.RadarService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/radar")
@Tag(name = "Trade Radar", description = "API for Trade Radar features")
public class RadarController {

    @Autowired
    private RadarService radarService;

    @Autowired
    private UserService userService;

    @PutMapping("/location")
    @Operation(summary = "Update user location", description = "Updates the user's current location for Radar")
    public ResponseEntity<Void> updateLocation(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody LocationUpdateRequest request) {

        // In a real app we'd get ID from userDetails
        // For now finding by username
        // Assuming userService.findByUsername exists
        return userService.getUserByUsername(userDetails.getUsername())
                .map(user -> {
                    radarService.updateUserLocation(user.getId(), request);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/nearby")
    @Operation(summary = "Get nearby users", description = "Get users within specific radius")
    public ResponseEntity<List<RadarUserDto>> getNearbyUsers(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "10.0") double radiusKm) {

        return userService.getUserByUsername(userDetails.getUsername())
                .map(user -> {
                    List<RadarUserDto> users = radarService.getNearbyUsers(user.getId(), latitude, longitude, radiusKm);
                    return ResponseEntity.ok(users);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/ping/{targetUserId}")
    @Operation(summary = "Ping a user", description = "Send a ping notification to another user")
    public ResponseEntity<Void> pingUser(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long targetUserId) {

        return userService.getUserByUsername(userDetails.getUsername())
                .map(user -> {
                    radarService.sendPing(user.getId(), targetUserId);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
