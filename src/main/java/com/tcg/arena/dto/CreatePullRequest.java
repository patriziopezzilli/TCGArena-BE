package com.tcg.arena.dto;

import com.tcg.arena.model.TCGType;

public class CreatePullRequest {
    private TCGType tcgType;
    private String imageBase64;

    public CreatePullRequest() {
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
