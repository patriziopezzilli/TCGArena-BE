package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_threads")
public class CommunityThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(name = "tcg_type", nullable = false)
    private String tcgType;

    @Enumerated(EnumType.STRING)
    @Column(name = "thread_type", nullable = false)
    private ThreadType threadType = ThreadType.DISCUSSION;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ThreadResponse> responses = new ArrayList<>();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PollOption> pollOptions = new ArrayList<>();

    // Transient fields for DTO conversion
    @Transient
    private int responseCount;

    @Transient
    private boolean hasCurrentUserResponded;

    // Constructors
    public CommunityThread() {
    }

    public CommunityThread(User creator, String tcgType, String title, String content) {
        this.creator = creator;
        this.tcgType = tcgType;
        this.threadType = ThreadType.DISCUSSION;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    public CommunityThread(User creator, String tcgType, ThreadType threadType, String title, String content) {
        this.creator = creator;
        this.tcgType = tcgType;
        this.threadType = threadType;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public ThreadType getThreadType() {
        return threadType;
    }

    public void setThreadType(ThreadType threadType) {
        this.threadType = threadType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<ThreadResponse> getResponses() {
        return responses;
    }

    public void setResponses(List<ThreadResponse> responses) {
        this.responses = responses;
    }

    public List<PollOption> getPollOptions() {
        return pollOptions;
    }

    public void setPollOptions(List<PollOption> pollOptions) {
        this.pollOptions = pollOptions;
    }

    public int getResponseCount() {
        return responseCount;
    }

    public void setResponseCount(int responseCount) {
        this.responseCount = responseCount;
    }

    public boolean isHasCurrentUserResponded() {
        return hasCurrentUserResponded;
    }

    public void setHasCurrentUserResponded(boolean hasCurrentUserResponded) {
        this.hasCurrentUserResponded = hasCurrentUserResponded;
    }
}
