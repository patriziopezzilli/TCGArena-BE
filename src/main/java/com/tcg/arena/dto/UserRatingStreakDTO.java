package com.tcg.arena.dto;

import java.time.LocalDate;

public class UserRatingStreakDTO {
    private Integer currentStreak;
    private Integer longestStreak;
    private Integer totalVotes;
    private Integer streakBreaks;
    private Integer totalRatingDays;
    private LocalDate lastRatingDate;
    private Integer nextMilestone;
    private Integer pointsToNextMilestone;
    private Boolean ratedToday;

    public UserRatingStreakDTO() {
    }

    public UserRatingStreakDTO(Integer currentStreak, Integer longestStreak, Integer totalVotes,
            Integer streakBreaks, Integer totalRatingDays, LocalDate lastRatingDate,
            Integer nextMilestone, Integer pointsToNextMilestone, Boolean ratedToday) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.totalVotes = totalVotes;
        this.streakBreaks = streakBreaks;
        this.totalRatingDays = totalRatingDays;
        this.lastRatingDate = lastRatingDate;
        this.nextMilestone = nextMilestone;
        this.pointsToNextMilestone = pointsToNextMilestone;
        this.ratedToday = ratedToday;
    }

    // Getters and Setters
    public Integer getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(Integer currentStreak) {
        this.currentStreak = currentStreak;
    }

    public Integer getLongestStreak() {
        return longestStreak;
    }

    public void setLongestStreak(Integer longestStreak) {
        this.longestStreak = longestStreak;
    }

    public Integer getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Integer totalVotes) {
        this.totalVotes = totalVotes;
    }

    public Integer getStreakBreaks() {
        return streakBreaks;
    }

    public void setStreakBreaks(Integer streakBreaks) {
        this.streakBreaks = streakBreaks;
    }

    public Integer getTotalRatingDays() {
        return totalRatingDays;
    }

    public void setTotalRatingDays(Integer totalRatingDays) {
        this.totalRatingDays = totalRatingDays;
    }

    public LocalDate getLastRatingDate() {
        return lastRatingDate;
    }

    public void setLastRatingDate(LocalDate lastRatingDate) {
        this.lastRatingDate = lastRatingDate;
    }

    public Integer getNextMilestone() {
        return nextMilestone;
    }

    public void setNextMilestone(Integer nextMilestone) {
        this.nextMilestone = nextMilestone;
    }

    public Integer getPointsToNextMilestone() {
        return pointsToNextMilestone;
    }

    public void setPointsToNextMilestone(Integer pointsToNextMilestone) {
        this.pointsToNextMilestone = pointsToNextMilestone;
    }

    public Boolean getRatedToday() {
        return ratedToday;
    }

    public void setRatedToday(Boolean ratedToday) {
        this.ratedToday = ratedToday;
    }
}
