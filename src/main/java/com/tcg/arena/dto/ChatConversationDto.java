package com.tcg.arena.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ChatConversationDto {
    private Long id;
    private List<RadarUserDto> participants;
    private LocalDateTime lastMessageAt;
    private String type; // FREE, TRADE
    private String contextJson;
    private String lastMessagePreview;
    private Integer unreadCount;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<RadarUserDto> getParticipants() {
        return participants;
    }

    public void setParticipants(List<RadarUserDto> participants) {
        this.participants = participants;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public String getLastMessagePreview() {
        return lastMessagePreview;
    }

    public void setLastMessagePreview(String lastMessagePreview) {
        this.lastMessagePreview = lastMessagePreview;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    private String status; // ACTIVE, COMPLETED
    private Boolean isReadOnly;
    private Boolean agreementReached; // true if trade completed successfully, false if closed without agreement
    private Long initiatorId;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }
}
