package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a trading card game from JustTCG API.
 * Supported games: MTG, Pokemon, Yu-Gi-Oh!, Lorcana, One Piece, Digimon
 */
@Entity
@Table(name = "arena_games")
public class ArenaGame {

    @Id
    @Column(nullable = false, length = 50)
    private String id; // JustTCG game ID: "pokemon", "mtg", "yugioh", etc.

    @Column(nullable = false)
    private String name; // Display name: "Magic: The Gathering", "Pokémon", etc.

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ArenaSet> sets = new ArrayList<>();

    private LocalDateTime lastSync;

    // Constructors
    public ArenaGame() {
    }

    public ArenaGame(String id, String name) {
        this.id = id;
        this.name = name;
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

    public List<ArenaSet> getSets() {
        return sets;
    }

    public void setSets(List<ArenaSet> sets) {
        this.sets = sets;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }
}
