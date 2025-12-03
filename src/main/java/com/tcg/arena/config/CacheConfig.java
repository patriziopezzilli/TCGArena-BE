package com.tcg.arena.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Arrays;

@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names
    public static final String EXPANSIONS_CACHE = "expansions";
    public static final String RECENT_EXPANSIONS_CACHE = "recentExpansions";
    public static final String SETS_CACHE = "sets";
    public static final String SET_CARDS_CACHE = "setCards";
    public static final String CARD_TEMPLATES_CACHE = "cardTemplates";
    public static final String EXPANSION_CARDS_CACHE = "expansionCards";
    public static final String USER_STATS_CACHE = "userStats";
    public static final String LEADERBOARD_CACHE = "leaderboard";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Set explicit cache names
        cacheManager.setCacheNames(Arrays.asList(
            EXPANSIONS_CACHE,
            RECENT_EXPANSIONS_CACHE,
            SETS_CACHE,
            SET_CARDS_CACHE,
            CARD_TEMPLATES_CACHE,
            EXPANSION_CARDS_CACHE,
            USER_STATS_CACHE,
            LEADERBOARD_CACHE
        ));
        
        // Configure 3-hour cache for API responses
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(200)
                .maximumSize(5000)
                .expireAfterWrite(Duration.ofHours(3))
                .recordStats());
        
        return cacheManager;
    }
}