package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "decks")
public class Deck {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @JsonProperty("tcg_type")
    private TCGType tcgType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    @JsonProperty("deck_type")
    private DeckType deckType;

    @Column(nullable = false)
    @JsonProperty("owner_id")
    private Long ownerId;

    @Column(nullable = false)
    @JsonProperty("date_created")
    private LocalDateTime dateCreated;

    @Column(nullable = false)
    @JsonProperty("date_modified")
    private LocalDateTime dateModified;

    @Column(nullable = false)
    @JsonProperty("is_public")
    private Boolean isPublic = false;

    @Column(nullable = false)
    @JsonProperty("is_hidden")
    private Boolean isHidden = false; // Hide deck from public profile view

    @Column(nullable = false)
    private Long likes = 0L;

    @Column(length = 1000)
    private String description;

    @ElementCollection
    @CollectionTable(name = "deck_tags")
    private List<String> tags = new ArrayList<>();

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL)
    private List<DeckCard> cards = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public DeckType getDeckType() {
        return deckType != null ? deckType : DeckType.LISTA;
    }

    public void setDeckType(DeckType deckType) {
        this.deckType = deckType != null ? deckType : DeckType.LISTA;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateModified() {
        return dateModified;
    }

    public void setDateModified(LocalDateTime dateModified) {
        this.dateModified = dateModified;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Boolean getIsHidden() {
        return isHidden;
    }

    public void setIsHidden(Boolean isHidden) {
        this.isHidden = isHidden;
    }

    public Long getLikes() {
        return likes;
    }

    public void setLikes(Long likes) {
        this.likes = likes;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<DeckCard> getCards() {
        return cards;
    }

    public void setCards(List<DeckCard> cards) {
        this.cards = cards;
    }
}