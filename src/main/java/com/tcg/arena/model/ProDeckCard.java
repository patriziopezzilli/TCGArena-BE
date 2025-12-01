package com.tcg.arena.model;

import jakarta.persistence.*;

@Entity
@Table(name = "pro_deck_cards")
public class ProDeckCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pro_deck_id", nullable = false)
    private ProDeck proDeck;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_template_id", nullable = false)
    private CardTemplate cardTemplate;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private String section; // "main" or "sideboard"

    // Constructors
    public ProDeckCard() {}

    public ProDeckCard(ProDeck proDeck, CardTemplate cardTemplate, Integer quantity, String section) {
        this.proDeck = proDeck;
        this.cardTemplate = cardTemplate;
        this.quantity = quantity;
        this.section = section;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProDeck getProDeck() {
        return proDeck;
    }

    public void setProDeck(ProDeck proDeck) {
        this.proDeck = proDeck;
    }

    public CardTemplate getCardTemplate() {
        return cardTemplate;
    }

    public void setCardTemplate(CardTemplate cardTemplate) {
        this.cardTemplate = cardTemplate;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }
}