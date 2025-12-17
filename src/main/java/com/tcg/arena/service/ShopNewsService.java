package com.tcg.arena.service;

import com.tcg.arena.model.ShopNews;
import com.tcg.arena.model.NewsType;
import com.tcg.arena.repository.ShopNewsRepository;
import com.tcg.arena.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ShopNewsService {

    @Autowired
    private ShopNewsRepository shopNewsRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Create a new news item for a shop
     */
    @Transactional
    public ShopNews createNews(Long shopId, String title, String content, NewsType newsType,
            LocalDateTime startDate, LocalDateTime expiryDate,
            String imageUrl, Boolean isPinned) {
        // Verify shop exists
        var shop = shopRepository.findById(shopId)
                .orElseThrow(() -> new IllegalArgumentException("Shop not found with id: " + shopId));

        ShopNews news = new ShopNews();
        news.setShopId(shopId);
        news.setTitle(title);
        news.setContent(content);
        news.setNewsType(newsType);
        news.setStartDate(startDate != null ? startDate : LocalDateTime.now());
        news.setExpiryDate(expiryDate);
        news.setImageUrl(imageUrl);
        news.setIsPinned(isPinned != null ? isPinned : false);

        ShopNews saved = shopNewsRepository.save(news);

        // Send notification to subscribers if news is immediately active
        if (saved.getStartDate().isBefore(LocalDateTime.now()) || saved.getStartDate().isEqual(LocalDateTime.now())) {
            try {
                notificationService.sendShopNewsNotification(shopId, shop.getName(), title);
            } catch (Exception e) {
                // Log error but don't fail the news creation
                System.err.println("Failed to send news notification: " + e.getMessage());
            }
        }

        return saved;
    }

    /**
     * Update an existing news item
     */
    @Transactional
    public ShopNews updateNews(Long newsId, String title, String content, NewsType newsType,
            LocalDateTime startDate, LocalDateTime expiryDate,
            String imageUrl, Boolean isPinned) {
        ShopNews news = shopNewsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("News not found with id: " + newsId));

        if (title != null)
            news.setTitle(title);
        if (content != null)
            news.setContent(content);
        if (newsType != null)
            news.setNewsType(newsType);
        if (startDate != null)
            news.setStartDate(startDate);
        news.setExpiryDate(expiryDate); // Can be set to null
        if (imageUrl != null)
            news.setImageUrl(imageUrl);
        if (isPinned != null)
            news.setIsPinned(isPinned);

        return shopNewsRepository.save(news);
    }

    /**
     * Delete a news item
     */
    @Transactional
    public void deleteNews(Long newsId) {
        if (!shopNewsRepository.existsById(newsId)) {
            throw new IllegalArgumentException("News not found with id: " + newsId);
        }
        shopNewsRepository.deleteById(newsId);
    }

    /**
     * Get a single news item by ID
     */
    public Optional<ShopNews> getNewsById(Long newsId) {
        return shopNewsRepository.findById(newsId);
    }

    /**
     * Get all news for a shop (for merchant management)
     */
    public List<ShopNews> getAllNewsByShopId(Long shopId) {
        return shopNewsRepository.findByShopIdOrderByIsPinnedDescCreatedAtDesc(shopId);
    }

    /**
     * Get active news (currently visible)
     */
    public List<ShopNews> getActiveNews(Long shopId) {
        return shopNewsRepository.findActiveNewsByShopId(shopId, LocalDateTime.now());
    }

    /**
     * Get future news (scheduled but not yet visible)
     */
    public List<ShopNews> getFutureNews(Long shopId) {
        return shopNewsRepository.findFutureNewsByShopId(shopId, LocalDateTime.now());
    }

    /**
     * Get expired news
     */
    public List<ShopNews> getExpiredNews(Long shopId) {
        return shopNewsRepository.findExpiredNewsByShopId(shopId, LocalDateTime.now());
    }

    /**
     * Get public active news (for iOS app and public API)
     */
    public List<ShopNews> getPublicActiveNews(Long shopId) {
        return getActiveNews(shopId);
    }

    /**
     * Verify that a news item belongs to a specific shop
     */
    public boolean newsBegunToShop(Long newsId, Long shopId) {
        return shopNewsRepository.findById(newsId)
                .map(news -> news.getShopId().equals(shopId))
                .orElse(false);
    }

    /**
     * Verify shop ownership by user
     */
    public boolean isShopOwner(Long shopId, Long userId) {
        return shopRepository.findById(shopId)
                .map(shop -> shop.getOwnerId().equals(userId))
                .orElse(false);
    }
}
