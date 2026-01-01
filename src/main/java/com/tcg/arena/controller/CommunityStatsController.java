package com.tcg.arena.controller;

import com.tcg.arena.dto.CommunityStatsDTO;
import com.tcg.arena.service.CommunityStatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/stats")
public class CommunityStatsController {

    @Autowired
    private CommunityStatsService statsService;

    @Autowired
    private com.tcg.arena.repository.UserRepository userRepository;

    /**
     * Get community statistics for the current user
     */
    @GetMapping
    public ResponseEntity<CommunityStatsDTO> getStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        CommunityStatsDTO stats = statsService.getStatsForUser(userId);
        return ResponseEntity.ok(stats);
    }

    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"))
                .getId();
    }
}
