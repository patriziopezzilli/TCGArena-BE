package com.example.tcgbackend.controller;

import com.example.tcgbackend.model.CardCondition;
import com.example.tcgbackend.model.GradeService;
import com.example.tcgbackend.model.User;
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
import java.util.Optional;

// DTO for partial card updates
class UserCardUpdateDto {
    private String condition;
    private Boolean isGraded;
    private String gradeService;
    private Integer gradeScore;
    private Double purchasePrice;
    private Long deckId;

    // Default constructor
    public UserCardUpdateDto() {}

    // Getters and setters
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }

    public Boolean getIsGraded() { return isGraded; }
    public void setIsGraded(Boolean isGraded) { this.isGraded = isGraded; }

    public String getGradeService() { return gradeService; }
    public void setGradeService(String gradeService) { this.gradeService = gradeService; }

    public Integer getGradeScore() { return gradeScore; }
    public void setGradeScore(Integer gradeScore) { this.gradeScore = gradeScore; }

    public Double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(Double purchasePrice) { this.purchasePrice = purchasePrice; }

    public Long getDeckId() { return deckId; }
    public void setDeckId(Long deckId) { this.deckId = deckId; }
}

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
        String username = userDetails.getUsername();
        Optional<User> userOpt = userCardService.getUserByUsername(username);
        if (userOpt.isPresent()) {
            List<UserCard> userCards = userCardService.getUserCardsByUserId(userOpt.get().getId());
            return ResponseEntity.ok(userCards);
        }
        return ResponseEntity.notFound().build();
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
            String username = userDetails.getUsername();
            Optional<User> userOpt = userCardService.getUserByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            User user = userOpt.get();

            var cardTemplate = cardTemplateService.getCardTemplateById(cardTemplateId).orElseThrow();
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
        @ApiResponse(responseCode = "404", description = "User card not found"),
        @ApiResponse(responseCode = "403", description = "User not authorized to delete this card")
    })
    public ResponseEntity<Void> removeFromCollection(
            @Parameter(description = "Unique identifier of the user card") @PathVariable Long userCardId,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Optional<User> userOpt = userCardService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<UserCard> userCardOpt = userCardService.getUserCardById(userCardId);
        if (userCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserCard userCard = userCardOpt.get();
        if (!userCard.getOwner().getId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        if (userCardService.deleteUserCard(userCardId)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/collection/{userCardId}")
    @Operation(summary = "Update user card details", description = "Updates details of a specific user card in the authenticated user's collection")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User card updated successfully"),
        @ApiResponse(responseCode = "404", description = "User card not found"),
        @ApiResponse(responseCode = "403", description = "User not authorized to update this card")
    })
    public ResponseEntity<UserCard> updateUserCard(
            @Parameter(description = "Unique identifier of the user card") @PathVariable Long userCardId,
            @RequestBody UserCardUpdateDto updateDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();
        Optional<User> userOpt = userCardService.getUserByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Optional<UserCard> existingCardOpt = userCardService.getUserCardById(userCardId);
        if (existingCardOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        UserCard existingCard = existingCardOpt.get();
        if (!existingCard.getOwner().getId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(403).build(); // Forbidden
        }

        // Update only the fields provided in the DTO
        if (updateDto.getCondition() != null) {
            existingCard.setCondition(CardCondition.valueOf(updateDto.getCondition()));
        }
        if (updateDto.getIsGraded() != null) {
            existingCard.setIsGraded(updateDto.getIsGraded());
        }
        if (updateDto.getGradeService() != null) {
            existingCard.setGradeService(GradeService.valueOf(updateDto.getGradeService()));
        }
        if (updateDto.getGradeScore() != null) {
            existingCard.setGradeScore(updateDto.getGradeScore());
        }
        if (updateDto.getPurchasePrice() != null) {
            existingCard.setPurchasePrice(updateDto.getPurchasePrice());
        }
        if (updateDto.getDeckId() != null) {
            existingCard.setDeckId(updateDto.getDeckId());
        }

        return userCardService.updateUserCard(userCardId, existingCard)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
}