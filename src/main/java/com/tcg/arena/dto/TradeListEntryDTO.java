package com.tcg.arena.dto;

import com.tcg.arena.model.TradeListType;

public class TradeListEntryDTO {
    private Long id;
    private Long cardTemplateId;
    private String cardName;
    private TradeListType type;

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

    public TradeListType getType() {
        return type;
    }

    public void setType(TradeListType type) {
        this.type = type;
    }
}
