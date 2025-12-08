package com.tcg.arena.model;

/**
 * Classification of TCG products/sets across all games.
 * Used to categorize expansions and sets for filtering and display.
 */
public enum ProductType {
    // Main product types
    BOOSTER_SET, // Main booster packs (e.g., XY Flashfire, OP-01)
    STARTER_DECK, // Pre-built starter decks (e.g., ST-01)
    STRUCTURE_DECK, // Pre-built themed decks (Yu-Gi-Oh!)

    // Special products
    SPECIAL_SET, // Sub-sets (Shiny Vault, Trainer Gallery)
    PREMIUM_PACK, // Premium/gift products
    PROMO, // Promo cards
    TIN, // Collector tins
    BOX_SET, // Gift/box sets
    THEME_BOOSTER, // Theme boosters (Digimon)

    // Magic-specific
    MASTERS_SET, // Masters reprint sets
    COMMANDER_SET, // Commander pre-cons
    SUPPLEMENTAL, // Supplemental products

    // Flesh and Blood specific
    STANDALONE_SET,

    // Unknown/default
    OTHER
}
