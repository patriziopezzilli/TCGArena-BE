package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Represents a card variant from JustTCG API.
 * Each variant is a unique combination of condition + printing for a card,
 * with its own price and tracking information.
 */
@Entity
@Table(name = "arena_card_variants", indexes = {
        @Index(name = "idx_arena_variants_card", columnList = "card_id"),
        @Index(name = "idx_arena_variants_condition", columnList = "condition"),
        @Index(name = "idx_arena_variants_printing", columnList = "printing")
})
public class ArenaCardVariant {

    @Id
    @Column(nullable = false, length = 600)
    private String id; // JustTCG variant ID:
                       // "pokemon-battle-academy-fire-energy-22-charizard-stamped_near-mint"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    @JsonIgnore
    private ArenaCard card;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArenaCardCondition condition; // SEALED, NEAR_MINT, LIGHTLY_PLAYED, etc.

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ArenaPrinting printing; // NORMAL, FOIL

    private Double price;

    private Long lastUpdatedEpoch; // Unix timestamp from JustTCG API

    private LocalDateTime lastUpdated; // Converted for internal use

    @OneToOne(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private ArenaPriceStatistics statistics;

    // Constructors
    public ArenaCardVariant() {
    }

    public ArenaCardVariant(String id, ArenaCardCondition condition, ArenaPrinting printing) {
        this.id = id;
        this.condition = condition;
        this.printing = printing;
    }

    // Helper to set statistics
    public void setStatisticsEntity(ArenaPriceStatistics stats) {
        this.statistics = stats;
        if (stats != null) {
            stats.setVariant(this);
        }
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public ArenaCard getCard() {
        return card;
    }

    public void setCard(ArenaCard card) {
        this.card = card;
    }

    public ArenaCardCondition getCondition() {
        return condition;
    }

    public void setCondition(ArenaCardCondition condition) {
        this.condition = condition;
    }

    public ArenaPrinting getPrinting() {
        return printing;
    }

    public void setPrinting(ArenaPrinting printing) {
        this.printing = printing;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Long getLastUpdatedEpoch() {
        return lastUpdatedEpoch;
    }

    public void setLastUpdatedEpoch(Long lastUpdatedEpoch) {
        this.lastUpdatedEpoch = lastUpdatedEpoch;
        // Auto-convert to LocalDateTime
        if (lastUpdatedEpoch != null) {
            this.lastUpdated = LocalDateTime.ofInstant(
                    Instant.ofEpochSecond(lastUpdatedEpoch),
                    ZoneId.systemDefault());
        }
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public ArenaPriceStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(ArenaPriceStatistics statistics) {
        this.statistics = statistics;
    }
}
