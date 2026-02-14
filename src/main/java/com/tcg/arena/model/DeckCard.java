package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "deck_cards")
public class DeckCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "deck_id", nullable = false)
    @JsonBackReference
    private Deck deck;

    @Column(name = "card_id", nullable = false)
    @JsonProperty("card_id")
    private Long cardId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @JsonProperty("card_name")
    private String cardName;

    @JsonProperty("card_image_url")
    private String cardImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CardCondition condition;

    @Column(nullable = true)
    private Boolean isGraded;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private GradeService gradeService;

    @Column(nullable = true)
    private String grade;

    @Column(nullable = true)
    private String certificateNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CardNationality nationality;

    @Column(nullable = false, length = 50)
    private String section = "MAIN"; // Default to MAIN deck

    // Relation to CardTemplate for derived fields (rarity, set_name)
    // References CardTemplate by matching cardId field to CardTemplate.id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnore
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private CardTemplate cardTemplate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Deck getDeck() {
        return deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public CardCondition getCondition() {
        return condition != null ? condition : CardCondition.MINT;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public Boolean getIsGraded() {
        return isGraded != null ? isGraded : false;
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

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public CardNationality getNationality() {
        return nationality != null ? nationality : CardNationality.EN; // Default to English
    }

    public void setNationality(CardNationality nationality) {
        this.nationality = nationality;
    }

    public CardTemplate getCardTemplate() {
        return cardTemplate;
    }

    public void setCardTemplate(CardTemplate cardTemplate) {
        this.cardTemplate = cardTemplate;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    // Derived from CardTemplate
    @JsonProperty("rarity")
    public String getRarity() {
        if (cardTemplate != null && cardTemplate.getRarity() != null) {
            return cardTemplate.getRarity().name();
        }
        return null;
    }

    // Derived from CardTemplate's expansion
    @JsonProperty("set_name")
    public String getSetName() {
        if (cardTemplate != null && cardTemplate.getExpansion() != null) {
            return cardTemplate.getExpansion().getTitle();
        }
        return null;
    }
}