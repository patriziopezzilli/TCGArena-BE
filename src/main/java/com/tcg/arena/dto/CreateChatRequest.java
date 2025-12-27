package com.tcg.arena.dto;

public class CreateChatRequest {
    private Long targetUserId;
    private String type; // FREE, TRADE
    private String contextJson;

    // Getters and Setters
    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
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
}
