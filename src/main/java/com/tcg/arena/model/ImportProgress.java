package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "import_progress", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "tcg_type" })
})
public class ImportProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tcg_type", nullable = false, unique = true)
    private TCGType tcgType;

    @Column(name = "last_processed_page", nullable = false)
    private int lastProcessedPage = 0;

    @Column(name = "last_offset", nullable = false)
    private int lastOffset = 0;

    @Column(name = "total_pages_known")
    private Integer totalPagesKnown;

    @Column(name = "is_complete", nullable = false)
    private boolean isComplete = false;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "last_check_date")
    private LocalDateTime lastCheckDate;

    // Constructors
    public ImportProgress() {
    }

    public ImportProgress(TCGType tcgType) {
        this.tcgType = tcgType;
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public int getLastProcessedPage() {
        return lastProcessedPage;
    }

    public void setLastProcessedPage(int lastProcessedPage) {
        this.lastProcessedPage = lastProcessedPage;
        this.lastUpdated = LocalDateTime.now();
    }

    public int getLastOffset() {
        return lastOffset;
    }

    public void setLastOffset(int lastOffset) {
        this.lastOffset = lastOffset;
        this.lastUpdated = LocalDateTime.now();
    }

    public Integer getTotalPagesKnown() {
        return totalPagesKnown;
    }

    public void setTotalPagesKnown(Integer totalPagesKnown) {
        this.totalPagesKnown = totalPagesKnown;
        this.lastUpdated = LocalDateTime.now();
    }

    public boolean isComplete() {
        return isComplete;
    }

    public void setComplete(boolean complete) {
        isComplete = complete;
        this.lastUpdated = LocalDateTime.now();
        if (complete) {
            this.lastCheckDate = LocalDateTime.now();
        }
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public LocalDateTime getLastCheckDate() {
        return lastCheckDate;
    }

    public void setLastCheckDate(LocalDateTime lastCheckDate) {
        this.lastCheckDate = lastCheckDate;
    }
}