package com.tcg.arena.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "waiting_list")
public class WaitingListEntry {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String email;
    
    @Column(nullable = false)
    private String city;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private Boolean contacted = false;
    
    // Constructors
    public WaitingListEntry() {
    }
    
    public WaitingListEntry(Long id, String email, String city, UserType userType, 
                           LocalDateTime createdAt, Boolean contacted) {
        this.id = id;
        this.email = email;
        this.city = city;
        this.userType = userType;
        this.createdAt = createdAt;
        this.contacted = contacted;
    }
    
    // Getters and setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
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
    
    public UserType getUserType() {
        return userType;
    }
    
    public void setUserType(UserType userType) {
        this.userType = userType;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public Boolean getContacted() {
        return contacted;
    }
    
    public void setContacted(Boolean contacted) {
        this.contacted = contacted;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WaitingListEntry that = (WaitingListEntry) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "WaitingListEntry{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", city='" + city + '\'' +
                ", userType=" + userType +
                ", createdAt=" + createdAt +
                ", contacted=" + contacted +
                '}';
    }
    
    public enum UserType {
        PLAYER,      // Giocatore/Collezionista
        MERCHANT     // Negoziante
    }
}
