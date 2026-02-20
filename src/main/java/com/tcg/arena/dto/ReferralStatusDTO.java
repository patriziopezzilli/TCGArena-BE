package com.tcg.arena.dto;

public class ReferralStatusDTO {
    private String username;
    private String invitationCode;
    private Integer referralsCount;

    public ReferralStatusDTO() {
    }

    public ReferralStatusDTO(String username, String invitationCode, Integer referralsCount) {
        this.username = username;
        this.invitationCode = invitationCode;
        this.referralsCount = referralsCount;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public Integer getReferralsCount() {
        return referralsCount;
    }

    public void setReferralsCount(Integer referralsCount) {
        this.referralsCount = referralsCount;
    }
}
