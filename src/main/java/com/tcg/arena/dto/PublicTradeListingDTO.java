package com.tcg.arena.dto;

import com.tcg.arena.model.TradeListEntry;

public class PublicTradeListingDTO {

    private Long id;
    private Long cardTemplateId;
    private String cardName;
    private String cardImageUrl;
    private String tcgType;
    private String rarity;
    private String listType; // WANT or HAVE
    private Long userId;
    private String username;
    private String userDisplayName;
    private String userProfileImageUrl;
    private Double userLatitude;
    private Double userLongitude;
    private String userCity;
    private Double distance; // Calculated client-side or server-side

    // Default constructor
    public PublicTradeListingDTO() {
    }

    // Factory method from entity
    public static PublicTradeListingDTO fromEntity(TradeListEntry entry) {
        PublicTradeListingDTO dto = new PublicTradeListingDTO();
        dto.setId(entry.getId());
        dto.setListType(entry.getType().name());

        // Get card info from CardTemplate
        if (entry.getCardTemplate() != null) {
            dto.setCardTemplateId(entry.getCardTemplate().getId());
            dto.setCardName(entry.getCardTemplate().getName());
            dto.setCardImageUrl(entry.getCardTemplate().getImageUrl());
            if (entry.getCardTemplate().getTcgType() != null) {
                dto.setTcgType(entry.getCardTemplate().getTcgType().name());
            }
            if (entry.getCardTemplate().getRarity() != null) {
                dto.setRarity(entry.getCardTemplate().getRarity().name());
            }
        }

        if (entry.getUser() != null) {
            dto.setUserId(entry.getUser().getId());
            dto.setUsername(entry.getUser().getUsername());
            dto.setUserDisplayName(entry.getUser().getDisplayName());
            dto.setUserProfileImageUrl(entry.getUser().getProfileImageUrl());

            if (entry.getUser().getLocation() != null) {
                dto.setUserLatitude(entry.getUser().getLocation().getLatitude());
                dto.setUserLongitude(entry.getUser().getLocation().getLongitude());
                dto.setUserCity(entry.getUser().getLocation().getCity());
            }
        }

        return dto;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardTemplateId() {
        return cardTemplateId;
    }

    public void setCardTemplateId(Long cardTemplateId) {
        this.cardTemplateId = cardTemplateId;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getListType() {
        return listType;
    }

    public void setListType(String listType) {
        this.listType = listType;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserProfileImageUrl() {
        return userProfileImageUrl;
    }

    public void setUserProfileImageUrl(String userProfileImageUrl) {
        this.userProfileImageUrl = userProfileImageUrl;
    }

    public Double getUserLatitude() {
        return userLatitude;
    }

    public void setUserLatitude(Double userLatitude) {
        this.userLatitude = userLatitude;
    }

    public Double getUserLongitude() {
        return userLongitude;
    }

    public void setUserLongitude(Double userLongitude) {
        this.userLongitude = userLongitude;
    }

    public String getUserCity() {
        return userCity;
    }

    public void setUserCity(String userCity) {
        this.userCity = userCity;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
