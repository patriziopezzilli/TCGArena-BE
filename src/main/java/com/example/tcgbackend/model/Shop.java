package com.example.tcgbackend.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "shops")
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
    private Long ownerId;

    private String openingHours; // e.g., "9:00-18:00"
    private String openingDays; // e.g., "Mon-Fri,Sat"

    @OneToMany(mappedBy = "shopId", cascade = CascadeType.ALL)
    private List<ShopInventory> inventory = new ArrayList<>();

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

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public String getOpeningHours() { return openingHours; }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }

    public String getOpeningDays() { return openingDays; }
    public void setOpeningDays(String openingDays) { this.openingDays = openingDays; }

    public List<ShopInventory> getInventory() { return inventory; }
    public void setInventory(List<ShopInventory> inventory) { this.inventory = inventory; }
}