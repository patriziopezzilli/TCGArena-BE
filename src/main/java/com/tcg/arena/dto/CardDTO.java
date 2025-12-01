package com.tcg.arena.dto;

import com.tcg.arena.model.*;

import java.time.LocalDateTime;

public class CardDTO {
    private Long id;
    private String name;
    private TCGType tcgType;
    private String setCode;
    private ExpansionDTO expansion;
    private Rarity rarity;
    private String cardNumber;
    private String description;
    private String imageUrl;
    private Double marketPrice;
    private Integer manaCost;
    private CardCondition condition;
    private Boolean isGraded;
    private GradeService gradeService;
    private Integer gradeScore;
    private LocalDateTime dateAdded;

    // Constructors, Getters, Setters
    public CardDTO() {}

    public CardDTO(Card card) {
        this.id = card.getId();
        this.name = card.getName();
        this.tcgType = card.getTcgType();
        this.setCode = card.getSetCode();
        if (card.getExpansion() != null) {
            this.expansion = new ExpansionDTO(card.getExpansion());
        }
        this.rarity = card.getRarity();
        this.cardNumber = card.getCardNumber();
        this.description = card.getDescription();
        this.imageUrl = card.getImageUrl();
        this.marketPrice = card.getMarketPrice();
        this.manaCost = card.getManaCost();
        this.condition = card.getCondition();
        this.isGraded = card.getIsGraded();
        this.gradeService = card.getGradeService();
        this.gradeScore = card.getGradeScore();
        this.dateAdded = card.getDateAdded();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public TCGType getTcgType() { return tcgType; }
    public void setTcgType(TCGType tcgType) { this.tcgType = tcgType; }

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }

    public ExpansionDTO getExpansion() { return expansion; }
    public void setExpansion(ExpansionDTO expansion) { this.expansion = expansion; }

    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }

    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Double getMarketPrice() { return marketPrice; }
    public void setMarketPrice(Double marketPrice) { this.marketPrice = marketPrice; }

    public Integer getManaCost() { return manaCost; }
    public void setManaCost(Integer manaCost) { this.manaCost = manaCost; }

    public CardCondition getCondition() { return condition; }
    public void setCondition(CardCondition condition) { this.condition = condition; }

    public Boolean getIsGraded() { return isGraded; }
    public void setIsGraded(Boolean isGraded) { this.isGraded = isGraded; }

    public GradeService getGradeService() { return gradeService; }
    public void setGradeService(GradeService gradeService) { this.gradeService = gradeService; }

    public Integer getGradeScore() { return gradeScore; }
    public void setGradeScore(Integer gradeScore) { this.gradeScore = gradeScore; }

    public LocalDateTime getDateAdded() { return dateAdded; }
    public void setDateAdded(LocalDateTime dateAdded) { this.dateAdded = dateAdded; }
}