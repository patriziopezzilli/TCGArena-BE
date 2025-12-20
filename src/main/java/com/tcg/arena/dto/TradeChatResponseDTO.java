package com.tcg.arena.dto;

import java.util.List;

public class TradeChatResponseDTO {
    private List<TradeMessageDTO> messages;
    private String matchStatus;

    public TradeChatResponseDTO(List<TradeMessageDTO> messages, String matchStatus) {
        this.messages = messages;
        this.matchStatus = matchStatus;
    }

    public List<TradeMessageDTO> getMessages() {
        return messages;
    }

    public void setMessages(List<TradeMessageDTO> messages) {
        this.messages = messages;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }
}
