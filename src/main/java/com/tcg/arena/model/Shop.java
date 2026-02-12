package com.tcg.arena.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Shop {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    private String phoneNumber;
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShopType type;

    @Column(nullable = false)
    private Boolean isVerified = false;

    @Column(nullable = false)
    private Boolean active = false;

    @Column(nullable = true)
    private Long ownerId;

    @Deprecated // Use openingHoursStructured instead
    private String openingHours; // e.g., "9:00-18:00"

    @Deprecated // Use openingHoursStructured instead
    private String openingDays; // e.g., "Mon-Fri,Sat"

    // Structured opening hours stored as JSON
    @Column(columnDefinition = "TEXT")
    private String openingHoursJson;

    // Social media links
    private String instagramUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String email;

    // Shop photo as base64 string
    @Column(columnDefinition = "TEXT")
    private String photoBase64;

    // TCG Types supported (stored as comma-separated string)
    // e.g., "POKEMON,MAGIC,YUGIOH,ONE_PIECE,LORCANA"
    @Column(length = 500)
    private String tcgTypes;

    // Services offered (stored as comma-separated string)
    // e.g.,
    // "BUY_CARDS,SELL_CARDS,TRADE,TOURNAMENTS,CARD_GRADING,PREORDERS,SEALED_PRODUCTS,ACCESSORIES,PLAY_AREA,EVENTS"
    @Column(length = 1000)
    private String services;

    // Reservation settings
    @Column(name = "reservation_duration_minutes", nullable = false)
    private Integer reservationDurationMinutes = 30; // Default 30 minutes

    // Partner status - automatically set when shop creates first reward
    @Column(nullable = false)
    private Boolean isPartner = false;

    @org.hibernate.annotations.Formula("(SELECT AVG(r.rating) FROM shop_reviews r WHERE r.shop_id = id)")
    private Double averageRating;

    @org.hibernate.annotations.Formula("(SELECT COUNT(r.id) FROM shop_reviews r WHERE r.shop_id = id)")
    private Integer reviewCount;

    @OneToMany(mappedBy = "shopId", cascade = CascadeType.ALL)
    private List<ShopInventory> inventory = new ArrayList<>();

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public ShopType getType() {
        return type;
    }

    public void setType(ShopType type) {
        this.type = type;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(String openingHours) {
        this.openingHours = openingHours;
    }

    public String getOpeningDays() {
        return openingDays;
    }

    public void setOpeningDays(String openingDays) {
        this.openingDays = openingDays;
    }

    public String getOpeningHoursJson() {
        return openingHoursJson;
    }

    public void setOpeningHoursJson(String openingHoursJson) {
        this.openingHoursJson = openingHoursJson;
    }

    // Helper methods to work with structured opening hours
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public OpeningHours getOpeningHoursStructured() {
        if (openingHoursJson == null || openingHoursJson.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(openingHoursJson, OpeningHours.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public void setOpeningHoursStructured(OpeningHours hours) {
        if (hours == null) {
            this.openingHoursJson = null;
            return;
        }
        try {
            this.openingHoursJson = objectMapper.writeValueAsString(hours);
        } catch (JsonProcessingException e) {
            this.openingHoursJson = null;
        }
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public void setInstagramUrl(String instagramUrl) {
        this.instagramUrl = instagramUrl;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public void setFacebookUrl(String facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    public String getTwitterUrl() {
        return twitterUrl;
    }

    public void setTwitterUrl(String twitterUrl) {
        this.twitterUrl = twitterUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public List<ShopInventory> getInventory() {
        return inventory;
    }

    public void setInventory(List<ShopInventory> inventory) {
        this.inventory = inventory;
    }

    public String getTcgTypes() {
        return tcgTypes;
    }

    public void setTcgTypes(String tcgTypes) {
        this.tcgTypes = tcgTypes;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public Integer getReservationDurationMinutes() {
        return reservationDurationMinutes;
    }

    public void setReservationDurationMinutes(Integer reservationDurationMinutes) {
        this.reservationDurationMinutes = reservationDurationMinutes;
    }

    public Boolean getIsPartner() {
        return isPartner;
    }

    public void setIsPartner(Boolean isPartner) {
        this.isPartner = isPartner;
    }

    // Helper methods to convert comma-separated strings to/from Lists
    public List<String> getTcgTypesList() {
        if (tcgTypes == null || tcgTypes.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(tcgTypes.split(","));
    }

    public void setTcgTypesList(List<String> types) {
        if (types == null || types.isEmpty()) {
            this.tcgTypes = null;
        } else {
            this.tcgTypes = String.join(",", types);
        }
    }

    public List<String> getServicesList() {
        if (services == null || services.isEmpty()) {
            return new ArrayList<>();
        }
        return List.of(services.split(","));
    }

    public void setServicesList(List<String> servicesList) {
        if (servicesList == null || servicesList.isEmpty()) {
            this.services = null;
        } else {
            this.services = String.join(",", servicesList);
        }
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
}