package com.tcg.arena.dto;

import com.tcg.arena.model.CommunityEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class CommunityEventDTO {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime eventDate;
    private String locationType;
    private String locationName;
    private Long shopId;
    private Double latitude;
    private Double longitude;
    private Integer maxParticipants;
    private Integer currentParticipants;
    private String tcgType;
    private String status;
    private String creatorUsername;
    private String creatorDisplayName;
    private String creatorAvatarUrl;
    private Long creatorId;
    private boolean isJoined;
    private boolean isCreator;
    private boolean isFull;
    private LocalDateTime createdAt;
    private List<EventParticipantDTO> participants;

    // Default constructor
    public CommunityEventDTO() {
    }

    // Factory method from entity
    public static CommunityEventDTO fromEntity(CommunityEvent event, Long currentUserId) {
        CommunityEventDTO dto = new CommunityEventDTO();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate());
        dto.setLocationType(event.getLocationType().name());
        dto.setLocationName(event.getLocationName());
        dto.setShopId(event.getShop() != null ? event.getShop().getId() : null);
        dto.setLatitude(event.getLatitude());
        dto.setLongitude(event.getLongitude());
        dto.setMaxParticipants(event.getMaxParticipants());
        dto.setCurrentParticipants(event.getCurrentParticipantsCount());
        dto.setTcgType(event.getTcgType());
        dto.setStatus(event.getStatus().name());
        dto.setCreatorUsername(event.getCreator().getUsername());
        dto.setCreatorDisplayName(event.getCreator().getDisplayName());
        dto.setCreatorAvatarUrl(event.getCreator().getProfileImageUrl());
        dto.setCreatorId(event.getCreator().getId());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setFull(event.isFull());

        // Convert participants to DTOs (only JOINED status)
        List<EventParticipantDTO> participantDTOs = event.getParticipants().stream()
                .filter(p -> p.getStatus() == com.tcg.arena.model.CommunityEventParticipant.ParticipantStatus.JOINED)
                .map(EventParticipantDTO::fromEntity)
                .collect(Collectors.toList());
        dto.setParticipants(participantDTOs);

        // Check if current user is creator or joined
        if (currentUserId != null) {
            dto.setCreator(event.getCreator().getId().equals(currentUserId));
            dto.setJoined(participantDTOs.stream()
                    .anyMatch(p -> p.getUserId().equals(currentUserId)));
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDateTime eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getLocationName() {
        return locationName;
    }

    public void setLocationName(String locationName) {
        this.locationName = locationName;
    }

    public Long getShopId() {
        return shopId;
    }

    public void setShopId(Long shopId) {
        this.shopId = shopId;
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

    public Integer getMaxParticipants() {
        return maxParticipants;
    }

    public void setMaxParticipants(Integer maxParticipants) {
        this.maxParticipants = maxParticipants;
    }

    public Integer getCurrentParticipants() {
        return currentParticipants;
    }

    public void setCurrentParticipants(Integer currentParticipants) {
        this.currentParticipants = currentParticipants;
    }

    public String getTcgType() {
        return tcgType;
    }

    public void setTcgType(String tcgType) {
        this.tcgType = tcgType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatorUsername() {
        return creatorUsername;
    }

    public void setCreatorUsername(String creatorUsername) {
        this.creatorUsername = creatorUsername;
    }

    public String getCreatorDisplayName() {
        return creatorDisplayName;
    }

    public void setCreatorDisplayName(String creatorDisplayName) {
        this.creatorDisplayName = creatorDisplayName;
    }

    public String getCreatorAvatarUrl() {
        return creatorAvatarUrl;
    }

    public void setCreatorAvatarUrl(String creatorAvatarUrl) {
        this.creatorAvatarUrl = creatorAvatarUrl;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public boolean isJoined() {
        return isJoined;
    }

    public void setJoined(boolean joined) {
        isJoined = joined;
    }

    public boolean isCreator() {
        return isCreator;
    }

    public void setCreator(boolean creator) {
        isCreator = creator;
    }

    public boolean isFull() {
        return isFull;
    }

    public void setFull(boolean full) {
        isFull = full;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<EventParticipantDTO> getParticipants() {
        return participants;
    }

    public void setParticipants(List<EventParticipantDTO> participants) {
        this.participants = participants;
    }
}
