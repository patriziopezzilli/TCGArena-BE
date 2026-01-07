package com.tcg.arena.model;

/**
 * Printing type enum matching TCG API.
 * Used for ArenaCardVariant to distinguish Normal vs Foil printings.
 */
public enum ArenaPrinting {
    NORMAL("Normal"),
    FOIL("Foil");

    private final String displayName;

    ArenaPrinting(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Parse printing from TCG API string.
     */
    public static ArenaPrinting fromString(String value) {
        if (value == null)
            return null;
        String normalized = value.trim().toUpperCase();

        for (ArenaPrinting printing : values()) {
            if (printing.name().equals(normalized) ||
                    printing.displayName.equalsIgnoreCase(value.trim())) {
                return printing;
            }
        }
        return NORMAL; // Default to Normal if unknown
    }
}
