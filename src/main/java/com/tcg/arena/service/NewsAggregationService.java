package com.tcg.arena.service;

import com.tcg.arena.dto.NewsItemDTO;
import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.Shop;
import com.tcg.arena.model.ShopNews;
import com.tcg.arena.model.User;
import com.tcg.arena.repository.BroadcastNewsRepository;
import com.tcg.arena.repository.ShopNewsRepository;
import com.tcg.arena.repository.ShopRepository;
import com.tcg.arena.model.NewsType;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.model.NewsCategory;
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

                // 1. Get active broadcast news
                List<BroadcastNews> broadcastNews = broadcastNewsRepository.findActiveNews(now);

                allNews.addAll(broadcastNews.stream()
                                .map(NewsItemDTO::new)
                                .collect(Collectors.toList()));

                // 2. Get subscribed shops
                List<Long> subscribedShopIds = shopSubscriptionService.getUserSubscriptions(user.getId())
                                .stream()
                                .map(subscription -> subscription.getShopId())
                                .collect(Collectors.toList());

                // 3. Get active news from subscribed shops
                for (Long shopId : subscribedShopIds) {
                        List<ShopNews> shopNews = shopNewsRepository.findActiveNewsByShopId(shopId, now);
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

        /**
         * Get filtered news based on category and TCG type
         */
        public List<NewsItemDTO> getFilteredNews(User user, NewsCategory category, TCGType tcgType, int limit) {
                List<NewsItemDTO> allNews = new ArrayList<>();
                LocalDateTime now = LocalDateTime.now();

                // 1. Handle Broadcast News
                if (category == NewsCategory.ALL || category == NewsCategory.GENERAL
                                || category == NewsCategory.TCG_SPECIFIC) {
                        List<BroadcastNews> broadcastNews = broadcastNewsRepository.findActiveNews(now);

                        allNews.addAll(broadcastNews.stream()
                                        .filter(n -> {
                                                if (category == NewsCategory.GENERAL)
                                                        return n.getTcgType() == null;
                                                if (category == NewsCategory.TCG_SPECIFIC)
                                                        return n.getTcgType() != null && (tcgType == null
                                                                        || n.getTcgType() == tcgType);
                                                if (tcgType != null)
                                                        return n.getTcgType() == tcgType;
                                                return true;
                                        })
                                        .map(NewsItemDTO::new)
                                        .collect(Collectors.toList()));
                }

                // 2. Handle Shop News
                if (category == NewsCategory.ALL || category == NewsCategory.SHOP
                                || (category == NewsCategory.TCG_SPECIFIC && tcgType != null)) {
                        List<Long> subscribedShopIds = shopSubscriptionService.getUserSubscriptions(user.getId())
                                        .stream()
                                        .map(subscription -> subscription.getShopId())
                                        .collect(Collectors.toList());

                        for (Long shopId : subscribedShopIds) {
                                List<ShopNews> shopNews = shopNewsRepository.findActiveNewsByShopId(shopId, now);
                                Shop shop = shopRepository.findById(shopId).orElse(null);
                                String shopName = shop != null ? shop.getName() : "Unknown Shop";

                                allNews.addAll(shopNews.stream()
                                                .filter(n -> {
                                                        if (category == NewsCategory.SHOP)
                                                                return true; // All shop news if specifically requested
                                                        if (tcgType != null)
                                                                return n.getTcgType() == tcgType;
                                                        return true;
                                                })
                                                .map(news -> new NewsItemDTO(news, shopName))
                                                .collect(Collectors.toList()));
                        }
                }

                // 3. Sort and Limit
                return allNews.stream()
                                .sorted(Comparator
                                                .comparing(NewsItemDTO::getIsPinned, Comparator.reverseOrder())
                                                .thenComparing(NewsItemDTO::getStartDate, Comparator.reverseOrder()))
                                .limit(limit)
                                .collect(Collectors.toList());
        }
}
