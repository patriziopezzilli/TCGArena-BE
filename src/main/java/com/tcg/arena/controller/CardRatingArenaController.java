package com.tcg.arena.controller;

import com.tcg.arena.dto.CardVoteRequest;
import com.tcg.arena.dto.CardVoteStatsDTO;
import com.tcg.arena.dto.UserRatingStreakDTO;
import com.tcg.arena.model.CardTemplate;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.service.CardVoteService;
import com.tcg.arena.service.StreakService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/cards/rating-arena")
@Tag(name = "Card Rating Arena", description = "Tinder-style card voting and engagement feature")
public class CardRatingArenaController {

    @Autowired
    private CardVoteService voteService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StreakService streakService;

    @GetMapping("/streak")
    @Operation(summary = "Get rating streak", description = "Get user's rating streak statistics including current streak, milestones, and total votes")
    public ResponseEntity<?> getStreakStats(@RequestHeader("Authorization") String token) {
        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            UserRatingStreakDTO streak = streakService.getStreak(userId);
            return ResponseEntity.ok(streak);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/vote/{cardTemplateId}")
    @Operation(summary = "Vote on a card", description = "Submit a like or dislike vote for a card template")
    public ResponseEntity<?> submitVote(
            @PathVariable Long cardTemplateId,
            @RequestBody CardVoteRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            CardVoteStatsDTO stats = voteService.submitVote(cardTemplateId, request, userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/discover")
    @Operation(summary = "Get discover feed", description = "Get random unvoted cards for the user to rate")
    public ResponseEntity<?> getDiscoverFeed(
            @Parameter(description = "TCG Type to filter") @RequestParam TCGType tcgType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @RequestHeader("Authorization") String token) {

        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            Page<CardTemplate> cards = voteService.getDiscoverFeed(tcgType, userId, page, size);
            return ResponseEntity.ok(cards);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/stats/{cardTemplateId}")
    @Operation(summary = "Get vote statistics", description = "Get like/dislike counts for a specific card")
    public ResponseEntity<?> getVoteStats(
            @PathVariable Long cardTemplateId) {

        try {
            CardVoteStatsDTO stats = voteService.getVoteStats(cardTemplateId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/rankings/loved")
    @Operation(summary = "Get most loved cards", description = "Get cards with highest like counts")
    public ResponseEntity<Page<CardTemplate>> getTopLovedCards(
            @Parameter(description = "Optional TCG Type filter") @RequestParam(required = false) TCGType tcgType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CardTemplate> cards = voteService.getTopLovedCards(tcgType, pageable);
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/rankings/hated")
    @Operation(summary = "Get most hated cards", description = "Get cards with highest dislike counts")
    public ResponseEntity<Page<CardTemplate>> getTopHatedCards(
            @Parameter(description = "Optional TCG Type filter") @RequestParam(required = false) TCGType tcgType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<CardTemplate> cards = voteService.getTopHatedCards(tcgType, pageable);
        return ResponseEntity.ok(cards);
    }

    private Long resolveUserIdFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            return null;
        }
        try {
            String jwt = token.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(jwt);
            Optional<User> user = userRepository.findByUsername(username);
            return user.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
