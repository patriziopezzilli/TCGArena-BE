package com.tcg.arena.dto;

import com.tcg.arena.model.TradeListType;

public class TradeListEntryDTO {
    private Long id;
    private Long cardTemplateId;
    private String cardName;
    private String imageUrl;
    private TradeListType type;
    private String tcgType;
    private String rarity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardTemplateId() {
        return cardTemplateId;
    }

    public void setCardTemplateId(Long cardTemplateId) {
        this.cardTemplateId = cardTemplateId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public TradeListType getType() {
        return type;
    }

    public void setType(TradeListType type) {
        this.type = type;
    }

    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
}
