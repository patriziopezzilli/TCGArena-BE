package com.tcg.arena.dto;

import com.tcg.arena.model.ThreadType;
import java.util.List;

public class CreateThreadRequest {

    private String tcgType;
    private ThreadType threadType = ThreadType.DISCUSSION;
    private String title;
    private String content;
    private List<String> pollOptions;

    // Constructors
    public CreateThreadRequest() {
    }

    public CreateThreadRequest(String tcgType, String title, String content) {
        this.tcgType = tcgType;
        this.threadType = ThreadType.DISCUSSION;
        this.title = title;
        this.content = content;
    }

    public CreateThreadRequest(String tcgType, ThreadType threadType, String title, String content, List<String> pollOptions) {
        this.tcgType = tcgType;
        this.threadType = threadType;
        this.title = title;
        this.content = content;
        this.pollOptions = pollOptions;
    }

    // Getters and Setters
    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public ThreadType getThreadType() {
        return threadType;
    }

    public void setThreadType(ThreadType threadType) {
        this.threadType = threadType;
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

    public List<String> getPollOptions() {
        return pollOptions;
    }

    public void setPollOptions(List<String> pollOptions) {
        this.pollOptions = pollOptions;
    }
}
