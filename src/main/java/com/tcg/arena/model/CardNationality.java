package com.tcg.arena.model;

/**
 * Enum representing the nationality/language of a card
 */
public enum CardNationality {
    JPN("Japanese"),
    ITA("Italian"),
    EN("English"),
    COR("Korean"),
    FRA("French"),
    GER("German"),
    SPA("Spanish"),
    POR("Portuguese"),
    CHI("Chinese"),
    RUS("Russian");

    private final String displayName;

    CardNationality(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCode() {
        return name();
    }
}