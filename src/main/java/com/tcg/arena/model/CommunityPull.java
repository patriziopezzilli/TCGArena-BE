package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_pulls")
public class CommunityPull {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "tcg_type", nullable = false)
    private TCGType tcgType;

    // Storing image as Base64 string in DB as requested
    // PostgreSQL uses TEXT type which can store up to 1GB
    // For MySQL compatibility, use LONGTEXT in columnDefinition
    @Column(name = "image_base64", columnDefinition = "TEXT", nullable = false)
    private String imageBase64;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "pull", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PullLike> likes = new ArrayList<>();

    @Transient
    private boolean isLikedByCurrentUser;

    @Transient
    private int likesCount;

    public CommunityPull() {
        this.createdAt = LocalDateTime.now();
    }

    public CommunityPull(User user, TCGType tcgType, String imageBase64) {
        this.user = user;
        this.tcgType = tcgType;
        this.imageBase64 = imageBase64;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<PullLike> getLikes() {
        return likes;
    }

    public void setLikes(List<PullLike> likes) {
        this.likes = likes;
    }

    public boolean isLikedByCurrentUser() {
        return isLikedByCurrentUser;
    }

    public void setLikedByCurrentUser(boolean likedByCurrentUser) {
        isLikedByCurrentUser = likedByCurrentUser;
    }

    public int getLikesCount() {
        return likes.size();
    }

    public void setLikesCount(int count) {
        this.likesCount = count;
    }
}
