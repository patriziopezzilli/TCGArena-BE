package com.tcg.arena.controller;

import com.tcg.arena.model.CardCondition;
import com.tcg.arena.model.Deck;
import com.tcg.arena.model.GradeService;
import com.tcg.arena.model.User;
import com.tcg.arena.model.UserCard;
import com.tcg.arena.model.UserCardDeck;
import com.tcg.arena.repository.DeckRepository;
import com.tcg.arena.repository.UserCardDeckRepository;
import com.tcg.arena.service.CardTemplateService;
import com.tcg.arena.service.UserCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// DTO for partial card updates
class UserCardUpdateDto {
    private String condition;
    private Boolean isGraded;
    private String gradeService;
    private Integer gradeScore;
    private Double purchasePrice;
    private Long deckId;

    // Default constructor
    public UserCardUpdateDto() {
    }

    // Getters and setters
    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public Boolean getIsGraded() {
        return isGraded;
    }

    public void setIsGraded(Boolean isGraded) {
        this.isGraded = isGraded;
    }

    public String getGradeService() {
        return gradeService;
    }

    public void setGradeService(String gradeService) {
        this.gradeService = gradeService;
    }

    public Integer getGradeScore() {
        return gradeScore;
    }

    public void setGradeScore(Integer gradeScore) {
        this.gradeScore = gradeScore;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }
}

// DTO for deck assignment response
class DeckAssignmentDto {
    private Long deckId;
    private String deckName;
    private String deckType;
    private String tcgType;

    public DeckAssignmentDto(Deck deck) {
        this.deckId = deck.getId();
        this.deckName = deck.getName();
        this.deckType = deck.getDeckType() != null ? deck.getDeckType().name() : null;
        this.tcgType = deck.getTcgType() != null ? deck.getTcgType().name() : null;
    }

    public Long getDeckId() {
        return deckId;
    }

    public String getDeckName() {
        return deckName;
    }

    public String getDeckType() {
        return deckType;
    }

    public String getTcgType() {
        return tcgType;
    }
}

@RestController
@RequestMapping("/api/cards")
@Tag(name = "User Cards", description = "API for managing user card collections in the TCG Arena system")
public class UserCardController {

    @Autowired
    private UserCardService userCardService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @Autowired
    private DeckRepository deckRepository;

    @Autowired
    private UserCardDeckRepository userCardDeckRepository;

    @GetMapping("/collection")
    @Operation(summary = "Get current user's card collection", description = "Retrieves all user cards owned by the currently authenticated user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user's card collection")
    })
    public ResponseEntity<List<UserCard>> getUserCollection(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
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

    // ===== DECK ASSIGNMENT ENDPOINTS =====

    @PostMapping("/collection/{userCardId}/assign-to-deck/{deckId}")
    @Operation(summary = "Assign user card to a deck", description = "Assigns an inventory card to a specific deck (many-to-many)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card assigned to deck successfully"),
            @ApiResponse(responseCode = "404", description = "User card or deck not found"),
            @ApiResponse(responseCode = "403", description = "User not authorized"),
            @ApiResponse(responseCode = "409", description = "Card already assigned to this deck")
    })
    @Transactional
    public ResponseEntity<UserCardDeck> assignCardToDeck(
            @Parameter(description = "User card ID") @PathVariable Long userCardId,
            @Parameter(description = "Deck ID") @PathVariable Long deckId,
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
            return ResponseEntity.status(403).build();
        }

        Optional<Deck> deckOpt = deckRepository.findById(deckId);
        if (deckOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Deck deck = deckOpt.get();
        // Verify user owns the deck
        if (!deck.getOwnerId().equals(userOpt.get().getId())) {
            return ResponseEntity.status(403).build();
        }

        // Check if already assigned
        if (userCardDeckRepository.existsByUserCardIdAndDeckId(userCardId, deckId)) {
            return ResponseEntity.status(409).build(); // Conflict
        }

        UserCardDeck assignment = new UserCardDeck(userCard, deck);
        UserCardDeck saved = userCardDeckRepository.save(assignment);
        return ResponseEntity.ok(saved);
    }

    @DeleteMapping("/collection/{userCardId}/remove-from-deck/{deckId}")
    @Operation(summary = "Remove user card from a deck", description = "Removes an inventory card from a specific deck")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Card removed from deck successfully"),
            @ApiResponse(responseCode = "404", description = "Assignment not found"),
            @ApiResponse(responseCode = "403", description = "User not authorized")
    })
    @Transactional
    public ResponseEntity<Void> removeCardFromDeck(
            @Parameter(description = "User card ID") @PathVariable Long userCardId,
            @Parameter(description = "Deck ID") @PathVariable Long deckId,
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
            return ResponseEntity.status(403).build();
        }

        Optional<UserCardDeck> assignmentOpt = userCardDeckRepository.findByUserCardIdAndDeckId(userCardId, deckId);
        if (assignmentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        userCardDeckRepository.delete(assignmentOpt.get());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/collection/{userCardId}/decks")
    @Operation(summary = "Get all decks containing this card", description = "Returns list of decks the user card is assigned to")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved deck assignments"),
            @ApiResponse(responseCode = "404", description = "User card not found"),
            @ApiResponse(responseCode = "403", description = "User not authorized")
    })
    public ResponseEntity<List<DeckAssignmentDto>> getCardDecks(
            @Parameter(description = "User card ID") @PathVariable Long userCardId,
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
            return ResponseEntity.status(403).build();
        }

        List<UserCardDeck> assignments = userCardDeckRepository.findByUserCardId(userCardId);
        List<DeckAssignmentDto> dtos = assignments.stream()
                .map(a -> new DeckAssignmentDto(a.getDeck()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}