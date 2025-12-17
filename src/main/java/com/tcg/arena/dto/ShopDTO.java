package com.tcg.arena.dto;

import com.tcg.arena.model.OpeningHours;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopType;
import java.util.List;

public class ShopDTO {
    private Long id;
    private String name;
    private String description;
    private String address;
    private Double latitude;
    private Double longitude;
    private String phoneNumber;
    private String websiteUrl;
    private ShopType type;
    private Boolean isVerified;
    private Boolean active;
    private Long ownerId;
    
    // Legacy fields for backward compatibility
    @Deprecated
    private String openingHours;
    @Deprecated
    private String openingDays;
    
    // New structured opening hours
    private OpeningHours openingHoursStructured;
    
    private String instagramUrl;
    private String facebookUrl;
    private String twitterUrl;
    private String email;
    private String photoBase64;
    private List<String> tcgTypes;
    private List<String> services;
    private Integer reservationDurationMinutes;

    public ShopDTO() {}

    public ShopDTO(Shop shop) {
        this.id = shop.getId();
        this.name = shop.getName();
        this.description = shop.getDescription();
        this.address = shop.getAddress();
        this.latitude = shop.getLatitude();
        this.longitude = shop.getLongitude();
        this.phoneNumber = shop.getPhoneNumber();
        this.websiteUrl = shop.getWebsiteUrl();
        this.type = shop.getType();
        this.isVerified = shop.getIsVerified();
        this.active = shop.getActive();
        this.ownerId = shop.getOwnerId();
        
        // Legacy fields
        this.openingHours = shop.getOpeningHours();
        this.openingDays = shop.getOpeningDays();
        
        // New structured opening hours
        this.openingHoursStructured = shop.getOpeningHoursStructured();
        
        this.instagramUrl = shop.getInstagramUrl();
        this.facebookUrl = shop.getFacebookUrl();
        this.twitterUrl = shop.getTwitterUrl();
        this.email = shop.getEmail();
        this.photoBase64 = shop.getPhotoBase64();
        this.tcgTypes = shop.getTcgTypesList();
        this.services = shop.getServicesList();
        this.reservationDurationMinutes = shop.getReservationDurationMinutes();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public ShopType getType() { return type; }
    public void setType(ShopType type) { this.type = type; }

    public Boolean getIsVerified() { return isVerified; }
    public void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    @Deprecated
    public String getOpeningHours() { return openingHours; }
    @Deprecated
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    @Deprecated
    public String getOpeningDays() { return openingDays; }
    @Deprecated
    public void setOpeningDays(String openingDays) { this.openingDays = openingDays; }

    public OpeningHours getOpeningHoursStructured() { return openingHoursStructured; }
    public void setOpeningHoursStructured(OpeningHours openingHoursStructured) { 
        this.openingHoursStructured = openingHoursStructured; 
    }

    public String getInstagramUrl() { return instagramUrl; }
    public void setInstagramUrl(String instagramUrl) { this.instagramUrl = instagramUrl; }

    public String getFacebookUrl() { return facebookUrl; }
    public void setFacebookUrl(String facebookUrl) { this.facebookUrl = facebookUrl; }

    public String getTwitterUrl() { return twitterUrl; }
    public void setTwitterUrl(String twitterUrl) { this.twitterUrl = twitterUrl; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }

    public List<String> getTcgTypes() { return tcgTypes; }
    public void setTcgTypes(List<String> tcgTypes) { this.tcgTypes = tcgTypes; }

    public List<String> getServices() { return services; }
    public void setServices(List<String> services) { this.services = services; }

    public Integer getReservationDurationMinutes() { return reservationDurationMinutes; }
    public void setReservationDurationMinutes(Integer reservationDurationMinutes) { 
        this.reservationDurationMinutes = reservationDurationMinutes; 
    }
}
