package com.tcg.arena.dto;

public class PollOptionDTO {

    private Long id;
    private String optionText;
    private int voteCount;
    private boolean hasCurrentUserVoted;

    // Constructors
    public PollOptionDTO() {
    }

    public PollOptionDTO(Long id, String optionText, int voteCount, boolean hasCurrentUserVoted) {
        this.id = id;
        this.optionText = optionText;
        this.voteCount = voteCount;
        this.hasCurrentUserVoted = hasCurrentUserVoted;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOptionText() {
        return optionText;
    }

    public void setOptionText(String optionText) {
        this.optionText = optionText;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public boolean isHasCurrentUserVoted() {
        return hasCurrentUserVoted;
    }

    public void setHasCurrentUserVoted(boolean hasCurrentUserVoted) {
        this.hasCurrentUserVoted = hasCurrentUserVoted;
    }
}