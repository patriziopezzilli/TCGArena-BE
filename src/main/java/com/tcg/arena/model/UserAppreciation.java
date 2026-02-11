package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_appreciations", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "target_user_id", "liker_user_id" })
})
public class UserAppreciation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "liker_user_id", nullable = false)
    private Long likerUserId;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    public UserAppreciation() {
    }

    public UserAppreciation(Long targetUserId, Long likerUserId) {
        this.targetUserId = targetUserId;
        this.likerUserId = likerUserId;
        this.dateCreated = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public Long getLikerUserId() {
        return likerUserId;
    }

    public void setLikerUserId(Long likerUserId) {
        this.likerUserId = likerUserId;
    }

    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }
}
