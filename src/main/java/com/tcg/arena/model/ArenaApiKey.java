package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an API key for accessing the Arena API.
 * Used for authentication and rate limiting.
 */
@Entity
@Table(name = "arena_api_keys", indexes = {
        @Index(name = "idx_arena_api_key", columnList = "apiKey", unique = true)
})
public class ArenaApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 64)
    private String apiKey;

    @Column(nullable = false)
    private String name; // Client/application name

    @Column(nullable = false)
    private String email; // Contact email

    @Column(length = 500)
    private String description; // Optional description

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArenaApiPlan plan = ArenaApiPlan.FREE;

    // Rate limiting
    private Integer requestsToday = 0;
    private LocalDate requestsResetDate;

    // Status
    private boolean active = true;

    // Timestamps
    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastUsedAt;

    // Constructors
    public ArenaApiKey() {
        this.apiKey = generateApiKey();
        this.createdAt = LocalDateTime.now();
        this.requestsResetDate = LocalDate.now();
    }

    public ArenaApiKey(String name, String email, ArenaApiPlan plan) {
        this();
        this.name = name;
        this.email = email;
        this.plan = plan;
    }

    /**
     * Generate a secure API key.
     */
    private static String generateApiKey() {
        return "arena_" + UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Regenerate the API key.
     */
    public void regenerateKey() {
        this.apiKey = generateApiKey();
    }

    /**
     * Check if the API key can make more requests today.
     */
    public boolean canMakeRequest() {
        if (!active)
            return false;
        if (plan.isUnlimited())
            return true;

        resetIfNewDay();
        return requestsToday < plan.getDailyRequestLimit();
    }

    /**
     * Increment request counter and update last used timestamp.
     * Returns true if the request was allowed, false if rate limited.
     */
    public boolean recordRequest() {
        if (!canMakeRequest())
            return false;

        resetIfNewDay();
        requestsToday++;
        lastUsedAt = LocalDateTime.now();
        return true;
    }

    /**
     * Reset counter if it's a new day.
     */
    private void resetIfNewDay() {
        LocalDate today = LocalDate.now();
        if (requestsResetDate == null || !requestsResetDate.equals(today)) {
            requestsToday = 0;
            requestsResetDate = today;
        }
    }

    /**
     * Get remaining requests for today.
     */
    public int getRemainingRequests() {
        if (plan.isUnlimited())
            return Integer.MAX_VALUE;
        resetIfNewDay();
        return Math.max(0, plan.getDailyRequestLimit() - requestsToday);
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ArenaApiPlan getPlan() {
        return plan;
    }

    public void setPlan(ArenaApiPlan plan) {
        this.plan = plan;
    }

    public Integer getRequestsToday() {
        return requestsToday;
    }

    public void setRequestsToday(Integer requestsToday) {
        this.requestsToday = requestsToday;
    }

    public LocalDate getRequestsResetDate() {
        return requestsResetDate;
    }

    public void setRequestsResetDate(LocalDate requestsResetDate) {
        this.requestsResetDate = requestsResetDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
