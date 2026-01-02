package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity for global chat messages in Arena Chat.
 * Messages are public and visible to all authenticated users.
 */
@Entity
@Table(name = "global_chat_messages", indexes = {
        @Index(name = "idx_global_chat_timestamp", columnList = "timestamp DESC")
})
public class GlobalChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String username;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    // Constructors
    public GlobalChatMessage() {
    }

    public GlobalChatMessage(Long userId, String username, String displayName, String content) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.content = content;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
