package com.tcg.arena.model;

public enum TournamentStatus {
    PENDING_APPROVAL,  // Waiting for shop owner approval
    UPCOMING, 
    REGISTRATION_OPEN, 
    REGISTRATION_CLOSED, 
    IN_PROGRESS, 
    COMPLETED, 
    CANCELLED,
    REJECTED  // Rejected by shop owner
}