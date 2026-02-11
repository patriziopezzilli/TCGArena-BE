package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "deck_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "deck_id", "user_id" })
})
public class DeckLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "deck_id", nullable = false)
    private Long deckId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public DeckLike() {
    }

    public DeckLike(Long deckId, Long userId) {
        this.deckId = deckId;
        this.userId = userId;
        this.dateCreated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDeckId() {
        return deckId;
    }

    public void setDeckId(Long deckId) {
        this.deckId = deckId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
