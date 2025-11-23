package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.CardTemplate;
import com.example.tcgbackend.model.UserCard;
import com.example.tcgbackend.service.CardTemplateService;
import com.example.tcgbackend.service.UserCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@Tag(name = "User Cards", description = "API for managing user card collections in the TCG Arena system")
public class UserCardController {

    @Autowired
    private UserCardService userCardService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @GetMapping("/collection")
    @Operation(summary = "Get current user's card collection", description = "Retrieves all user cards owned by the currently authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved user's card collection")
    })
    public ResponseEntity<List<UserCard>> getUserCollection(@AuthenticationPrincipal UserDetails userDetails) {
        // TODO: Extract user ID from JWT token
        // For now, return empty list or mock data
        List<UserCard> userCards = List.of();
        return ResponseEntity.ok(userCards);
    }

    @PostMapping("/{cardTemplateId}/add-to-collection")
    @Operation(summary = "Add card template to user's collection", description = "Adds a specific card template to the authenticated user's collection as a user card")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card added to collection successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request or card template not found"),
        @ApiResponse(responseCode = "404", description = "Card template not found")
    })
    public ResponseEntity<UserCard> addToCollection(
            @Parameter(description = "Unique identifier of the card template") @PathVariable Long cardTemplateId,
            @Parameter(description = "Condition of the card") @RequestParam(defaultValue = "NEAR_MINT") String condition,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // TODO: Extract user ID from JWT token
            // For now, use a mock user ID
            Long userId = 1L; // Mock user ID

            var cardTemplate = cardTemplateService.getCardTemplateById(cardTemplateId).orElseThrow();
            var user = userCardService.getUserById(userId).orElseThrow();
            CardCondition cardCondition = CardCondition.valueOf(condition.toUpperCase());
            UserCard userCard = userCardService.addCardToUserCollection(cardTemplate, user, cardCondition);
            return ResponseEntity.ok(userCard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/collection/{userCardId}")
    @Operation(summary = "Remove card from user's collection", description = "Removes a specific user card from the authenticated user's collection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Card removed from collection successfully"),
        @ApiResponse(responseCode = "404", description = "User card not found")
    })
    public ResponseEntity<Void> removeFromCollection(
            @Parameter(description = "Unique identifier of the user card") @PathVariable Long userCardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: Extract user ID from JWT token and verify ownership
        if (userCardService.deleteUserCard(userCardId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/collection/{userCardId}")
    @Operation(summary = "Update user card details", description = "Updates details of a specific user card in the authenticated user's collection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User card updated successfully"),
        @ApiResponse(responseCode = "404", description = "User card not found")
    })
    public ResponseEntity<UserCard> updateUserCard(
            @Parameter(description = "Unique identifier of the user card") @PathVariable Long userCardId,
            @RequestBody UserCard updatedCard,
            @AuthenticationPrincipal UserDetails userDetails) {
        // TODO: Extract user ID from JWT token and verify ownership
        return userCardService.updateUserCard(userCardId, updatedCard)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}