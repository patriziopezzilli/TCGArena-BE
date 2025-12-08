package com.tcg.arena.model;

/**
 * Unified rarity system across all TCGs.
 * Each TCG uses a subset of these rarities.
 */
public enum Rarity {
    // Universal rarities
    COMMON,
    UNCOMMON,
    RARE,

    // High tier (multiple TCGs)
    SUPER_RARE, // Pokémon, One Piece, Digimon
    ULTRA_RARE, // Pokémon, Yu-Gi-Oh!
    SECRET_RARE, // Pokémon, Yu-Gi-Oh!

    // Pokémon specific
    ILLUSTRATION_RARE,
    SPECIAL_ART_RARE,
    HYPER_RARE,
    DOUBLE_RARE,
    ACE_SPEC,
    TRAINER_GALLERY,
    SHINY,

    // Magic specific
    MYTHIC_RARE,
    TIMESHIFTED,

    // One Piece specific
    LEADER,
    ALTERNATE_ART,
    MANGA_ART,

    // Yu-Gi-Oh! specific
    GHOST_RARE,
    STARLIGHT_RARE,
    ULTIMATE_RARE,
    PRISMATIC_SECRET,

    // Digimon specific
    ALT_ART,

    // Flesh and Blood specific
    MAJESTIC,
    LEGENDARY,
    FABLED,

    // Lorcana specific
    ENCHANTED,

    // General
    PROMO,
    HOLOGRAPHIC,

    // Legacy/backward compatibility aliases (for existing database data)
    SECRET, // Maps to SECRET_RARE
    ULTRA_SECRET, // Maps to ULTRA_RARE
    MYTHIC // Maps to MYTHIC_RARE
}