package com.tcg.arena.dto;

import com.tcg.arena.model.Expansion;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.service.CardTemplateService;
import java.util.List;
import java.util.stream.Collectors;

public class ExpansionDTO {
    private Long id;
    private String title;
    private TCGType tcgType;
    private String imageUrl;
    private List<TCGSetDTO> sets;

    // Constructors, Getters, Setters
    public ExpansionDTO() {}

    public ExpansionDTO(Expansion expansion) {
        this.id = expansion.getId();
        this.title = expansion.getTitle();
        this.tcgType = expansion.getTcgType();
        this.imageUrl = expansion.getImageUrl();
        this.sets = expansion.getSets().stream()
            .map(TCGSetDTO::new)
            .collect(Collectors.toList());
    }

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

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public TCGType getTcgType() { return tcgType; }
    public void setTcgType(TCGType tcgType) { this.tcgType = tcgType; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<TCGSetDTO> getSets() { return sets; }
    public void setSets(List<TCGSetDTO> sets) { this.sets = sets; }
}