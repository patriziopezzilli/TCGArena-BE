package com.tcg.arena.controller;

import com.tcg.arena.model.ShopNews;
import com.tcg.arena.model.User;
import com.tcg.arena.service.ShopNewsService;
import com.tcg.arena.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ShopNewsController {

    @Autowired
    private ShopNewsService shopNewsService;

    @Autowired
    private UserService userService;

    // ==================== PUBLIC ENDPOINTS ====================

    /**
     * Get active news for a shop (public endpoint for iOS app)
     */
    @GetMapping("/shops/{shopId}/news")
    public ResponseEntity<List<ShopNewsDTO>> getPublicActiveNews(@PathVariable Long shopId) {
        List<ShopNews> news = shopNewsService.getPublicActiveNews(shopId);
        List<ShopNewsDTO> dtos = news.stream()
                .map(ShopNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ==================== MERCHANT ENDPOINTS ====================

    /**
     * Get all news for merchant's shop
     */
    @GetMapping("/merchant/shops/{shopId}/news")
    public ResponseEntity<?> getAllShopNews(
            @PathVariable Long shopId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato a gestire le notizie di questo negozio"));
        }

        List<ShopNews> news = shopNewsService.getAllNewsByShopId(shopId);
        List<ShopNewsDTO> dtos = news.stream()
                .map(ShopNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get active news for merchant's shop
     */
    @GetMapping("/merchant/shops/{shopId}/news/active")
    public ResponseEntity<?> getActiveNews(
            @PathVariable Long shopId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato"));
        }

        List<ShopNews> news = shopNewsService.getActiveNews(shopId);
        List<ShopNewsDTO> dtos = news.stream()
                .map(ShopNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get future news for merchant's shop
     */
    @GetMapping("/merchant/shops/{shopId}/news/future")
    public ResponseEntity<?> getFutureNews(
            @PathVariable Long shopId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato"));
        }

        List<ShopNews> news = shopNewsService.getFutureNews(shopId);
        List<ShopNewsDTO> dtos = news.stream()
                .map(ShopNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get expired news for merchant's shop
     */
    @GetMapping("/merchant/shops/{shopId}/news/expired")
    public ResponseEntity<?> getExpiredNews(
            @PathVariable Long shopId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato"));
        }

        List<ShopNews> news = shopNewsService.getExpiredNews(shopId);
        List<ShopNewsDTO> dtos = news.stream()
                .map(ShopNewsDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    /**
     * Create a new news item
     */
    @PostMapping("/merchant/shops/{shopId}/news")
    public ResponseEntity<?> createNews(
            @PathVariable Long shopId,
            @RequestBody ShopNewsDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato a creare notizie per questo negozio"));
        }

        try {
            ShopNews news = shopNewsService.createNews(
                    shopId,
                    request.getTitle(),
                    request.getContent(),
                    request.getNewsType(),
                    request.getStartDate(),
                    request.getExpiryDate(),
                    request.getImageUrl(),
                    request.getIsPinned(),
                    request.getTcgType(),
                    request.getExternalUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(new ShopNewsDTO(news));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Update a news item
     */
    @PutMapping("/merchant/shops/{shopId}/news/{newsId}")
    public ResponseEntity<?> updateNews(
            @PathVariable Long shopId,
            @PathVariable Long newsId,
            @RequestBody ShopNewsDTO request,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato"));
        }

        if (!shopNewsService.newsBegunToShop(newsId, shopId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Notizia non trovata"));
        }

        try {
            ShopNews news = shopNewsService.updateNews(
                    newsId,
                    request.getTitle(),
                    request.getContent(),
                    request.getNewsType(),
                    request.getStartDate(),
                    request.getExpiryDate(),
                    request.getImageUrl(),
                    request.getIsPinned(),
                    request.getTcgType(),
                    request.getExternalUrl());
            return ResponseEntity.ok(new ShopNewsDTO(news));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Delete a news item
     */
    @DeleteMapping("/merchant/shops/{shopId}/news/{newsId}")
    public ResponseEntity<?> deleteNews(
            @PathVariable Long shopId,
            @PathVariable Long newsId,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        if (!shopNewsService.isShopOwner(shopId, user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Non sei autorizzato"));
        }

        if (!shopNewsService.newsBegunToShop(newsId, shopId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Notizia non trovata"));
        }

        try {
            shopNewsService.deleteNews(newsId);
            return ResponseEntity.ok(Map.of("message", "Notizia eliminata con successo"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // ==================== HELPER METHODS ====================

    private User getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        return userService.getUserByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
