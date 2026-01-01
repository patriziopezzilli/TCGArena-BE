package com.tcg.arena.dto;

public class CreateThreadRequest {

    private String tcgType;
    private String title;
    private String content;

    // Constructors
    public CreateThreadRequest() {
    }

    public CreateThreadRequest(String tcgType, String title, String content) {
        this.tcgType = tcgType;
        this.title = title;
        this.content = content;
    }

    // Getters and Setters
    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
