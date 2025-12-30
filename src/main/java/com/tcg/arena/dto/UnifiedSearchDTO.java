package com.tcg.arena.dto;

import com.tcg.arena.model.CardTemplate;
import java.util.List;

/**
 * DTO for unified search results containing both cards and expansions.
 * Used by the Discover section to search across multiple entity types.
 */
public class UnifiedSearchDTO {
    private List<CardTemplate> cards;
    private List<ExpansionDTO> expansions;
    private int totalCards;
    private int totalExpansions;

    public UnifiedSearchDTO() {
    }

    public UnifiedSearchDTO(List<CardTemplate> cards, List<ExpansionDTO> expansions) {
        this.cards = cards;
        this.expansions = expansions;
        this.totalCards = cards != null ? cards.size() : 0;
        this.totalExpansions = expansions != null ? expansions.size() : 0;
    }

    // Getters and Setters
    public List<CardTemplate> getCards() {
        return cards;
    }

    public void setCards(List<CardTemplate> cards) {
        this.cards = cards;
        this.totalCards = cards != null ? cards.size() : 0;
    }

    public List<ExpansionDTO> getExpansions() {
        return expansions;
    }

    public void setExpansions(List<ExpansionDTO> expansions) {
        this.expansions = expansions;
        this.totalExpansions = expansions != null ? expansions.size() : 0;
    }

    public int getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(int totalCards) {
        this.totalCards = totalCards;
    }

    public int getTotalExpansions() {
        return totalExpansions;
    }

    public void setTotalExpansions(int totalExpansions) {
        this.totalExpansions = totalExpansions;
    }
}
