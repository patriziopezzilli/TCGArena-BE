package com.tcg.arena.dto;

import java.util.Objects;

public class WaitingListResponseDTO {
    private String message;
    private Boolean success;
    
    public WaitingListResponseDTO() {
    }
    
    public WaitingListResponseDTO(String message, Boolean success) {
        this.message = message;
        this.success = success;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaitingListResponseDTO that = (WaitingListResponseDTO) o;
        return Objects.equals(message, that.message) &&
               Objects.equals(success, that.success);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(message, success);
    }
    
    @Override
    public String toString() {
        return "WaitingListResponseDTO{" +
                "message='" + message + '\'' +
                ", success=" + success +
                '}';
    }
}
