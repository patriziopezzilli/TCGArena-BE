package com.tcg.arena.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pull_likes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "pull_id", "user_id" })
})
public class PullLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_id", nullable = false)
    private CommunityPull pull;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "liked_at")
    private LocalDateTime likedAt;

    public PullLike() {
        this.likedAt = LocalDateTime.now();
    }

    public PullLike(CommunityPull pull, User user) {
        this.pull = pull;
        this.user = user;
        this.likedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public CommunityPull getPull() {
        return pull;
    }

    public void setPull(CommunityPull pull) {
        this.pull = pull;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getLikedAt() {
        return likedAt;
    }

    public void setLikedAt(LocalDateTime likedAt) {
        this.likedAt = likedAt;
    }
}
