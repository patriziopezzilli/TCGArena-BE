package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;

public class TCGStatsDTO {
    private TCGType tcgType;
    private int expansions;
    private int sets;
    private int cards;

    // Constructors
    public TCGStatsDTO() {}

    public TCGStatsDTO(TCGType tcgType, int expansions, int sets, int cards) {
        this.tcgType = tcgType;
        this.expansions = expansions;
        this.sets = sets;
        this.cards = cards;
    }

    // Getters and Setters
    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public int getExpansions() {
        return expansions;
    }

    public void setExpansions(int expansions) {
        this.expansions = expansions;
    }

    public int getSets() {
        return sets;
    }

    public void setSets(int sets) {
        this.sets = sets;
    }

    public int getCards() {
        return cards;
    }

    public void setCards(int cards) {
        this.cards = cards;
    }
}