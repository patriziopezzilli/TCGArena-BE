package com.tcg.arena.dto;

import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.CardTemplateService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpansionDTO {
    private Long id;
    private String title;
    private TCGType tcgType;
    private String imageUrl;
    private List<TCGSetDTO> sets;

    // Constructors, Getters, Setters
    public ExpansionDTO() {
    }

    public ExpansionDTO(Expansion expansion) {
        this.id = expansion.getId();
        this.title = expansion.getTitle();
        this.tcgType = expansion.getTcgType();
        this.imageUrl = expansion.getImageUrl();
        this.sets = expansion.getSets().stream()
                .map(TCGSetDTO::new)
                .collect(Collectors.toList());
    }

    @Deprecated // Use the optimized constructor with pre-calculated counts instead
    public ExpansionDTO(Expansion expansion, CardTemplateService cardTemplateService) {
        this.id = expansion.getId();
        this.title = expansion.getTitle();
        this.tcgType = expansion.getTcgType();
        this.imageUrl = expansion.getImageUrl();
        this.sets = expansion.getSets().stream()
                .map(set -> {
                    // Calculate actual card count from database using COUNT query
                    long actualCount = cardTemplateService.countCardsBySetCode(set.getSetCode());

                    // If no cards by setCode, try by expansion
                    if (actualCount == 0 && set.getExpansion() != null) {
                        actualCount = cardTemplateService.countCardsByExpansionId(set.getExpansion().getId());
                    }

                    return new TCGSetDTO(set, (int) actualCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * OPTIMIZED constructor - uses pre-calculated count maps for O(1) lookups
     * instead of N database queries
     */
    public ExpansionDTO(Expansion expansion, Map<String, Long> setCodeCounts, Map<Long, Long> expansionIdCounts) {
        this.id = expansion.getId();
        this.title = expansion.getTitle();
        this.tcgType = expansion.getTcgType();
        this.imageUrl = expansion.getImageUrl();
        this.sets = expansion.getSets().stream()
                .map(set -> {
                    // O(1) lookup from pre-calculated map
                    long actualCount = setCodeCounts.getOrDefault(set.getSetCode(), 0L);

                    // If no cards by setCode, try by expansion
                    if (actualCount == 0 && set.getExpansion() != null) {
                        actualCount = expansionIdCounts.getOrDefault(set.getExpansion().getId(), 0L);
                    }

                    return new TCGSetDTO(set, (int) actualCount);
                })
                .collect(Collectors.toList());
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

    public TCGType getTcgType() {
        return tcgType;
    }

    public void setTcgType(TCGType tcgType) {
        this.tcgType = tcgType;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<TCGSetDTO> getSets() {
        return sets;
    }

    public void setSets(List<TCGSetDTO> sets) {
        this.sets = sets;
    }
}