package com.tcg.arena.controller;

import com.tcg.arena.model.Suggestion;
import com.tcg.arena.model.User;
import com.tcg.arena.service.SuggestionService;
import com.tcg.arena.security.JwtTokenUtil;
import com.tcg.arena.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suggestions")
public class SuggestionController {

    @Autowired
    private SuggestionService suggestionService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Submit a suggestion", description = "Allows a user to submit a suggestion")
    public ResponseEntity<?> submitSuggestion(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> payload) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String email = jwtTokenUtil.getUsernameFromToken(jwt);
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            return ResponseEntity.status(401).body("Invalid User");
        }

        Long userId = user.getId();
        String text = payload.get("text");

        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body("Text is required");
        }

        Suggestion suggestion = suggestionService.createSuggestion(userId, text);
        return ResponseEntity.ok(suggestion);
    }

    @GetMapping
    @Operation(summary = "Get all suggestions", description = "Admin only: Get list of user suggestions")
    public ResponseEntity<List<Suggestion>> getAllSuggestions() {
        // ideally add admin check here
        return ResponseEntity.ok(suggestionService.getAllSuggestions());
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark suggestion as read")
    public ResponseEntity<?> markAsRead(@PathVariable Long id) {
        suggestionService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
}
