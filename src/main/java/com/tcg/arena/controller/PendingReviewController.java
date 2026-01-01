package com.tcg.arena.controller;

import com.tcg.arena.dto.PendingReviewDTO;
import com.tcg.arena.dto.SubmitReviewRequest;
import com.tcg.arena.model.PendingReview;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.PendingReviewRepository;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.security.JwtTokenUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reviews")
public class PendingReviewController {

    @Autowired
    private PendingReviewRepository pendingReviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * Get pending reviews for the authenticated user
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingReviewDTO>> getPendingReviews(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<PendingReview> reviews = pendingReviewRepository
                .findByReviewerIdAndCompletedAtIsNullOrderByCreatedAtDesc(userId);

        List<PendingReviewDTO> dtos = reviews.stream()
                .map(PendingReviewDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get count of pending reviews for badge
     */
    @GetMapping("/pending/count")
    public ResponseEntity<Map<String, Long>> getPendingReviewsCount(
            @RequestHeader("Authorization") String authHeader) {

        Long userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        long count = pendingReviewRepository.countByReviewerIdAndCompletedAtIsNull(userId);

        Map<String, Long> response = new HashMap<>();
        response.put("count", count);

        return ResponseEntity.ok(response);
    }

    /**
     * Submit a review
     */
    @PostMapping("/pending/{id}/submit")
    public ResponseEntity<?> submitReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @Valid @RequestBody SubmitReviewRequest request) {

        Long userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        // Find the pending review
        PendingReview review = pendingReviewRepository.findById(id).orElse(null);
        if (review == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify ownership
        if (!review.getReviewer().getId().equals(userId)) {
            return ResponseEntity.status(403).body("Non sei autorizzato a inviare questa recensione");
        }

        // Check if already completed
        if (!review.isPending()) {
            return ResponseEntity.badRequest().body("Recensione già completata");
        }

        // Update the review
        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review.setCompletedAt(LocalDateTime.now());
        pendingReviewRepository.save(review);

        // Update the reviewee's trade rating
        // Instead of complex average calculation, we just add the new rating
        // This is consistent with how ChatService handles points-as-ratings
        User reviewee = review.getReviewee();
        reviewee.addTradeRating(request.getRating());
        userRepository.save(reviewee);

        return ResponseEntity.ok(PendingReviewDTO.fromEntity(review));
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        try {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            User user = userRepository.findByUsername(username).orElse(null);
            return user != null ? user.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
