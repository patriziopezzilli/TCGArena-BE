package com.tcg.arena.service;

import com.tcg.arena.model.Shop;
import com.tcg.arena.repository.ShopRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ShopService {

    @Autowired
    private ShopRepository shopRepository;

    /**
     * Get all active shops (for public API - app)
     */
    public List<Shop> getAllShops() {
        return shopRepository.findByActiveTrue();
    }

    /**
     * Get all shops including inactive (for admin/backoffice)
     */
    public List<Shop> getAllShopsIncludingInactive() {
        return shopRepository.findAll();
    }

    public Optional<Shop> getShopById(Long id) {
        return shopRepository.findById(id);
    }

    public Optional<Shop> getShopByOwnerId(Long ownerId) {
        return shopRepository.findByOwnerId(ownerId);
    }

    public List<Shop> searchUnverifiedShops(String name) {
        return shopRepository.findByNameContainingIgnoreCaseAndIsVerifiedFalseAndOwnerIdIsNull(name);
    }

    public Shop saveShop(Shop shop) {
        return shopRepository.save(shop);
    }

    public Optional<Shop> updateShop(Long id, Shop shopDetails) {
        return shopRepository.findById(id).map(shop -> {
            shop.setName(shopDetails.getName());
            shop.setDescription(shopDetails.getDescription());
            shop.setAddress(shopDetails.getAddress());
            shop.setLatitude(shopDetails.getLatitude());
            shop.setLongitude(shopDetails.getLongitude());
            shop.setPhoneNumber(shopDetails.getPhoneNumber());
            shop.setWebsiteUrl(shopDetails.getWebsiteUrl());
            shop.setType(shopDetails.getType());
            shop.setIsVerified(shopDetails.getIsVerified());
            shop.setReservationDurationMinutes(shopDetails.getReservationDurationMinutes());
            return shopRepository.save(shop);
        });
    }

    /**
     * Update reservation duration setting for a shop
     */
    public Optional<Shop> updateReservationDuration(Long shopId, Integer durationMinutes) {
        if (durationMinutes == null || durationMinutes < 1 || durationMinutes > 1440) { // Max 24 hours
            throw new IllegalArgumentException("Reservation duration must be between 1 and 1440 minutes");
        }

        return shopRepository.findById(shopId).map(shop -> {
            shop.setReservationDurationMinutes(durationMinutes);
            return shopRepository.save(shop);
        });
    }

    /**
     * Get reservation duration for a shop
     */
    public Integer getReservationDuration(Long shopId) {
        return shopRepository.findById(shopId)
                .map(shop -> shop.getReservationDurationMinutes() != null ? shop.getReservationDurationMinutes() : 30)
                .orElse(30); // Default to 30 minutes if shop not found
    }

    public boolean deleteShop(Long id) {
        if (shopRepository.existsById(id)) {
            shopRepository.deleteById(id);
            return true;
        }
        return false;
    }
}