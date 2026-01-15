package com.tcg.arena.dto;

import com.tcg.arena.model.CardVote;

public class CardVoteRequest {
    private CardVote.VoteType voteType;

    public CardVoteRequest() {
    }

    public CardVoteRequest(CardVote.VoteType voteType) {
        this.voteType = voteType;
    }

    public CardVote.VoteType getVoteType() {
        return voteType;
    }

    public void setVoteType(CardVote.VoteType voteType) {
        this.voteType = voteType;
    }
}
