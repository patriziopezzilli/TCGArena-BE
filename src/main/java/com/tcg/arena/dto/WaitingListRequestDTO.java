package com.tcg.arena.dto;

import com.tcg.arena.model.WaitingListEntry;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WaitingListRequestDTO {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @NotBlank(message = "City is required")
    private String city;
    
    @NotNull(message = "User type is required")
    private WaitingListEntry.UserType userType;
}
