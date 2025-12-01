package com.tcg.arena.dto;

import com.tcg.arena.model.WaitingListEntry;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

public class WaitingListRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotNull(message = "User type is required")
    private WaitingListEntry.UserType userType;
    
    // Constructors
    public WaitingListRequestDTO() {
    }
    
    public WaitingListRequestDTO(String email, String city, WaitingListEntry.UserType userType) {
        this.email = email;
        this.city = city;
        this.userType = userType;
    }
    
    // Getters and setters
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public WaitingListEntry.UserType getUserType() {
        return userType;
    }
    
    public void setUserType(WaitingListEntry.UserType userType) {
        this.userType = userType;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaitingListRequestDTO that = (WaitingListRequestDTO) o;
        return Objects.equals(email, that.email) &&
               Objects.equals(city, that.city) &&
               userType == that.userType;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(email, city, userType);
    }
    
    @Override
    public String toString() {
        return "WaitingListRequestDTO{" +
                "email='" + email + '\'' +
                ", city='" + city + '\'' +
                ", userType=" + userType +
                '}';
    }
}
