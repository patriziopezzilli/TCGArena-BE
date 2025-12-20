package com.tcg.arena.service;

import com.tcg.arena.dto.NewsItemDTO;
import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopNews;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.BroadcastNewsRepository;
import com.tcg.arena.repository.ShopNewsRepository;
import com.tcg.arena.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsAggregationService {

    @Autowired
    private BroadcastNewsRepository broadcastNewsRepository;

    @Autowired
    private ShopNewsRepository shopNewsRepository;

    @Autowired
    private ShopRepository shopRepository;

    @Autowired
    private ShopSubscriptionService shopSubscriptionService;

    /**
     * Get aggregated news for a user (broadcast + subscribed shops)
     */
    public List<NewsItemDTO> getAggregatedNews(User user, int limit) {
        List<NewsItemDTO> allNews = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        System.out.println("🕐 DEBUG - Current time: " + now);

        // 1. Get active broadcast news
        List<BroadcastNews> broadcastNews = broadcastNewsRepository.findActiveNews(now);
        System.out.println("📰 DEBUG - Broadcast news found: " + broadcastNews.size());
        
        // Debug: Get ALL broadcast news to see what exists
        List<BroadcastNews> allBroadcastNews = broadcastNewsRepository.findAll();
        System.out.println("📰 DEBUG - Total broadcast news in DB: " + allBroadcastNews.size());
        for (BroadcastNews news : allBroadcastNews) {
            System.out.println("  - ID: " + news.getId() + 
                             ", Title: " + news.getTitle() + 
                             ", StartDate: " + news.getStartDate() + 
                             ", ExpiryDate: " + news.getExpiryDate() +
                             ", Active: " + (news.getStartDate() != null && news.getStartDate().isBefore(now) || news.getStartDate().isEqual(now)) +
                                          (news.getExpiryDate() == null || news.getExpiryDate().isAfter(now)));
        }
        
        allNews.addAll(broadcastNews.stream()
                .map(NewsItemDTO::new)
                .collect(Collectors.toList()));

        // 2. Get subscribed shops
        System.out.println("👤 DEBUG - User ID: " + user.getId() + ", Username: " + user.getUsername());
        List<Long> subscribedShopIds = shopSubscriptionService.getUserSubscriptions(user.getId())
                .stream()
                .map(subscription -> subscription.getShopId())
                .collect(Collectors.toList());
        System.out.println("🏪 DEBUG - User subscribed to shops: " + subscribedShopIds.size());
        if (!subscribedShopIds.isEmpty()) {
            System.out.println("  Shop IDs: " + subscribedShopIds);
        }

        // 3. Get active news from subscribed shops
        for (Long shopId : subscribedShopIds) {
            List<ShopNews> shopNews = shopNewsRepository.findActiveNewsByShopId(shopId, now);
            System.out.println("📰 DEBUG - Shop " + shopId + " has " + shopNews.size() + " active news");
            Shop shop = shopRepository.findById(shopId).orElse(null);
            String shopName = shop != null ? shop.getName() : "Unknown Shop";

            allNews.addAll(shopNews.stream()
                    .map(news -> new NewsItemDTO(news, shopName))
                    .collect(Collectors.toList()));
        }

        // 4. Sort by pinned first, then by start date descending
        allNews.sort(Comparator
                .comparing(NewsItemDTO::getIsPinned, Comparator.reverseOrder())
                .thenComparing(NewsItemDTO::getStartDate, Comparator.reverseOrder()));

        System.out.println("✅ DEBUG - Total news aggregated: " + allNews.size());

        // 5. Limit results
        return allNews.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Get aggregated news without user context (only broadcast)
     */
    public List<NewsItemDTO> getPublicNews(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<BroadcastNews> broadcastNews = broadcastNewsRepository.findActiveNews(now);

        return broadcastNews.stream()
                .map(NewsItemDTO::new)
                .limit(limit)
                .collect(Collectors.toList());
    }
}
