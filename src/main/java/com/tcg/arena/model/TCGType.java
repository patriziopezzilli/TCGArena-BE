package com.tcg.arena.model;

public enum TCGType {
    POKEMON("Pokemon", "Era", "Set", true, true),
    ONE_PIECE("One Piece", null, "Set", false, true),
    MAGIC("Magic: The Gathering", "Block", "Set", true, true),
    YUGIOH("Yu-Gi-Oh!", null, "Set", false, true),
    DIGIMON("Digimon", null, "Booster Set", false, true),
    LORCANA("Lorcana", null, "Set", true, false),
    RIFTBOUND("Riftbound: League of Legends", null, "Set", false, true);

    private final String displayName;
    private final String level1Label; // Era, Block, etc. (null for flat hierarchy)
    private final String level2Label; // Set, Booster Set, etc.
    private final boolean hasRotation;
    private final boolean usesBanlist;

    TCGType(String displayName, String level1Label, String level2Label, boolean hasRotation, boolean usesBanlist) {
        this.displayName = displayName;
        this.level1Label = level1Label;
        this.level2Label = level2Label;
        this.hasRotation = hasRotation;
        this.usesBanlist = usesBanlist;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLevel1Label() {
        return level1Label;
    }

    public String getLevel2Label() {
        return level2Label;
    }

    public boolean hasRotation() {
        return hasRotation;
    }

    public boolean usesBanlist() {
        return usesBanlist;
    }

    public boolean hasHierarchy() {
        return level1Label != null;
    }
}