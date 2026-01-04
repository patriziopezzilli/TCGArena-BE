package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "chat_conversations")
public class ChatConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany
    @JoinTable(name = "chat_conversation_participants", joinColumns = @JoinColumn(name = "conversation_id"), inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> participants = new HashSet<>();

    @Column(nullable = false)
    private LocalDateTime lastMessageAt; // For sorting

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatType type; // FREE, TRADE

    @Column(columnDefinition = "TEXT")
    private String contextJson; // JSON string for card details if type is TRADE

    @Enumerated(EnumType.STRING)
    @Column
    private ChatStatus status; // ACTIVE, COMPLETED

    @Column
    private Boolean isReadOnly; // Lock chat when trade is completed

    @Column
    private Boolean agreementReached; // true if trade completed successfully, false if closed without agreement

    public enum ChatType {
        FREE, TRADE
    }

    public enum ChatStatus {
        ACTIVE, COMPLETED
    }

    public ChatConversation() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Set<User> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<User> participants) {
        this.participants = participants;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public ChatStatus getStatus() {
        return status;
    }

    public void setStatus(ChatStatus status) {
        this.status = status;
    }

    public Boolean getIsReadOnly() {
        return isReadOnly;
    }

    public void setIsReadOnly(Boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public Boolean getAgreementReached() {
        return agreementReached;
    }

    public void setAgreementReached(Boolean agreementReached) {
        this.agreementReached = agreementReached;
    }
}
