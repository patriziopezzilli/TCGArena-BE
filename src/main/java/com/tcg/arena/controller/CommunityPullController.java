package com.tcg.arena.controller;

import com.tcg.arena.dto.CommunityPullDTO;
import com.tcg.arena.dto.CreatePullRequest;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.UserRepository;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.service.CommunityPullService;
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
@RequestMapping("/api/community/pulls")
@Tag(name = "Community Pulls", description = "API for community card pulls showcase")
public class CommunityPullController {

    @Autowired
    private CommunityPullService pullService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get community pulls feed", description = "Get paginated feed of pulls, optionally filtered by TCG type")
    public ResponseEntity<Page<CommunityPullDTO>> getPulls(
            @Parameter(description = "Optional TCG Type filter") @RequestParam(required = false) TCGType tcgType,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size,
            @RequestHeader(value = "Authorization", required = false) String token) {

        Long userId = resolveUserIdFromToken(token);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(pullService.getPulls(tcgType, userId, pageable));
    }

    @PostMapping
    @Operation(summary = "Create a new pull post", description = "Upload a photo of a pull")
    public ResponseEntity<?> createPull(
            @RequestBody CreatePullRequest request,
            @RequestHeader("Authorization") String token) {

        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            CommunityPullDTO pull = pullService.createPull(request, userId);
            return ResponseEntity.ok(pull);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "Toggle like", description = "Like or unlike a pull")
    public ResponseEntity<?> toggleLike(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            CommunityPullDTO updatedPull = pullService.toggleLike(id, userId);
            return ResponseEntity.ok(updatedPull);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a pull", description = "Delete a pull (only owner)")
    public ResponseEntity<?> deletePull(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {

        Long userId = resolveUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        try {
            pullService.deletePull(id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}
