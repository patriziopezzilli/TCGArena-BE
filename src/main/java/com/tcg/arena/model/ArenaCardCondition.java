package com.tcg.arena.model;

/**
 * Card condition enum matching JustTCG API conditions.
 * Used for ArenaCardVariant to track price per condition.
 */
public enum ArenaCardCondition {
    SEALED("S", "Sealed"),
    NEAR_MINT("NM", "Near Mint"),
    LIGHTLY_PLAYED("LP", "Lightly Played"),
    MODERATELY_PLAYED("MP", "Moderately Played"),
    HEAVILY_PLAYED("HP", "Heavily Played"),
    DAMAGED("DMG", "Damaged");

    private final String abbreviation;
    private final String displayName;

    ArenaCardCondition(String abbreviation, String displayName) {
        this.abbreviation = abbreviation;
        this.displayName = displayName;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse condition from JustTCG API string.
     * Supports both full names and abbreviations.
     */
    public static ArenaCardCondition fromString(String value) {
        if (value == null)
            return null;
        String normalized = value.trim().toUpperCase().replace(" ", "_").replace("-", "_");

        for (ArenaCardCondition condition : values()) {
            if (condition.name().equals(normalized) ||
                    condition.abbreviation.equalsIgnoreCase(value.trim()) ||
                    condition.displayName.equalsIgnoreCase(value.trim())) {
                return condition;
            }
        }
        return null;
    }
}
