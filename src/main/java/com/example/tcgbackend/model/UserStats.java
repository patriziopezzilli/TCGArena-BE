package com.example.tcgbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_stats")
public class UserStats {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer totalCards = 0;

    @Column(nullable = false)
    private Integer totalDecks = 0;

    @Column(nullable = false)
    private Integer totalTournaments = 0;

    @Column(nullable = false)
    private Integer totalWins = 0;

    @Column(nullable = false)
    private Integer totalLosses = 0;

    @Column(nullable = false)
    private Double winRate = 0.0;

    @Enumerated(EnumType.STRING)
    private TCGType favoriteTCGType;

    @Column(nullable = false)
    private LocalDateTime joinDate;

    private LocalDateTime lastActivity;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getTotalCards() {
        return totalCards;
    }

    public void setTotalCards(Integer totalCards) {
        this.totalCards = totalCards;
    }

    public Integer getTotalDecks() {
        return totalDecks;
    }

    public void setTotalDecks(Integer totalDecks) {
        this.totalDecks = totalDecks;
    }

    public Integer getTotalTournaments() {
        return totalTournaments;
    }

    public void setTotalTournaments(Integer totalTournaments) {
        this.totalTournaments = totalTournaments;
    }

    public Integer getTotalWins() {
        return totalWins;
    }

    public void setTotalWins(Integer totalWins) {
        this.totalWins = totalWins;
    }

    public Integer getTotalLosses() {
        return totalLosses;
    }

    public void setTotalLosses(Integer totalLosses) {
        this.totalLosses = totalLosses;
    }

    public Double getWinRate() {
        return winRate;
    }

    public void setWinRate(Double winRate) {
        this.winRate = winRate;
    }

    public TCGType getFavoriteTCGType() {
        return favoriteTCGType;
    }

    public void setFavoriteTCGType(TCGType favoriteTCGType) {
        this.favoriteTCGType = favoriteTCGType;
    }

    public LocalDateTime getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(LocalDateTime joinDate) {
        this.joinDate = joinDate;
    }

    public LocalDateTime getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(LocalDateTime lastActivity) {
        this.lastActivity = lastActivity;
    }
}