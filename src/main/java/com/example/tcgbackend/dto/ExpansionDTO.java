package com.example.tcgbackend.dto;

import com.example.tcgbackend.model.Expansion;
import com.example.tcgbackend.model.TCGType;

public class ExpansionDTO {
    private Long id;
    private String title;
    private TCGType tcgType;
    private String imageUrl;

    // Constructors, Getters, Setters
    public ExpansionDTO() {}

    public ExpansionDTO(Expansion expansion) {
        this.id = expansion.getId();
        this.title = expansion.getTitle();
        this.tcgType = expansion.getTcgType();
        this.imageUrl = expansion.getImageUrl();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TCGType getTcgType() { return tcgType; }
    public void setTcgType(TCGType tcgType) { this.tcgType = tcgType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}