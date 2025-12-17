package com.tcg.arena.dto;

import com.tcg.arena.model.TCGSet;
import java.time.LocalDateTime;

public class TCGSetDTO {
    private Long id;
    private String name;
    private String setCode;
    private String imageUrl;
    private LocalDateTime releaseDate;
    private Integer cardCount;
    private String description;

    // Constructors, Getters, Setters
    public TCGSetDTO() {}

    public TCGSetDTO(TCGSet tcgSet) {
        this.id = tcgSet.getId();
        this.name = tcgSet.getName();
        this.setCode = tcgSet.getSetCode();
        this.imageUrl = tcgSet.getImageUrl();
        this.releaseDate = tcgSet.getReleaseDate();
        this.cardCount = tcgSet.getCardCount();
        this.description = tcgSet.getDescription();
    }

    public TCGSetDTO(TCGSet tcgSet, Integer actualCardCount) {
        this.id = tcgSet.getId();
        this.name = tcgSet.getName();
        this.setCode = tcgSet.getSetCode();
        this.imageUrl = tcgSet.getImageUrl();
        this.releaseDate = tcgSet.getReleaseDate();
        this.cardCount = actualCardCount != null ? actualCardCount : tcgSet.getCardCount();
        this.description = tcgSet.getDescription();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSetCode() { return setCode; }
    public void setSetCode(String setCode) { this.setCode = setCode; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public LocalDateTime getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDateTime releaseDate) { this.releaseDate = releaseDate; }

    public Integer getCardCount() { return cardCount; }
    public void setCardCount(Integer cardCount) { this.cardCount = cardCount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}