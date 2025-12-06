package com.tcg.arena.controller;

import com.tcg.arena.model.ParticipantStatus;
import com.tcg.arena.model.TournamentParticipant;
import java.time.LocalDateTime;

public class TournamentParticipantDTO {
    private Long id;
    private Long tournamentId;
    private Long userId;
    private String username;
    private String displayName;
    private String email;
    private LocalDateTime registrationDate;
    private Boolean hasPaid;
    private ParticipantStatus status;
    private Integer placement;
    private LocalDateTime checkedInAt;
    private String checkInCode;

    // Constructors
    public TournamentParticipantDTO() {}

    public TournamentParticipantDTO(TournamentParticipant participant) {
        this.id = participant.getId();
        this.tournamentId = participant.getTournamentId();
        this.userId = participant.getUserId();
        this.registrationDate = participant.getRegistrationDate();
        this.hasPaid = participant.getHasPaid();
        this.status = participant.getStatus();
        this.placement = participant.getPlacement();
        this.checkedInAt = participant.getCheckedInAt();
        this.checkInCode = participant.getCheckInCode();

        // User details
        if (participant.getUser() != null) {
            this.username = participant.getUser().getUsername();
            this.displayName = participant.getUser().getDisplayName();
            this.email = participant.getUser().getEmail();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTournamentId() { return tournamentId; }
    public void setTournamentId(Long tournamentId) { this.tournamentId = tournamentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public Boolean getHasPaid() { return hasPaid; }
    public void setHasPaid(Boolean hasPaid) { this.hasPaid = hasPaid; }

    public ParticipantStatus getStatus() { return status; }
    public void setStatus(ParticipantStatus status) { this.status = status; }

    public Integer getPlacement() { return placement; }
    public void setPlacement(Integer placement) { this.placement = placement; }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public String getCheckInCode() { return checkInCode; }
    public void setCheckInCode(String checkInCode) { this.checkInCode = checkInCode; }
}