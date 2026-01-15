package com.tcg.arena.controller;

import com.tcg.arena.dto.*;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.service.CommunityThreadService;
import com.tcg.arena.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/threads")
public class CommunityThreadController {

    @Autowired
    private CommunityThreadService threadService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * Get paginated list of threads, optionally filtered by TCG type
     */
    @GetMapping
    public ResponseEntity<Page<CommunityThreadDTO>> getThreads(
            @RequestParam(required = false) String tcgType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long currentUserId = extractUserIdFromToken(authHeader);
        Page<CommunityThreadDTO> threads = threadService.getThreads(tcgType, currentUserId, page, size);
        return ResponseEntity.ok(threads);
    }

    /**
     * Get single thread with all responses
     */
    @GetMapping("/{id}")
    public ResponseEntity<CommunityThreadDTO> getThread(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            Long currentUserId = extractUserIdFromToken(authHeader);
            CommunityThreadDTO thread = threadService.getThreadById(id, currentUserId);
            return ResponseEntity.ok(thread);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Create a new thread
     */
    @PostMapping
    public ResponseEntity<?> createThread(
            @RequestBody CreateThreadRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long creatorId = extractUserIdFromToken(authHeader);
            if (creatorId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Autenticazione richiesta");
            }

            CommunityThreadDTO thread = threadService.createThread(request, creatorId);
            return ResponseEntity.status(HttpStatus.CREATED).body(thread);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Add a response to a thread (one per user)
     */
    @PostMapping("/{id}/responses")
    public ResponseEntity<?> addResponse(
            @PathVariable Long id,
            @RequestBody CreateThreadResponseRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long responderId = extractUserIdFromToken(authHeader);
            if (responderId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Autenticazione richiesta");
            }

            ThreadResponseDTO response = threadService.addResponse(id, request, responderId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Check if current user can respond to a thread
     */
    @GetMapping("/{id}/can-respond")
    public ResponseEntity<Boolean> canRespond(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.ok(false);
        }

        boolean canRespond = threadService.canUserRespond(id, userId);
        return ResponseEntity.ok(canRespond);
    }

    /**
     * Vote on a poll option
     */
    @PostMapping("/poll-options/{pollOptionId}/vote")
    public ResponseEntity<?> voteOnPoll(
            @PathVariable Long pollOptionId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            Long voterId = extractUserIdFromToken(authHeader);
            if (voterId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Autenticazione richiesta");
            }

            PollOptionDTO result = threadService.voteOnPoll(pollOptionId, voterId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Check if current user has voted on a poll
     */
    @GetMapping("/{threadId}/poll/has-voted")
    public ResponseEntity<Boolean> hasUserVotedOnPoll(
            @PathVariable Long threadId,
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserIdFromToken(authHeader);
        if (userId == null) {
            return ResponseEntity.ok(false);
        }

        boolean hasVoted = threadService.hasUserVotedOnPoll(threadId, userId);
        return ResponseEntity.ok(hasVoted);
    }

    /**
     * Extract user ID from JWT token
     */
    private Long extractUserIdFromToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        try {
            String token = authHeader.substring(7);
            String username = jwtTokenUtil.getUsernameFromToken(token);
            return userRepository.findByUsername(username)
                    .map(user -> user.getId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
}
