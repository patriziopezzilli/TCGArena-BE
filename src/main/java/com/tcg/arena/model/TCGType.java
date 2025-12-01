package com.tcg.arena.model;

public enum TCGType {
    POKEMON("Pokemon"),
    ONE_PIECE("One Piece"),
    MAGIC("Magic: The Gathering"),
    YUGIOH("Yu-Gi-Oh!"),
    DIGIMON("Digimon");

    private final String displayName;

    TCGType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}