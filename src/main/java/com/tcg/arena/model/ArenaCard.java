package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trading card from TCG API.
 * Contains all card metadata and references to variants (condition/printing
 * combinations).
 */
@Entity
@Table(name = "arena_cards", indexes = {
        @Index(name = "idx_arena_cards_tcgplayer", columnList = "tcgplayerId"),
        @Index(name = "idx_arena_cards_set", columnList = "set_id"),
        @Index(name = "idx_arena_cards_game", columnList = "game_id")
})
public class ArenaCard {

    @Id
    @Column(nullable = false, length = 512)
    private String id; // TCG card ID: "pokemon-battle-academy-fire-energy-22-charizard-stamped"

    @Column(nullable = false)
    private String name; // "Fire Energy (#22 Charizard Stamped)"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonIgnore
    private ArenaGame game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "set_id")
    @JsonIgnore
    private ArenaSet set;

    @Column(length = 255)
    private String setName; // Denormalized: "Battle Academy"

    @Column(length = 50)
    private String number; // Card number in set: "22", "N/A"

    @Column(length = 50)
    private String tcgplayerId; // TCGPlayer ID for direct linking

    @Column(length = 100)
    private String scryfallId; // Scryfall ID (for MTG)

    @Column(length = 100)
    private String mtgjsonId; // MTGJSON ID (for MTG)

    @Column(length = 100)
    private String rarity; // "Promo", "Common", "Rare", "Ultra Rare", etc.

    @Column(length = 2000)
    private String details; // Additional details (can be null)

    @Column(length = 1000)
    private String imageUrl;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ArenaCardVariant> variants = new ArrayList<>();

    private LocalDateTime lastSync;

    // Constructors
    public ArenaCard() {
    }

    public ArenaCard(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Helper method to add variant
    public void addVariant(ArenaCardVariant variant) {
        variants.add(variant);
        variant.setCard(this);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArenaGame getGame() {
        return game;
    }

    public void setGame(ArenaGame game) {
        this.game = game;
    }

    public ArenaSet getSet() {
        return set;
    }

    public void setSet(ArenaSet set) {
        this.set = set;
        if (set != null) {
            this.setName = set.getName();
        }
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTcgplayerId() {
        return tcgplayerId;
    }

    public void setTcgplayerId(String tcgplayerId) {
        this.tcgplayerId = tcgplayerId;
    }

    public String getScryfallId() {
        return scryfallId;
    }

    public void setScryfallId(String scryfallId) {
        this.scryfallId = scryfallId;
    }

    public String getMtgjsonId() {
        return mtgjsonId;
    }

    public void setMtgjsonId(String mtgjsonId) {
        this.mtgjsonId = mtgjsonId;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ArenaCardVariant> getVariants() {
        return variants;
    }

    public void setVariants(List<ArenaCardVariant> variants) {
        this.variants = variants;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }
}
