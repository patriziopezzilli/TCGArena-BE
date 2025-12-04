package com.tcg.arena.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import com.tcg.arena.dto.InventoryCardDTO.*;
import com.tcg.arena.model.InventoryCard;
import com.tcg.arena.repository.InventoryCardRepository;
import com.tcg.arena.service.CardTemplateService;

@Service
public class InventoryCardService {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryCardService.class);
    
    private final InventoryCardRepository inventoryCardRepository;
    private final CardTemplateService cardTemplateService;
    
    public InventoryCardService(InventoryCardRepository inventoryCardRepository, CardTemplateService cardTemplateService) {
        this.inventoryCardRepository = inventoryCardRepository;
        this.cardTemplateService = cardTemplateService;
    }
    
    /**
     * Get inventory for a shop with filters
     */
    @Transactional(readOnly = true)
    public InventoryListResponse getInventory(
        Long shopId,
        InventoryFilters filters,
        int page,
        int size
    ) {
        log.info("Getting inventory for shop: {}", shopId);
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<InventoryCard> inventoryPage;
        
        if (filters == null || isEmptyFilter(filters)) {
            inventoryPage = inventoryCardRepository.findByShopId(shopId, pageable);
        } else {
            inventoryPage = inventoryCardRepository.searchInventory(
                shopId,
                filters.getTcgType(),
                filters.getCondition(),
                filters.getMinPrice(),
                filters.getMaxPrice(),
                filters.getSearchQuery(),
                pageable
            );
        }
        
        return new InventoryListResponse(
            inventoryPage.getContent(),
            (int) inventoryPage.getTotalElements(),
            page,
            size
        );
    }
    
    /**
     * Get single inventory card by ID
     */
    @Transactional(readOnly = true)
    public InventoryCard getInventoryCard(String id) {
        log.info("Getting inventory card: {}", id);
        return inventoryCardRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Inventory card not found: " + id));
    }
    
    /**
     * Create new inventory card
     */
    @Transactional
    public InventoryCard createInventoryCard(CreateInventoryCardRequest request) {
        log.info("Creating inventory card for shop: {}", request.getShopId());
        
        // Validate that the card template exists
        if (!cardTemplateService.getCardTemplateById(request.getCardTemplateId()).isPresent()) {
            throw new RuntimeException("Card template not found: " + request.getCardTemplateId());
        }
        
        InventoryCard inventoryCard = new InventoryCard();
        inventoryCard.setCardTemplateId(request.getCardTemplateId());
        inventoryCard.setShopId(request.getShopId());
        inventoryCard.setCondition(request.getCondition());
        inventoryCard.setPrice(request.getPrice());
        inventoryCard.setQuantity(request.getQuantity());
        inventoryCard.setNotes(request.getNotes());
        inventoryCard.setNationality(request.getNationality());
        
        // Set timestamps manually since @CreationTimestamp is not used
        LocalDateTime now = LocalDateTime.now();
        inventoryCard.setCreatedAt(now);
        inventoryCard.setUpdatedAt(now);
        
        return inventoryCardRepository.save(inventoryCard);
    }
    
    /**
     * Update inventory card
     */
    @Transactional
    public InventoryCard updateInventoryCard(String id, UpdateInventoryCardRequest request) {
        log.info("Updating inventory card: {}", id);
        
        InventoryCard inventoryCard = getInventoryCard(id);
        
        if (request.getCondition() != null) {
            inventoryCard.setCondition(request.getCondition());
        }
        if (request.getPrice() != null) {
            inventoryCard.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null) {
            inventoryCard.setQuantity(request.getQuantity());
        }
        if (request.getNotes() != null) {
            inventoryCard.setNotes(request.getNotes());
        }
        if (request.getNationality() != null) {
            inventoryCard.setNationality(request.getNationality());
        }
        
        return inventoryCardRepository.save(inventoryCard);
    }
    
    /**
     * Delete inventory card
     */
    @Transactional
    public void deleteInventoryCard(String id) {
        log.info("Deleting inventory card: {}", id);
        
        if (!inventoryCardRepository.existsById(id)) {
            throw new RuntimeException("Inventory card not found: " + id);
        }
        
        inventoryCardRepository.deleteById(id);
    }
    
    /**
     * Update quantity (for reservations/sales)
     */
    @Transactional
    public InventoryCard updateQuantity(String id, int quantityChange) {
        log.info("Updating quantity for inventory card: {} by {}", id, quantityChange);
        
        InventoryCard inventoryCard = getInventoryCard(id);
        int newQuantity = inventoryCard.getQuantity() + quantityChange;
        
        if (newQuantity < 0) {
            throw new RuntimeException("Insufficient quantity");
        }
        
        inventoryCard.setQuantity(newQuantity);
        return inventoryCardRepository.save(inventoryCard);
    }
    
    /**
     * Get inventory statistics
     */
    @Transactional(readOnly = true)
    public InventoryStatsResponse getInventoryStats(Long shopId) {
        log.info("Getting inventory stats for shop: {}", shopId);
        
        List<InventoryCard> allInventory = inventoryCardRepository.findByShopId(
            shopId, 
            Pageable.unpaged()
        ).getContent();
        
        long totalItems = allInventory.size();
        long totalQuantity = allInventory.stream()
            .mapToLong(InventoryCard::getQuantity)
            .sum();
        double totalValue = allInventory.stream()
            .mapToDouble(card -> card.getPrice() * card.getQuantity())
            .sum();
        
        List<InventoryCard> lowStock = inventoryCardRepository.findLowStockItems(shopId, 5);
        
        return new InventoryStatsResponse(
            totalItems,
            totalQuantity,
            totalValue,
            lowStock.size()
        );
    }
    
    /**
     * Get low stock items
     */
    @Transactional(readOnly = true)
    public List<InventoryCard> getLowStockItems(Long shopId, int threshold) {
        log.info("Getting low stock items for shop: {} with threshold: {}", shopId, threshold);
        return inventoryCardRepository.findLowStockItems(shopId, threshold);
    }
    
    private boolean isEmptyFilter(InventoryFilters filters) {
        return filters.getTcgType() == null &&
               filters.getCondition() == null &&
               filters.getMinPrice() == null &&
               filters.getMaxPrice() == null &&
               (filters.getSearchQuery() == null || filters.getSearchQuery().isEmpty());
    }
}
