package com.example.tcgbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
public class Tournament {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TCGType tcgType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    @Column(nullable = false)
    private LocalDateTime endDate;

    @Column(nullable = false)
    private Integer maxParticipants;

    @Column(nullable = false)
    private Double entryFee;

    @Column(nullable = false)
    private Double prizePool;

    @Column(nullable = false)
    private Long organizerId;

    @Embedded
    private TournamentLocation location;

    @OneToMany(mappedBy = "tournamentId", cascade = CascadeType.ALL)
    private List<TournamentParticipant> participants = new ArrayList<>();

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public TCGType getTcgType() { return tcgType; }
    public void setTcgType(TCGType tcgType) { this.tcgType = tcgType; }

    public TournamentType getType() { return type; }
    public void setType(TournamentType type) { this.type = type; }

    public TournamentStatus getStatus() { return status; }
    public void setStatus(TournamentStatus status) { this.status = status; }

    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }

    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }

    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }

    public Double getEntryFee() { return entryFee; }
    public void setEntryFee(Double entryFee) { this.entryFee = entryFee; }

    public Double getPrizePool() { return prizePool; }
    public void setPrizePool(Double prizePool) { this.prizePool = prizePool; }

    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }

    public TournamentLocation getLocation() { return location; }
    public void setLocation(TournamentLocation location) { this.location = location; }

    public List<TournamentParticipant> getParticipants() { return participants; }
    public void setParticipants(List<TournamentParticipant> participants) { this.participants = participants; }
}