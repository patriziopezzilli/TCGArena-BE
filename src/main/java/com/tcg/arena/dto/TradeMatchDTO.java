package com.tcg.arena.dto;

import com.tcg.arena.model.TradeMatch;
import java.util.List;

public class TradeMatchDTO {
    private Long id;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserAvatar; // Assuming we have avatar URL or similar
    private Double distance;
    private List<TradeListEntryDTO> matchedCards; // List of full card details
    private String type; // "THEY_HAVE_WHAT_I_WANT" or "I_HAVE_WHAT_THEY_WANT" or "BOTH"
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(Long otherUserId) {
        this.otherUserId = otherUserId;
    }

    public String getOtherUserName() {
        return otherUserName;
    }

    public void setOtherUserName(String otherUserName) {
        this.otherUserName = otherUserName;
    }

    public String getOtherUserAvatar() {
        return otherUserAvatar;
    }

    public void setOtherUserAvatar(String otherUserAvatar) {
        this.otherUserAvatar = otherUserAvatar;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public List<TradeListEntryDTO> getMatchedCards() {
        return matchedCards;
    }

    public void setMatchedCards(List<TradeListEntryDTO> matchedCards) {
        this.matchedCards = matchedCards;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
