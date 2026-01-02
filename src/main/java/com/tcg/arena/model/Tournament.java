package com.tcg.arena.model;

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
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TournamentStatus status;

    @Column(nullable = false)
    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private Integer maxParticipants;

    @Transient
    private Integer currentParticipants;

    private Double entryFee;

    private String prizePool;

    @Column(nullable = false)
    private Long organizerId;

    // Approval workflow fields
    private Long createdByUserId; // User who created/requested the tournament

    private Long approvedByUserId; // Shop owner/merchant who approved the tournament

    private LocalDateTime approvalDate; // When the tournament was approved

    @Column(length = 500)
    private String rejectionReason; // Reason for rejection (if REJECTED status)

    @Embedded
    private TournamentLocation location;

    private Long winnerId; // Quick access to winner's user ID

    // Ranked/Official tournament fields
    @Column(nullable = false)
    private Boolean isRanked = false; // True = official tournament (no in-app registration)

    @Column(length = 500)
    private String externalRegistrationUrl; // URL for external registration (e.g., Pokemon Companion)

    // Public registration code (5 uppercase alphanumeric characters)
    @Column(unique = true, length = 5)
    private String registrationCode;

    @OneToMany(mappedBy = "tournamentId", cascade = CascadeType.ALL)
    private List<TournamentParticipant> participants = new ArrayList<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public TournamentType getType() {
        return type;
    }

    public void setType(TournamentType type) {
        this.type = type;
    }

    public TournamentStatus getStatus() {
        return status;
    }

    public void setStatus(TournamentStatus status) {
        this.status = status;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public Double getEntryFee() {
        return entryFee;
    }

    public void setEntryFee(Double entryFee) {
        this.entryFee = entryFee;
    }

    public String getPrizePool() {
        return prizePool;
    }

    public void setPrizePool(String prizePool) {
        this.prizePool = prizePool;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public TournamentLocation getLocation() {
        return location;
    }

    public void setLocation(TournamentLocation location) {
        this.location = location;
    }

    public Long getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Long winnerId) {
        this.winnerId = winnerId;
    }

    public List<TournamentParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<TournamentParticipant> participants) {
        this.participants = participants;
    }

    public Boolean getIsRanked() {
        return isRanked != null ? isRanked : false;
    }

    public void setIsRanked(Boolean isRanked) {
        this.isRanked = isRanked;
    }

    public String getExternalRegistrationUrl() {
        return externalRegistrationUrl;
    }

    public void setExternalRegistrationUrl(String externalRegistrationUrl) {
        this.externalRegistrationUrl = externalRegistrationUrl;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public Long getApprovedByUserId() {
        return approvedByUserId;
    }

    public void setApprovedByUserId(Long approvedByUserId) {
        this.approvedByUserId = approvedByUserId;
    }

    public LocalDateTime getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDateTime approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRegistrationCode() {
        return registrationCode;
    }

    public void setRegistrationCode(String registrationCode) {
        this.registrationCode = registrationCode;
    }
}