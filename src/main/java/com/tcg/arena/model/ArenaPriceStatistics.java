package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Price statistics for a card variant from TCG API.
 * Contains min/max/avg/change data for 7d, 30d, 90d, 1y, and all-time periods.
 */
@Entity
@Table(name = "arena_price_statistics")
public class ArenaPriceStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false, unique = true)
    @JsonIgnore
    private ArenaCardVariant variant;

    // 7 days statistics
    private Double stats7dMin;
    private Double stats7dMax;
    private Double stats7dAvg;
    private Double stats7dChange;
    private Double stats7dChangePercent;

    // 30 days statistics
    private Double stats30dMin;
    private Double stats30dMax;
    private Double stats30dAvg;
    private Double stats30dChange;
    private Double stats30dChangePercent;

    // 90 days statistics
    private Double stats90dMin;
    private Double stats90dMax;
    private Double stats90dAvg;
    private Double stats90dChange;
    private Double stats90dChangePercent;

    // 1 year statistics
    private Double stats1yMin;
    private Double stats1yMax;
    private Double stats1yAvg;
    private Double stats1yChange;
    private Double stats1yChangePercent;

    // All-time statistics
    private Double statsAllTimeMin;
    private Double statsAllTimeMax;
    private Double statsAllTimeAvg;

    private LocalDateTime lastUpdated;

    // Constructors
    public ArenaPriceStatistics() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ArenaCardVariant getVariant() {
        return variant;
    }

    public void setVariant(ArenaCardVariant variant) {
        this.variant = variant;
    }

    // 7d getters/setters
    public Double getStats7dMin() {
        return stats7dMin;
    }

    public void setStats7dMin(Double stats7dMin) {
        this.stats7dMin = stats7dMin;
    }

    public Double getStats7dMax() {
        return stats7dMax;
    }

    public void setStats7dMax(Double stats7dMax) {
        this.stats7dMax = stats7dMax;
    }

    public Double getStats7dAvg() {
        return stats7dAvg;
    }

    public void setStats7dAvg(Double stats7dAvg) {
        this.stats7dAvg = stats7dAvg;
    }

    public Double getStats7dChange() {
        return stats7dChange;
    }

    public void setStats7dChange(Double stats7dChange) {
        this.stats7dChange = stats7dChange;
    }

    public Double getStats7dChangePercent() {
        return stats7dChangePercent;
    }

    public void setStats7dChangePercent(Double stats7dChangePercent) {
        this.stats7dChangePercent = stats7dChangePercent;
    }

    // 30d getters/setters
    public Double getStats30dMin() {
        return stats30dMin;
    }

    public void setStats30dMin(Double stats30dMin) {
        this.stats30dMin = stats30dMin;
    }

    public Double getStats30dMax() {
        return stats30dMax;
    }

    public void setStats30dMax(Double stats30dMax) {
        this.stats30dMax = stats30dMax;
    }

    public Double getStats30dAvg() {
        return stats30dAvg;
    }

    public void setStats30dAvg(Double stats30dAvg) {
        this.stats30dAvg = stats30dAvg;
    }

    public Double getStats30dChange() {
        return stats30dChange;
    }

    public void setStats30dChange(Double stats30dChange) {
        this.stats30dChange = stats30dChange;
    }

    public Double getStats30dChangePercent() {
        return stats30dChangePercent;
    }

    public void setStats30dChangePercent(Double stats30dChangePercent) {
        this.stats30dChangePercent = stats30dChangePercent;
    }

    // 90d getters/setters
    public Double getStats90dMin() {
        return stats90dMin;
    }

    public void setStats90dMin(Double stats90dMin) {
        this.stats90dMin = stats90dMin;
    }

    public Double getStats90dMax() {
        return stats90dMax;
    }

    public void setStats90dMax(Double stats90dMax) {
        this.stats90dMax = stats90dMax;
    }

    public Double getStats90dAvg() {
        return stats90dAvg;
    }

    public void setStats90dAvg(Double stats90dAvg) {
        this.stats90dAvg = stats90dAvg;
    }

    public Double getStats90dChange() {
        return stats90dChange;
    }

    public void setStats90dChange(Double stats90dChange) {
        this.stats90dChange = stats90dChange;
    }

    public Double getStats90dChangePercent() {
        return stats90dChangePercent;
    }

    public void setStats90dChangePercent(Double stats90dChangePercent) {
        this.stats90dChangePercent = stats90dChangePercent;
    }

    // 1y getters/setters
    public Double getStats1yMin() {
        return stats1yMin;
    }

    public void setStats1yMin(Double stats1yMin) {
        this.stats1yMin = stats1yMin;
    }

    public Double getStats1yMax() {
        return stats1yMax;
    }

    public void setStats1yMax(Double stats1yMax) {
        this.stats1yMax = stats1yMax;
    }

    public Double getStats1yAvg() {
        return stats1yAvg;
    }

    public void setStats1yAvg(Double stats1yAvg) {
        this.stats1yAvg = stats1yAvg;
    }

    public Double getStats1yChange() {
        return stats1yChange;
    }

    public void setStats1yChange(Double stats1yChange) {
        this.stats1yChange = stats1yChange;
    }

    public Double getStats1yChangePercent() {
        return stats1yChangePercent;
    }

    public void setStats1yChangePercent(Double stats1yChangePercent) {
        this.stats1yChangePercent = stats1yChangePercent;
    }

    // All-time getters/setters
    public Double getStatsAllTimeMin() {
        return statsAllTimeMin;
    }

    public void setStatsAllTimeMin(Double statsAllTimeMin) {
        this.statsAllTimeMin = statsAllTimeMin;
    }

    public Double getStatsAllTimeMax() {
        return statsAllTimeMax;
    }

    public void setStatsAllTimeMax(Double statsAllTimeMax) {
        this.statsAllTimeMax = statsAllTimeMax;
    }

    public Double getStatsAllTimeAvg() {
        return statsAllTimeAvg;
    }

    public void setStatsAllTimeAvg(Double statsAllTimeAvg) {
        this.statsAllTimeAvg = statsAllTimeAvg;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
