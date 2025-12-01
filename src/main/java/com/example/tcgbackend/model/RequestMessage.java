package com.example.tcgbackend.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents a message in a customer request thread
 */
@Entity
@Table(name = "request_messages", indexes = {
    @Index(name = "idx_request_id", columnList = "request_id"),
    @Index(name = "idx_sender_id", columnList = "sender_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "request_id", nullable = false)
    @JsonProperty("request_id")
    private String requestId;
    
    @Column(name = "sender_id", nullable = false)
    @JsonProperty("sender_id")
    private String senderId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    @JsonProperty("sender_type")
    private SenderType senderType;
    
    @Column(nullable = false, length = 2000)
    private String message;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
    
    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    private CustomerRequest request;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", insertable = false, updatable = false)
    private User sender;
    
    public enum SenderType {
        USER,
        MERCHANT;
        
        public String getDisplayName() {
            return switch (this) {
                case USER -> "User";
                case MERCHANT -> "Merchant";
            };
        }
    }
}
