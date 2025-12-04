package com.tcg.arena.dto;

import com.tcg.arena.model.CardCondition;
import com.tcg.arena.model.CardNationality;
import com.tcg.arena.model.GradeService;

public class DeckCardUpdateDTO {
    private Integer quantity;
    private CardCondition condition;
    private Boolean isGraded;
    private GradeService gradeService;
    private String grade;
    private String certificateNumber;
    private CardNationality nationality;

    // Default constructor
    public DeckCardUpdateDTO() {}

    // Constructor with parameters
    public DeckCardUpdateDTO(Integer quantity, CardCondition condition, Boolean isGraded, GradeService gradeService, String grade, String certificateNumber, CardNationality nationality) {
        this.quantity = quantity;
        this.condition = condition;
        this.isGraded = isGraded;
        this.gradeService = gradeService;
        this.grade = grade;
        this.certificateNumber = certificateNumber;
        this.nationality = nationality;
    }

    // Getters and Setters
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public CardCondition getCondition() {
        return condition;
    }

    public void setCondition(CardCondition condition) {
        this.condition = condition;
    }

    public Boolean getIsGraded() {
        return isGraded;
    }

    public void setIsGraded(Boolean isGraded) {
        this.isGraded = isGraded;
    }

    public GradeService getGradeService() {
        return gradeService;
    }

    public void setGradeService(GradeService gradeService) {
        this.gradeService = gradeService;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getCertificateNumber() {
        return certificateNumber;
    }

    public void setCertificateNumber(String certificateNumber) {
        this.certificateNumber = certificateNumber;
    }

    public CardNationality getNationality() {
        return nationality;
    }

    public void setNationality(CardNationality nationality) {
        this.nationality = nationality;
    }
}