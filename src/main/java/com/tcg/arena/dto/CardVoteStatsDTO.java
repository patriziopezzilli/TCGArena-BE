package com.tcg.arena.dto;

import com.tcg.arena.model.CardVote;

public class CardVoteStatsDTO {
    private Long cardTemplateId;
    private Long likesCount;
    private Long dislikesCount;
    private Double likePercentage;
    private Long totalVotes;

    private CardVote.VoteType userVote;

    public CardVoteStatsDTO() {
    }

    public CardVoteStatsDTO(Long cardTemplateId, Long likesCount, Long dislikesCount) {
        this.cardTemplateId = cardTemplateId;
        this.likesCount = likesCount;
        this.dislikesCount = dislikesCount;
        this.totalVotes = likesCount + dislikesCount;
        this.likePercentage = totalVotes > 0 ? (likesCount * 100.0) / totalVotes : 0.0;
    }

    public Long getCardTemplateId() {
        return cardTemplateId;
    }

    public void setCardTemplateId(Long cardTemplateId) {
        this.cardTemplateId = cardTemplateId;
    }

    public Long getLikesCount() {
        return likesCount;
    }

    public void setLikesCount(Long likesCount) {
        this.likesCount = likesCount;
    }

    public Long getDislikesCount() {
        return dislikesCount;
    }

    public void setDislikesCount(Long dislikesCount) {
        this.dislikesCount = dislikesCount;
    }

    public Double getLikePercentage() {
        return likePercentage;
    }

    public void setLikePercentage(Double likePercentage) {
        this.likePercentage = likePercentage;
    }

    public Long getTotalVotes() {
        return totalVotes;
    }

    public void setTotalVotes(Long totalVotes) {
        this.totalVotes = totalVotes;
    }

    public CardVote.VoteType getUserVote() {
        return userVote;
    }

    public void setUserVote(CardVote.VoteType userVote) {
        this.userVote = userVote;
    }
}
