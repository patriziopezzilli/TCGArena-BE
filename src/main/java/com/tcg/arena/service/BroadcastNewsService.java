package com.tcg.arena.service;

import com.tcg.arena.model.BroadcastNews;
import com.tcg.arena.model.NewsType;
import com.tcg.arena.model.TCGType;
import com.tcg.arena.repository.BroadcastNewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class BroadcastNewsService {

    @Autowired
    private BroadcastNewsRepository broadcastNewsRepository;

    /**
     * Get all broadcast news
     */
    public List<BroadcastNews> getAllNews() {
        return broadcastNewsRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * Get active broadcast news
     */
    public List<BroadcastNews> getActiveNews() {
        return broadcastNewsRepository.findActiveNews(LocalDateTime.now());
    }

    /**
     * Get future broadcast news
     */
    public List<BroadcastNews> getFutureNews() {
        return broadcastNewsRepository.findFutureNews(LocalDateTime.now());
    }

    /**
     * Get expired broadcast news
     */
    public List<BroadcastNews> getExpiredNews() {
        return broadcastNewsRepository.findExpiredNews(LocalDateTime.now());
    }

    /**
     * Get broadcast news by ID
     */
    public Optional<BroadcastNews> getNewsById(Long id) {
        return broadcastNewsRepository.findById(id);
    }

    /**
     * Create new broadcast news
     */
    public BroadcastNews createNews(String title, String content, NewsType newsType,
            LocalDateTime startDate, LocalDateTime expiryDate,
            String imageUrl, Boolean isPinned, Long createdBy,
            TCGType tcgType, String externalUrl) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("Content is required");
        }
        if (newsType == null) {
            throw new IllegalArgumentException("News type is required");
        }

        BroadcastNews news = new BroadcastNews();
        news.setTitle(title);
        news.setContent(content);
        news.setNewsType(newsType);
        news.setStartDate(startDate != null ? startDate : LocalDateTime.now());
        news.setExpiryDate(expiryDate);
        news.setImageUrl(imageUrl);
        news.setIsPinned(isPinned != null ? isPinned : false);
        news.setCreatedBy(createdBy);
        news.setTcgType(tcgType);
        news.setExternalUrl(externalUrl);

        return broadcastNewsRepository.save(news);
    }

    /**
     * Update broadcast news
     */
    public BroadcastNews updateNews(Long id, String title, String content, NewsType newsType,
            LocalDateTime startDate, LocalDateTime expiryDate,
            String imageUrl, Boolean isPinned,
            TCGType tcgType, String externalUrl) {
        BroadcastNews news = broadcastNewsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("News not found"));

        if (title != null && !title.trim().isEmpty()) {
            news.setTitle(title);
        }
        if (content != null && !content.trim().isEmpty()) {
            news.setContent(content);
        }
        if (newsType != null) {
            news.setNewsType(newsType);
        }
        if (startDate != null) {
            news.setStartDate(startDate);
        }
        news.setExpiryDate(expiryDate);
        news.setImageUrl(imageUrl);
        if (isPinned != null) {
            news.setIsPinned(isPinned);
        }
        news.setTcgType(tcgType);
        news.setExternalUrl(externalUrl);

        return broadcastNewsRepository.save(news);
    }

    /**
     * Delete broadcast news
     */
    public void deleteNews(Long id) {
        broadcastNewsRepository.deleteById(id);
    }
}
