package com.tcg.arena.controller;

import com.tcg.arena.dto.HomeDashboardDTO;
import com.tcg.arena.model.User;
import com.tcg.arena.service.HomeDashboardService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/home/dashboard")
@Tag(name = "Home Dashboard", description = "API for the home dashboard aggregation")
public class HomeDashboardController {

    @Autowired
    private HomeDashboardService homeDashboardService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get home dashboard data", description = "Retrieves aggregated data for the home dashboard")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved dashboard data"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<HomeDashboardDTO> getDashboardData(
            @Parameter(description = "Latitude of the user") @RequestParam(required = false) Double latitude,
            @Parameter(description = "Longitude of the user") @RequestParam(required = false) Double longitude,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        String username = principal.getName();
        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));

        HomeDashboardDTO dashboardData = homeDashboardService.getDashboardData(user, latitude, longitude);
        return ResponseEntity.ok(dashboardData);
    }
}
