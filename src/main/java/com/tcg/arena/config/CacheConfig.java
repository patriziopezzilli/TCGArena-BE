package com.tcg.arena.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Cache configuration for TCG Arena Backend.
 * Uses Caffeine cache with specific TTL for different data types.
 * 
 * Cache Strategy:
 * - LONG_TTL (12h): Static data that rarely changes (expansions, sets, card templates, achievements, partners)
 * - MEDIUM_TTL (30min): Semi-static data (shops, rewards, pro decks)
 * - SHORT_TTL (5min): Dynamic data that changes more frequently (tournaments, search results)
 * - VERY_SHORT_TTL (1min): Real-time sensitive data (leaderboard, community content)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // ==================== CACHE NAMES ====================
    
    // Card & Catalog Caches (TTL: 6-12 hours)
    public static final String CARD_TEMPLATES_CACHE = "cardTemplates";
    public static final String CARD_TEMPLATE_BY_ID_CACHE = "cardTemplateById";
    public static final String CARD_SEARCH_CACHE = "cardSearch";
    public static final String CARD_FILTERS_CACHE = "cardFilters";
    public static final String MARKET_PRICE_CACHE = "marketPrice";
    
    // Expansion & Set Caches (TTL: 12-24 hours)
    public static final String EXPANSIONS_CACHE = "expansions";
    public static final String EXPANSION_BY_ID_CACHE = "expansionById";
    public static final String RECENT_EXPANSIONS_CACHE = "recentExpansions";
    public static final String SETS_CACHE = "sets";
    public static final String SET_BY_ID_CACHE = "setById";
    public static final String SET_CARDS_CACHE = "setCards";
    public static final String EXPANSION_CARDS_CACHE = "expansionCards";
    
    // Pro Decks Cache (TTL: 6 hours)
    public static final String PRO_DECKS_CACHE = "proDecks";
    public static final String PRO_DECK_BY_ID_CACHE = "proDeckById";
    public static final String RECENT_PRO_DECKS_CACHE = "recentProDecks";
    
    // Achievement & Partner Caches (TTL: 12 hours)
    public static final String ACHIEVEMENTS_CACHE = "achievements";
    public static final String ACHIEVEMENT_BY_ID_CACHE = "achievementById";
    public static final String PARTNERS_CACHE = "partners";
    public static final String PARTNER_BY_ID_CACHE = "partnerById";
    
    // Shop Caches (TTL: 15-30 min)
    public static final String SHOPS_CACHE = "shops";
    public static final String SHOP_BY_ID_CACHE = "shopById";
    public static final String SHOP_NEWS_CACHE = "shopNews";
    public static final String SHOP_REWARDS_CACHE = "shopRewards";
    
    // Tournament Caches (TTL: 2-5 min)
    public static final String TOURNAMENTS_CACHE = "tournaments";
    public static final String TOURNAMENT_BY_ID_CACHE = "tournamentById";
    public static final String UPCOMING_TOURNAMENTS_CACHE = "upcomingTournaments";
    public static final String PAST_TOURNAMENTS_CACHE = "pastTournaments";
    
    // Leaderboard & Stats Caches (TTL: 5-10 min)
    public static final String LEADERBOARD_CACHE = "leaderboard";
    public static final String USER_STATS_CACHE = "userStats";
    public static final String COMMUNITY_STATS_CACHE = "communityStats";
    
    // Rewards Cache (TTL: 10 min)
    public static final String REWARDS_CACHE = "rewards";
    public static final String REWARD_BY_ID_CACHE = "rewardById";
    
    // Arena Public API Cache (TTL: 6-24 hours)
    public static final String ARENA_GAMES_CACHE = "arenaGames";
    public static final String ARENA_SETS_CACHE = "arenaSets";
    public static final String ARENA_CARDS_CACHE = "arenaCards";
    
    // Community Caches (TTL: 1-2 min)
    public static final String COMMUNITY_EVENTS_CACHE = "communityEvents";
    public static final String COMMUNITY_THREADS_CACHE = "communityThreads";
    public static final String COMMUNITY_PULLS_CACHE = "communityPulls";
    
    // Global Chat Cache (TTL: 15 sec)
    public static final String GLOBAL_CHAT_CACHE = "globalChat";
    
    // Public Content Cache (TTL: 5-30 min)
    public static final String PUBLIC_CONTENT_CACHE = "publicContent";
    
    // Trade Listings Cache (TTL: 2 min)
    public static final String TRADE_LISTINGS_CACHE = "tradeListings";
    
    // Batch/Admin Cache (TTL: 24 hours)
    public static final String BATCH_CONFIG_CACHE = "batchConfig";

    @Bean
    @Primary
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        
        cacheManager.setCaches(Arrays.asList(
            // ==================== LONG TTL CACHES (6-24 hours) ====================
            
            // Card Templates - 6 hours, large size
            buildCache(CARD_TEMPLATES_CACHE, Duration.ofHours(6), 50000),
            buildCache(CARD_TEMPLATE_BY_ID_CACHE, Duration.ofHours(12), 100000),
            buildCache(CARD_SEARCH_CACHE, Duration.ofMinutes(30), 5000),
            buildCache(CARD_FILTERS_CACHE, Duration.ofHours(24), 100),
            buildCache(MARKET_PRICE_CACHE, Duration.ofMinutes(5), 10000),
            
            // Expansions & Sets - 12-24 hours
            buildCache(EXPANSIONS_CACHE, Duration.ofHours(12), 1000),
            buildCache(EXPANSION_BY_ID_CACHE, Duration.ofHours(24), 2000),
            buildCache(RECENT_EXPANSIONS_CACHE, Duration.ofHours(6), 100),
            buildCache(SETS_CACHE, Duration.ofHours(12), 2000),
            buildCache(SET_BY_ID_CACHE, Duration.ofHours(24), 5000),
            buildCache(SET_CARDS_CACHE, Duration.ofHours(12), 10000),
            buildCache(EXPANSION_CARDS_CACHE, Duration.ofHours(12), 10000),
            
            // Pro Decks - 6 hours
            buildCache(PRO_DECKS_CACHE, Duration.ofHours(6), 500),
            buildCache(PRO_DECK_BY_ID_CACHE, Duration.ofHours(6), 500),
            buildCache(RECENT_PRO_DECKS_CACHE, Duration.ofHours(1), 100),
            
            // Achievements & Partners - 12 hours
            buildCache(ACHIEVEMENTS_CACHE, Duration.ofHours(12), 200),
            buildCache(ACHIEVEMENT_BY_ID_CACHE, Duration.ofHours(12), 200),
            buildCache(PARTNERS_CACHE, Duration.ofHours(6), 100),
            buildCache(PARTNER_BY_ID_CACHE, Duration.ofHours(6), 100),
            
            // Arena Public API - 6-24 hours
            buildCache(ARENA_GAMES_CACHE, Duration.ofHours(24), 50),
            buildCache(ARENA_SETS_CACHE, Duration.ofHours(12), 1000),
            buildCache(ARENA_CARDS_CACHE, Duration.ofHours(6), 50000),
            
            // Batch Config - 24 hours
            buildCache(BATCH_CONFIG_CACHE, Duration.ofHours(24), 50),
            
            // ==================== MEDIUM TTL CACHES (15-30 min) ====================
            
            // Shops - 15-30 min
            buildCache(SHOPS_CACHE, Duration.ofMinutes(15), 2000),
            buildCache(SHOP_BY_ID_CACHE, Duration.ofMinutes(30), 2000),
            buildCache(SHOP_NEWS_CACHE, Duration.ofMinutes(5), 500),
            buildCache(SHOP_REWARDS_CACHE, Duration.ofMinutes(10), 500),
            
            // Public Content - 5-30 min
            buildCache(PUBLIC_CONTENT_CACHE, Duration.ofMinutes(30), 1000),
            
            // Rewards - 10 min
            buildCache(REWARDS_CACHE, Duration.ofMinutes(10), 500),
            buildCache(REWARD_BY_ID_CACHE, Duration.ofMinutes(10), 500),
            
            // ==================== SHORT TTL CACHES (2-5 min) ====================
            
            // Tournaments - 2-5 min
            buildCache(TOURNAMENTS_CACHE, Duration.ofMinutes(2), 500),
            buildCache(TOURNAMENT_BY_ID_CACHE, Duration.ofMinutes(1), 500),
            buildCache(UPCOMING_TOURNAMENTS_CACHE, Duration.ofMinutes(2), 200),
            buildCache(PAST_TOURNAMENTS_CACHE, Duration.ofMinutes(5), 500),
            
            // Leaderboard & Stats - 5-10 min
            buildCache(LEADERBOARD_CACHE, Duration.ofMinutes(10), 100),
            buildCache(USER_STATS_CACHE, Duration.ofMinutes(5), 1000),
            buildCache(COMMUNITY_STATS_CACHE, Duration.ofMinutes(5), 100),
            
            // Trade Listings - 2 min
            buildCache(TRADE_LISTINGS_CACHE, Duration.ofMinutes(2), 1000),
            
            // ==================== VERY SHORT TTL CACHES (15 sec - 2 min) ====================
            
            // Community Content - 1-2 min
            buildCache(COMMUNITY_EVENTS_CACHE, Duration.ofMinutes(2), 500),
            buildCache(COMMUNITY_THREADS_CACHE, Duration.ofMinutes(2), 500),
            buildCache(COMMUNITY_PULLS_CACHE, Duration.ofMinutes(1), 500),
            
            // Global Chat - 15 sec
            buildCache(GLOBAL_CHAT_CACHE, Duration.ofSeconds(15), 200)
        ));
        
        return cacheManager;
    }
    
    /**
     * Build a Caffeine cache with specified parameters.
     */
    private CaffeineCache buildCache(String name, Duration ttl, int maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}