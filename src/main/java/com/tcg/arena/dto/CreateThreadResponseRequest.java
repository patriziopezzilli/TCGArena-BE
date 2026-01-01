package com.tcg.arena.dto;

public class CreateThreadResponseRequest {

    private String content;

    // Constructors
    public CreateThreadResponseRequest() {
    }

    public CreateThreadResponseRequest(String content) {
        this.content = content;
    }

    // Getters and Setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
