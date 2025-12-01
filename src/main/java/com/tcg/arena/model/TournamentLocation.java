package com.tcg.arena.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class TournamentLocation {
    private String venueName;
    private String address;
    private String city;
    private String country;
    private Double latitude;
    private Double longitude;

    // Getters and Setters
    public String getVenueName() { return venueName; }
    public void setVenueName(String venueName) { this.venueName = venueName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}