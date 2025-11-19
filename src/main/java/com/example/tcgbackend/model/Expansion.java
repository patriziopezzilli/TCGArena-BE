package com.example.tcgbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "expansions")
public class Expansion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TCGType tcgType;

    private String imageUrl;

    @OneToMany(mappedBy = "expansion", cascade = CascadeType.ALL)
    private List<TCGSet> sets = new ArrayList<>();

    // Computed fields (handled in service layer)
    @Transient
    public LocalDateTime getReleaseDate() {
        return sets.stream()
            .map(TCGSet::getReleaseDate)
            .max(LocalDateTime::compareTo)
            .orElse(LocalDateTime.now());
    }

    @Transient
    public int getCardCount() {
        return sets.stream()
            .mapToInt(TCGSet::getCardCount)
            .sum();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<TCGSet> getSets() {
        return sets;
    }

    public void setSets(List<TCGSet> sets) {
        this.sets = sets;
    }
}