package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a card set from JustTCG API.
 * Each set belongs to a game and contains multiple cards.
 */
@Entity
@Table(name = "arena_sets")
public class ArenaSet {

    @Id
    @Column(nullable = false, length = 255)
    private String id; // JustTCG set ID: "battle-academy-pokemon", "scarlet-violet-pokemon"

    @Column(nullable = false)
    private String name; // Display name: "Battle Academy", "Scarlet & Violet"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    @JsonIgnore
    private ArenaGame game;

    // Denormalized for queries - maps to same column as 'game' relation
    @Column(name = "game_id", insertable = false, updatable = false, length = 50)
    private String gameId;

    private Integer cardsCount; // Number of cards in the set

    private LocalDate releaseDate;

    @Column(length = 1000)
    private String imageUrl;

    @OneToMany(mappedBy = "set", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ArenaCard> cards = new ArrayList<>();

    private LocalDateTime lastSync;

    // Constructors
    public ArenaSet() {
    }

    public ArenaSet(String id, String name) {
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

    public ArenaGame getGame() {
        return game;
    }

    public void setGame(ArenaGame game) {
        this.game = game;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public Integer getCardsCount() {
        return cardsCount;
    }

    public void setCardsCount(Integer cardsCount) {
        this.cardsCount = cardsCount;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ArenaCard> getCards() {
        return cards;
    }

    public void setCards(List<ArenaCard> cards) {
        this.cards = cards;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }
}
