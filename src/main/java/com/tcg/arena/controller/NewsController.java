package com.tcg.arena.controller;

import com.tcg.arena.dto.NewsItemDTO;
import com.tcg.arena.model.NewsCategory;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.User;
import com.tcg.arena.service.NewsAggregationService;
import com.tcg.arena.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/news")
@Tag(name = "News", description = "API for categorized and TCG-specific news")
public class NewsController {

    @Autowired
    private NewsAggregationService newsAggregationService;

    @Autowired
    private UserService userService;

    @GetMapping
    @Operation(summary = "Get filtered news", description = "Retrieves news filtered by category and TCG type")
    public ResponseEntity<List<NewsItemDTO>> getNews(
            @Parameter(description = "Category of news (ALL, GENERAL, TCG_SPECIFIC, SHOP)") @RequestParam(defaultValue = "ALL") NewsCategory category,
            @Parameter(description = "TCG type for filtering") @RequestParam(required = false) TCGType tcgType,
            @Parameter(description = "Limit of results") @RequestParam(defaultValue = "20") int limit,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getUserByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<NewsItemDTO> news = newsAggregationService.getFilteredNews(user, category, tcgType, limit);
        return ResponseEntity.ok(news);
    }

    @GetMapping("/home")
    @Operation(summary = "Get aggregated home news", description = "Retrieves the standard aggregated news feed for the home screen")
    public ResponseEntity<List<NewsItemDTO>> getHomeNews(
            @Parameter(description = "Limit of results") @RequestParam(defaultValue = "10") int limit,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.getUserByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<NewsItemDTO> news = newsAggregationService.getAggregatedNews(user, limit);
        return ResponseEntity.ok(news);
    }
}
