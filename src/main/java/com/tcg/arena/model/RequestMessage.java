package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a message in a customer request thread
 */
@Entity
@Table(name = "request_messages", indexes = {
    @Index(name = "idx_request_id", columnList = "request_id"),
    @Index(name = "idx_sender_id", columnList = "sender_id")
})
public class RequestMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "request_id", nullable = false)
    @JsonProperty("request_id")
    private String requestId;
    
    @Column(name = "sender_id", nullable = false)
    @JsonProperty("sender_id")
    private Long senderId;
    
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
    
    // Constructors
    public RequestMessage() {
    }
    
    public RequestMessage(String id, String requestId, Long senderId, SenderType senderType, 
                         String message, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requestId;
        this.senderId = senderId;
        this.senderType = senderType;
        this.message = message;
        this.createdAt = createdAt;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public SenderType getSenderType() {
        return senderType;
    }

    public void setSenderType(SenderType senderType) {
        this.senderType = senderType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public CustomerRequest getRequest() {
        return request;
    }
    
    public void setRequest(CustomerRequest request) {
        this.request = request;
    }
    
    public User getSender() {
        return sender;
    }
    
    public void setSender(User sender) {
        this.sender = sender;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequestMessage that = (RequestMessage) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return "RequestMessage{" +
                "id='" + id + '\'' +
                ", requestId='" + requestId + '\'' +
                ", senderId=" + senderId +
                ", senderType=" + senderType +
                ", message='" + message + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
    
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
