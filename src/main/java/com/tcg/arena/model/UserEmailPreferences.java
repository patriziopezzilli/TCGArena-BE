package com.tcg.arena.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_email_preferences")
public class UserEmailPreferences {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "welcome_email")
    private Boolean welcomeEmail = true;

    @Column(name = "password_reset")
    private Boolean passwordReset = true;

    @Column(name = "trade_notifications")
    private Boolean tradeNotifications = true;

    @Column(name = "event_notifications")
    private Boolean eventNotifications = true;

    @Column(name = "tournament_notifications")
    private Boolean tournamentNotifications = true;

    @Column(name = "reservation_confirmations")
    private Boolean reservationConfirmations = true;

    @Column(name = "security_alerts")
    private Boolean securityAlerts = true;

    @Column(name = "shop_notifications")
    private Boolean shopNotifications = true;

    @Column(name = "inactivity_reminders")
    private Boolean inactivityReminders = true;

    @Column(name = "import_summaries")
    private Boolean importSummaries = false;

    @Column(name = "daily_digest")
    private Boolean dailyDigest = false;

    @Column(name = "marketing_emails")
    private Boolean marketingEmails = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Constructors
    public UserEmailPreferences() {}

    public UserEmailPreferences(User user) {
        this.user = user;
        this.userId = user.getId();
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Boolean getWelcomeEmail() { return welcomeEmail; }
    public void setWelcomeEmail(Boolean welcomeEmail) { this.welcomeEmail = welcomeEmail; }

    public Boolean getPasswordReset() { return passwordReset; }
    public void setPasswordReset(Boolean passwordReset) { this.passwordReset = passwordReset; }

    public Boolean getTradeNotifications() { return tradeNotifications; }
    public void setTradeNotifications(Boolean tradeNotifications) { this.tradeNotifications = tradeNotifications; }

    public Boolean getEventNotifications() { return eventNotifications; }
    public void setEventNotifications(Boolean eventNotifications) { this.eventNotifications = eventNotifications; }

    public Boolean getTournamentNotifications() { return tournamentNotifications; }
    public void setTournamentNotifications(Boolean tournamentNotifications) { this.tournamentNotifications = tournamentNotifications; }

    public Boolean getReservationConfirmations() { return reservationConfirmations; }
    public void setReservationConfirmations(Boolean reservationConfirmations) { this.reservationConfirmations = reservationConfirmations; }

    public Boolean getSecurityAlerts() { return securityAlerts; }
    public void setSecurityAlerts(Boolean securityAlerts) { this.securityAlerts = securityAlerts; }

    public Boolean getShopNotifications() { return shopNotifications; }
    public void setShopNotifications(Boolean shopNotifications) { this.shopNotifications = shopNotifications; }

    public Boolean getInactivityReminders() { return inactivityReminders; }
    public void setInactivityReminders(Boolean inactivityReminders) { this.inactivityReminders = inactivityReminders; }

    public Boolean getImportSummaries() { return importSummaries; }
    public void setImportSummaries(Boolean importSummaries) { this.importSummaries = importSummaries; }

    public Boolean getDailyDigest() { return dailyDigest; }
    public void setDailyDigest(Boolean dailyDigest) { this.dailyDigest = dailyDigest; }

    public Boolean getMarketingEmails() { return marketingEmails; }
    public void setMarketingEmails(Boolean marketingEmails) { this.marketingEmails = marketingEmails; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
