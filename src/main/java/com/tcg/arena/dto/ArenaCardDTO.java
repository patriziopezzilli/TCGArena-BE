package com.tcg.arena.dto;

import com.tcg.arena.model.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO for Arena API card responses.
 */
public class ArenaCardDTO {
    private String id;
    private String name;
    private String gameId;
    private String setId;
    private String setName;
    private String number;
    private String tcgplayerId;
    private String scryfallId;
    private String mtgjsonId;
    private String rarity;
    private String details;
    private String imageUrl;
    private List<VariantDTO> variants;
    private LocalDateTime lastSync;

    // Nested DTO for variants
    public static class VariantDTO {
        private String id;
        private String condition;
        private String printing;
        private Double price;
        private Long lastUpdatedEpoch;

        public VariantDTO() {
        }

        public VariantDTO(ArenaCardVariant variant) {
            this.id = variant.getId();
            this.condition = variant.getCondition() != null ? variant.getCondition().getDisplayName() : null;
            this.printing = variant.getPrinting() != null ? variant.getPrinting().getDisplayName() : null;
            this.price = variant.getPrice();
            this.lastUpdatedEpoch = variant.getLastUpdatedEpoch();
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getPrinting() {
            return printing;
        }

        public void setPrinting(String printing) {
            this.printing = printing;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }

        public Long getLastUpdatedEpoch() {
            return lastUpdatedEpoch;
        }

        public void setLastUpdatedEpoch(Long lastUpdatedEpoch) {
            this.lastUpdatedEpoch = lastUpdatedEpoch;
        }
    }

    public ArenaCardDTO() {
    }

    public ArenaCardDTO(ArenaCard card) {
        this.id = card.getId();
        this.name = card.getName();
        this.gameId = card.getGame() != null ? card.getGame().getId() : null;
        this.setId = card.getSet() != null ? card.getSet().getId() : null;
        this.setName = card.getSetName();
        this.number = card.getNumber();
        this.tcgplayerId = card.getTcgplayerId();
        this.scryfallId = card.getScryfallId();
        this.mtgjsonId = card.getMtgjsonId();
        this.rarity = card.getRarity();
        this.details = card.getDetails();
        this.imageUrl = card.getImageUrl();
        this.lastSync = card.getLastSync();

        if (card.getVariants() != null) {
            this.variants = card.getVariants().stream()
                    .map(VariantDTO::new)
                    .collect(Collectors.toList());
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getSetName() {
        return setName;
    }

    public void setSetName(String setName) {
        this.setName = setName;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTcgplayerId() {
        return tcgplayerId;
    }

    public void setTcgplayerId(String tcgplayerId) {
        this.tcgplayerId = tcgplayerId;
    }

    public String getScryfallId() {
        return scryfallId;
    }

    public void setScryfallId(String scryfallId) {
        this.scryfallId = scryfallId;
    }

    public String getMtgjsonId() {
        return mtgjsonId;
    }

    public void setMtgjsonId(String mtgjsonId) {
        this.mtgjsonId = mtgjsonId;
    }

    public String getRarity() {
        return rarity;
    }

    public void setRarity(String rarity) {
        this.rarity = rarity;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<VariantDTO> getVariants() {
        return variants;
    }

    public void setVariants(List<VariantDTO> variants) {
        this.variants = variants;
    }

    public LocalDateTime getLastSync() {
        return lastSync;
    }

    public void setLastSync(LocalDateTime lastSync) {
        this.lastSync = lastSync;
    }
}
