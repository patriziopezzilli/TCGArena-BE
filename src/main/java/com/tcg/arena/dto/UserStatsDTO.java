package com.tcg.arena.dto;

public class UserStatsDTO {
    private Integer totalCards;
    private Integer totalDecks;
    private Integer tournamentsPlayed;
    private Integer tournamentsWon;
    private Double winRate;

    // Constructors, Getters, Setters
    public UserStatsDTO() {}

    // Getters and Setters
    public Integer getTotalCards() { return totalCards; }
    public void setTotalCards(Integer totalCards) { this.totalCards = totalCards; }

    public Integer getTotalDecks() { return totalDecks; }
    public void setTotalDecks(Integer totalDecks) { this.totalDecks = totalDecks; }

    public Integer getTournamentsPlayed() { return tournamentsPlayed; }
    public void setTournamentsPlayed(Integer tournamentsPlayed) { this.tournamentsPlayed = tournamentsPlayed; }

    public Integer getTournamentsWon() { return tournamentsWon; }
    public void setTournamentsWon(Integer tournamentsWon) { this.tournamentsWon = tournamentsWon; }

    public Double getWinRate() { return winRate; }
    public void setWinRate(Double winRate) { this.winRate = winRate; }
}