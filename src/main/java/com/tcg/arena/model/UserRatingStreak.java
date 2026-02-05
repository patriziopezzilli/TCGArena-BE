package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "user_rating_streak")
public class UserRatingStreak {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer longestStreak = 0;

    @Column(nullable = false)
    private Integer totalRatingDays = 0;

    @Column(nullable = false)
    private Integer totalVotes = 0;

    @Column(nullable = false)
    private Integer streakBreaks = 0;

    private LocalDate lastRatingDate;

    private LocalDate streakStartDate;

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

    public Integer getCurrentStreak() {
        return currentStreak != null ? currentStreak : 0;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak != null ? longestStreak : 0;
    }

    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }

    public Integer getTotalRatingDays() {
        return totalRatingDays != null ? totalRatingDays : 0;
    }

    public void setTotalRatingDays(Integer totalRatingDays) {
        this.totalRatingDays = totalRatingDays;
    }

    public Integer getTotalVotes() {
        return totalVotes != null ? totalVotes : 0;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public Integer getStreakBreaks() {
        return streakBreaks != null ? streakBreaks : 0;
    }

    public void setStreakBreaks(Integer streakBreaks) {
        this.streakBreaks = streakBreaks;
    }

    public LocalDate getLastRatingDate() {
        return lastRatingDate;
    }

    public void setLastRatingDate(LocalDate lastRatingDate) {
        this.lastRatingDate = lastRatingDate;
    }

    public LocalDate getStreakStartDate() {
        return streakStartDate;
    }

    public void setStreakStartDate(LocalDate streakStartDate) {
        this.streakStartDate = streakStartDate;
    }

    // Helper method to increment streak
    public void incrementStreak() {
        this.currentStreak++;
        if (this.currentStreak > this.longestStreak) {
            this.longestStreak = this.currentStreak;
        }
    }

    // Helper method to reset streak
    public void resetStreak() {
        if (this.currentStreak > 0) {
            this.streakBreaks++;
        }
        this.currentStreak = 0;
        this.streakStartDate = null;
    }

    // Helper method to increment total votes
    public void incrementTotalVotes() {
        this.totalVotes++;
    }
}
