package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_cards")
public class UserCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "card_template_id", nullable = false)
    private CardTemplate cardTemplate;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardCondition condition;

    @Column(nullable = false)
    private Boolean isGraded = false;

    @Enumerated(EnumType.STRING)
    private GradeService gradeService;

    private Integer gradeScore;

    private Double purchasePrice;

    private LocalDateTime dateAcquired;

    @Column(nullable = false)
    private LocalDateTime dateAdded;

    private Long deckId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CardNationality nationality;
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

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

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

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public LocalDateTime getDateAcquired() {
        return dateAcquired;
    }

    public void setDateAcquired(LocalDateTime dateAcquired) {
        this.dateAcquired = dateAcquired;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }

    public CardNationality getNationality() {
        return nationality != null ? nationality : CardNationality.EN; // Default to English
    }

    public void setNationality(CardNationality nationality) {
        this.nationality = nationality;
    }
}