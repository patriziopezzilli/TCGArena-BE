package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;

public class RadarTradeEntry {
    private Long id;
    private Long cardTemplateId;
    private String cardName;
    private String imageUrl;
    private TCGType tcgType;
    private String rarity;

    public RadarTradeEntry() {
    }

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

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }
}
