package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_templates")
public class CardTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TCGType tcgType;

    @Column(nullable = false)
    private String setCode;

    @ManyToOne
    @JoinColumn(name = "expansion_id")
    private Expansion expansion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rarity rarity;

    @Column(nullable = false)
    private String cardNumber;

    @Column(length = 2000)
    private String description;

    private String imageUrl;

    // === Price fields (from JustTCG API) ===
    // Market price (general/NM)
    private Double marketPrice;

    // Condition-based prices (Near Mint, Lightly Played, etc.)
    private Double priceNearMint;
    private Double priceLightlyPlayed;
    private Double priceModeratelyPlayed;
    private Double priceHeavilyPlayed;
    private Double priceDamaged;

    // Foil/Holo variant prices
    private Double priceFoil;
    private Double priceFoilNearMint;

    // Low/Mid/High price range
    private Double priceLow;
    private Double priceHigh;

    // When prices were last updated
    private LocalDateTime lastPriceUpdate;

    // TCGPlayer ID for direct linking
    private String tcgplayerId;

    // === End price fields ===

    private Integer manaCost;

    @Column(nullable = false)
    private LocalDateTime dateCreated;

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

    public String getSetCode() {
        return setCode;
    }

    public void setSetCode(String setCode) {
        this.setCode = setCode;
    }

    public Expansion getExpansion() {
        return expansion;
    }

    public void setExpansion(Expansion expansion) {
        this.expansion = expansion;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        if(imageUrl == null) {
            imageUrl = "https://tcgplayer-cdn.tcgplayer.com/product/"+ getTcgplayerId() + "_in_1000x1000.jpg";
        }

        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }

    public Double getPriceNearMint() {
        return priceNearMint;
    }

    public void setPriceNearMint(Double priceNearMint) {
        this.priceNearMint = priceNearMint;
    }

    public Double getPriceLightlyPlayed() {
        return priceLightlyPlayed;
    }

    public void setPriceLightlyPlayed(Double priceLightlyPlayed) {
        this.priceLightlyPlayed = priceLightlyPlayed;
    }

    public Double getPriceModeratelyPlayed() {
        return priceModeratelyPlayed;
    }

    public void setPriceModeratelyPlayed(Double priceModeratelyPlayed) {
        this.priceModeratelyPlayed = priceModeratelyPlayed;
    }

    public Double getPriceHeavilyPlayed() {
        return priceHeavilyPlayed;
    }

    public void setPriceHeavilyPlayed(Double priceHeavilyPlayed) {
        this.priceHeavilyPlayed = priceHeavilyPlayed;
    }

    public Double getPriceDamaged() {
        return priceDamaged;
    }

    public void setPriceDamaged(Double priceDamaged) {
        this.priceDamaged = priceDamaged;
    }

    public Double getPriceFoil() {
        return priceFoil;
    }

    public void setPriceFoil(Double priceFoil) {
        this.priceFoil = priceFoil;
    }

    public Double getPriceFoilNearMint() {
        return priceFoilNearMint;
    }

    public void setPriceFoilNearMint(Double priceFoilNearMint) {
        this.priceFoilNearMint = priceFoilNearMint;
    }

    public Double getPriceLow() {
        return priceLow;
    }

    public void setPriceLow(Double priceLow) {
        this.priceLow = priceLow;
    }

    public Double getPriceHigh() {
        return priceHigh;
    }

    public void setPriceHigh(Double priceHigh) {
        this.priceHigh = priceHigh;
    }

    public LocalDateTime getLastPriceUpdate() {
        return lastPriceUpdate;
    }

    public void setLastPriceUpdate(LocalDateTime lastPriceUpdate) {
        this.lastPriceUpdate = lastPriceUpdate;
    }

    public String getTcgplayerId() {
        return tcgplayerId;
    }

    public void setTcgplayerId(String tcgplayerId) {
        this.tcgplayerId = tcgplayerId;
    }

    public Integer getManaCost() {
        return manaCost;
    }

    public void setManaCost(Integer manaCost) {
        this.manaCost = manaCost;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}