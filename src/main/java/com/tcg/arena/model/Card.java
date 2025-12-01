package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_template_id", nullable = false)
    private CardTemplate cardTemplate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardCondition condition;

    @Column(nullable = false)
    private Boolean isGraded = false;

    @Enumerated(EnumType.STRING)
    private GradeService gradeService;

    private Integer gradeScore;

    @Column(nullable = false)
    private LocalDateTime dateAdded;

    @Column(nullable = false)
    private Long ownerId;

    private Long deckId;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public CardTemplate getCardTemplate() {
        return cardTemplate;
    }

    public void setCardTemplate(CardTemplate cardTemplate) {
        this.cardTemplate = cardTemplate;
    }

    // Delegated getters to CardTemplate
    public String getName() {
        return cardTemplate != null ? cardTemplate.getName() : null;
    }

    public TCGType getTcgType() {
        return cardTemplate != null ? cardTemplate.getTcgType() : null;
    }

    public String getSetCode() {
        return cardTemplate != null ? cardTemplate.getSetCode() : null;
    }

    public Expansion getExpansion() {
        return cardTemplate != null ? cardTemplate.getExpansion() : null;
    }

    public Rarity getRarity() {
        return cardTemplate != null ? cardTemplate.getRarity() : null;
    }

    public String getCardNumber() {
        return cardTemplate != null ? cardTemplate.getCardNumber() : null;
    }

    public String getDescription() {
        return cardTemplate != null ? cardTemplate.getDescription() : null;
    }

    public String getImageUrl() {
        return cardTemplate != null ? cardTemplate.getImageUrl() : null;
    }

    public Double getMarketPrice() {
        return cardTemplate != null ? cardTemplate.getMarketPrice() : null;
    }

    public Integer getManaCost() {
        return cardTemplate != null ? cardTemplate.getManaCost() : null;
    }

    // Instance-specific getters and setters
    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public Boolean getIsGraded() {
        return isGraded;
    }

    public void setIsGraded(Boolean isGraded) {
        this.isGraded = isGraded;
    }

    public GradeService getGradeService() {
        return gradeService;
    }

    public void setGradeService(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    public Integer getGradeScore() {
        return gradeScore;
    }

    public void setGradeScore(Integer gradeScore) {
        this.gradeScore = gradeScore;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }
}